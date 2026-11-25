package endpractice;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EnderEyeEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

/**
 * Eye throw trainer: a new world with a stronghold 128 to 384 blocks away, the same spots as endgame practice. Throw
 * eyes (they're refilled), work out where the stronghold is, and guess: the guess key where you stand, or /guess x z.
 * You're told how far off you were.
 */
public final class EyeTrainer {
    private static final int EYE = 381;

    private static boolean active;
    private static int attempts;
    private static long startTime, endTime;
    private static int eyes;
    private static BlockPos target;
    private static Vec3i startPos;
    private static String result;
    /** The kit's eyes: thrown ones are given back up to this many. */
    private static int eyesKept;

    private EyeTrainer() {
    }

    private static net.minecraft.world.gen.structure.StructureFeature strongholdFeature(net.minecraft.world.World world) {
        try {
            net.minecraft.world.chunk.ChunkSource gen = ((endpractice.mixin.ChunkCacheAccessor) world.getChunkSource()).endpractice$getGenerator();
            if (gen instanceof net.minecraft.world.gen.chunk.OverworldChunkGenerator) return ((net.minecraft.world.gen.chunk.OverworldChunkGenerator) gen).stronghold;
        } catch (RuntimeException e) {
            EndPractice.log("no stronghold feature: " + e);
        }
        return null;
    }

    /** Clears the strongholds laid out so far, as if none had generated yet. */
    public static void forgetStrongholds(net.minecraft.world.World world) {
        net.minecraft.world.gen.structure.StructureFeature f = strongholdFeature(world);
        if (f != null) ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$getStructures().clear();
    }

    /**
     * Where an eye thrown from x y z goes, the same way 1.0 works it out but without laying anything out: the
     * strongholds generated so far if there are any (fresh: pretend there are none), else the three spots at y 64.
     * Each scores dx + dx*dy*dy + dz*dz and the lowest wins.
     */
    @SuppressWarnings("unchecked")
    static BlockPos eyeTarget(net.minecraft.world.World world, int x, int y, int z, boolean fresh) {
        net.minecraft.world.gen.structure.StructureFeature f = strongholdFeature(world);
        if (f == null) return null;
        BlockPos best = null;
        double d = Double.MAX_VALUE;
        if (!fresh) {
            for (net.minecraft.world.gen.structure.StructureStart st : ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$getStructures().values()) {
                if (!st.isValid()) continue;
                BlockPos c = ((net.minecraft.world.gen.structure.StructurePiece) st.getPieces().get(0)).getCenterPos();
                double e = score(c, x, y, z);
                if (e < d) { d = e; best = c; }
            }
            if (best != null) return best;
        }
        java.util.List<BlockPos> list = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$potentialPositions();
        if (list == null) return null;
        for (BlockPos c : list) {
            double e = score(c, x, y, z);
            if (e < d) { d = e; best = c; }
        }
        return best;
    }

    private static double score(BlockPos c, int x, int y, int z) {
        int i = c.x - x, k = c.y - y, m = c.z - z;
        return i + i * k * k + m * m;
    }

    public static boolean isActive() {
        return active;
    }

    /**
     * Where an eye thrown at x z goes in practice: the closest portal room of the three strongholds, so there's no eye
     * bug. Null (1.0's own targeting) with Vanilla eye targeting on, outside the practice modes, or while ranked runs.
     */
    public static BlockPos eyeGoesTo(net.minecraft.world.World world, int x, int z) {
        if (EndPractice.cfg == null || EndPractice.cfg.vanillaEyes || endpractice.compat.RankedCompat.busy() || !EndPractice.inPracticeWorld()) return null;
        BlockPos[] r = closestRoom(world, x, z);
        return r == null ? null : r[1];
    }

    /** The stronghold (its spot) whose portal room is closest to x z, and that room's centre. */
    static BlockPos[] closestRoom(net.minecraft.world.World world, double x, double z) {
        net.minecraft.world.gen.structure.StructureFeature f = strongholdFeature(world);
        if (f == null) return null;
        @SuppressWarnings("unchecked")
        java.util.List<BlockPos> list = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$potentialPositions();
        if (list == null || list.isEmpty()) {
            // the three spots are worked out on the first search
            try {
                f.findNearestPosition(world, (int) x, 64, (int) z);
            } catch (Exception e) {
                EndPractice.log("couldn't find the strongholds: " + e);
            }
            list = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$potentialPositions();
            if (list == null || list.isEmpty()) return null;
        }
        BlockPos[] best = null;
        double d = Double.MAX_VALUE;
        for (BlockPos s : new ArrayList<BlockPos>(list)) {
            BlockPos room = portalRoom(world, s);
            if (room == null) continue;
            double e = Math.hypot(room.x - x, room.z - z);
            if (e < d) { d = e; best = new BlockPos[]{s, room}; }
        }
        return best;
    }

    static void stop() {
        active = false;
    }

    static void start(Minecraft mc) {
        attempts++;
        active = false;
        result = null;
        Endgame.makeTrainerWorld(mc);
        PlayerEntity p = mc.player;
        EndPractice.giveKit(p, EndPractice.cfg.eyeKit);
        eyesKept = 0;
        for (ItemStack s : p.inventory.items) if (s != null && s.id == EYE) eyesKept += s.size;
        p.inventory.selectedSlot = 0;
        p.setHealth(20);
        p.getHungerManager().add(20, 20.0F);
        mc.world.setTime(mc.world.getTime() - Math.floorMod(mc.world.getTime(), 24000L) + 24000 + 500); // morning

        startPos = new Vec3i((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
        // the stronghold the spot was picked around: from here an eye thrown from the wrong height may go elsewhere
        target = Endgame.spotStronghold != null ? Endgame.spotStronghold : eyeTarget(mc.world, startPos.x, 80, startPos.z, false);
        if (!EndPractice.cfg.vanillaEyes) {
            // eyes go to the closest portal room, so that's the one to find
            BlockPos[] r = closestRoom(mc.world, startPos.x, startPos.z);
            if (r != null) target = r[0];
        }
        keimaPending = false;
        keimaEye = null;
        keimaBase = 0;
        calibrating = 0;
        eyes = 0;
        throwsList.clear();
        startTime = mc.world.getTime();
        endTime = -1;
        Commands.cheated = false;
        active = true;
        EndPractice.chat(mc, "§dEye throw trainer§r attempt " + attempts + ". Throw eyes (they're refilled), then "
                + (EndPractice.cfg.keyGuess != org.lwjgl.input.Keyboard.KEY_NONE ? Config.keyName(EndPractice.cfg.keyGuess) + " guesses where you stand, or " : "")
                + "/guess x z. " + Config.keyName(EndPractice.cfg.keyEyeTrainer) + " for a new one.");
        if (EndPractice.cfg.eyeCoaching) EndPractice.chat(mc, "§7Coaching: after two throws far enough apart you'll see where the lines cross, whether both went to the same stronghold, and where the other two can be.§r");
    }

    static void tick(Minecraft mc) {
        if (!active || mc.player == null) return;
        if (keimaPending && mc.screen == null && (keimaEye == null || keimaEye.removed || mc.world.getTime() - keimaThrown > 70)) {
            keimaPending = false;
            keimaEye = null;
            mc.openScreen(new KeimaScreen());
        }
        if (endTime < 0) {
            // keep 16 eyes
            PlayerEntity p = mc.player;
            int n = 0;
            for (ItemStack s : p.inventory.items) if (s != null && s.id == EYE) n += s.size;
            if (n < eyesKept) p.inventory.addItem(new ItemStack(EYE, eyesKept - n, 0));
        }
    }

    /** From the ender eye mixin: an eye was thrown from where it is now, to x z. */
    public static void onEyeThrown(EnderEyeEntity eye, double tx, double tz) {
        Minecraft mc = EndPractice.mc;
        if (!active || endTime >= 0 || mc == null || mc.world == null || mc.player == null || eye.world != mc.world) return;
        eyes++;
        PlayerEntity p = mc.player;
        int px = (int) p.x, py = (int) p.y, pz = (int) p.z; // what the game searched from (F3's y)
        double dx = tx - eye.x, dz = tz - eye.z;
        String dir = direction(dx, dz);
        StringBuilder sb = new StringBuilder("Eye " + eyes + " from y " + py + ": went " + dir);
        if (EndPractice.cfg.eyeAngles) {
            // Minecraft's own yaw: 0 is south (+z), 90 west (-x)
            sb.append(String.format(Locale.ROOT, ", angle %.1f", Math.toDegrees(Math.atan2(-dx, dz))));
        }
        EndPractice.chat(mc, sb.toString());
        BlockPos sim = eyeTarget(mc.world, px, py, pz, false);
        EndPractice.log("eye to " + (int) tx + " " + (int) tz + ", worked out " + (sim == null ? "none" : sim.x + " " + sim.z) + ", target " + (target == null ? "none" : target.x + " " + target.z));
        throwsList.add(new double[]{eye.x, eye.z, dx, dz, tx, tz});
        if (keimaPairDone()) {
            // the Keima method: you'll be asked how many pixels the crosshair moved once this eye is done
            keimaPending = true;
            keimaEye = eye;
            keimaThrown = mc.world.getTime();
            keimaAnswered = false;
        }
        if (!EndPractice.cfg.eyeCoaching || target == null) return;

        // the eye bug: this eye went to a stronghold that isn't the closest one to where you threw it
        BlockPos closest = closestStronghold(mc.world, eye.x, eye.z);
        BlockPos went = strongholdOf(mc.world, tx, tz);
        if (EndPractice.cfg.vanillaEyes && closest != null && went != null && !sameSpot(closest, went)) {
            EndPractice.chat(mc, "§c Eye bug:§r this eye went to a stronghold " + (int) Math.hypot(tx - eye.x, tz - eye.z)
                    + " blocks away, but one is " + (int) Math.hypot(closest.x - eye.x, closest.z - eye.z) + " blocks away.");
        }
        triangulate(mc);
    }

    /** Every throw this attempt: x, z, the way it went (dx, dz) and where it was going. */
    private static final List<double[]> throwsList = new ArrayList<>();

    /**
     * After two throws far enough apart: where their lines cross, whether they went to the same stronghold, and where
     * the other two strongholds can be from there.
     */
    private static void triangulate(Minecraft mc) {
        int n = throwsList.size();
        if (n < 2) return;
        double[] b = throwsList.get(n - 1);
        // the earlier throw at the widest angle to this one
        double[] a = null;
        double bestSin = 0;
        for (int i = 0; i < n - 1; i++) {
            double[] t = throwsList.get(i);
            double sin = Math.abs(cross(t[2], t[3], b[2], b[3])) / (Math.hypot(t[2], t[3]) * Math.hypot(b[2], b[3]));
            if (sin > bestSin) { bestSin = sin; a = t; }
        }
        int ai = throwsList.indexOf(a) + 1;
        if (a == null || bestSin < Math.sin(Math.toRadians(2))) {
            EndPractice.chat(mc, "§7 These lines are almost parallel: walk further sideways to the line before the next throw.§r");
            return;
        }
        // where line a (a + s*da) meets line b (b + t*db)
        double den = cross(a[2], a[3], b[2], b[3]);
        double s = cross(b[0] - a[0], b[1] - a[1], b[2], b[3]) / den;
        double cx = a[0] + s * a[2], cz = a[1] + s * a[3];
        double t = cross(b[0] - a[0], b[1] - a[1], a[2], a[3]) / den;
        if (s < 0 || t < 0) {
            EndPractice.chat(mc, "§7 Eyes " + ai + " and " + n + " point away from each other, so they're going to different strongholds.§r");
            return;
        }
        PlayerEntity p = mc.player;
        int dist = (int) Math.hypot(cx - p.x, cz - p.z);
        BlockPos sa = strongholdOf(mc.world, a[4], a[5]), sb = strongholdOf(mc.world, b[4], b[5]);
        boolean same = sa != null && sb != null ? sameSpot(sa, sb) : Math.hypot(a[4] - b[4], a[5] - b[5]) <= 64;
        EndPractice.chat(mc, "§b Eyes " + ai + " and " + n + " cross at " + (int) cx + " " + (int) cz + "§r, " + dist + " blocks "
                + direction(cx - p.x, cz - p.z) + " of you (" + String.format(Locale.ROOT, "%.0f", Math.toDegrees(Math.asin(Math.min(1, bestSin))))
                + " degrees apart)." + (same ? " §aBoth went to the same stronghold.§r" : (EndPractice.cfg.vanillaEyes ? " §cThey went to different strongholds (eye bug), so this crossing is nowhere.§r" : " §cThey went to different strongholds (you crossed the halfway line between two), so this crossing is nowhere.§r")));
        if (same) otherTwo(mc, cx, cz);
    }

    private static double cross(double ax, double az, double bx, double bz) {
        return ax * bz - az * bx;
    }

    /**
     * 1.0 places three strongholds around 0,0, 120 degrees apart (one random start angle), each its own distance of
     * 1.25 to 2.25 times 32 chunks (640 to 1152 blocks), then moves each up to 112 blocks to a biome it likes. So from
     * one stronghold the other two are each somewhere on an arc 120 degrees round from it.
     */
    private static void otherTwo(Minecraft mc, double cx, double cz) {
        double r = Math.hypot(cx, cz);
        PlayerEntity p = mc.player;
        double here = Math.hypot(cx - p.x, cz - p.z);
        if (r < 640 - 112 - 32 || r > 1152 + 112 + 32) {
            EndPractice.chat(mc, "§7 That's " + (int) r + " blocks from 0,0, outside 1.0's stronghold ring (640 to 1152), so one of the lines is off.§r");
            return;
        }
        double angle = Math.atan2(cz, cx);
        StringBuilder sb = new StringBuilder("§7 The other two are 120 degrees round 0,0 from it, 640 to 1152 out: ");
        boolean closer = false;
        for (int k = -1; k <= 1; k += 2) {
            double a = angle + k * Math.PI * 2 / 3;
            // the nearest point of that arc to you, allowing for the biome move
            double best = Double.MAX_VALUE, bx = 0, bz = 0;
            for (int d = 640; d <= 1152; d += 16) {
                double x = Math.cos(a) * d, z = Math.sin(a) * d;
                double e = Math.hypot(x - p.x, z - p.z);
                if (e < best) { best = e; bx = x; bz = z; }
            }
            best = Math.max(0, best - 112);
            if (best < here) closer = true;
            sb.append(k < 0 ? "" : ", ").append("one at least ").append((int) best).append(" blocks away to the ").append(direction(bx - p.x, bz - p.z));
        }
        EndPractice.chat(mc, sb.append(".§r").toString());
        EndPractice.chat(mc, closer ? "§e One of the others could be closer than this one, so check that way too.§r"
                : "§a Neither of the others can be closer: this is your nearest stronghold.§r");
    }

    /**
     * The centre of the portal room of the stronghold whose spot is sh, the point 1.0's eye flies to once that stronghold
     * has generated (its first piece's centre is the portal room's). If it hasn't generated yet it's laid out the way the
     * game would (only after the guess, and the next attempt forgets it again).
     */
    public static BlockPos portalRoom(net.minecraft.world.World world, BlockPos sh) {
        net.minecraft.world.gen.structure.StructureFeature f = strongholdFeature(world);
        if (f == null || sh == null) return null;
        long key = net.minecraft.util.math.ChunkPos.toLong(sh.x >> 4, sh.z >> 4);
        java.util.HashMap<Long, net.minecraft.world.gen.structure.StructureStart> map = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$getStructures();
        if (!map.containsKey(key)) {
            try {
                f.findNearestPosition(world, sh.x, 64, sh.z);
            } catch (Exception e) {
                EndPractice.log("couldn't lay out the stronghold: " + e);
            }
        }
        net.minecraft.world.gen.structure.StructureStart st = map.get(key);
        if (st == null || st.getPieces().isEmpty()) return null;
        return ((net.minecraft.world.gen.structure.StructurePiece) st.getPieces().get(0)).getCenterPos();
    }

    private static boolean sameSpot(BlockPos a, BlockPos b) {
        return a.x >> 4 == b.x >> 4 && a.z >> 4 == b.z >> 4;
    }

    /**
     * Which of the three strongholds (its spot) an eye flying to x z is going to. An eye goes either to a spot itself
     * (no stronghold generated yet) or to a generated stronghold's portal room, which can be 100 blocks or more from
     * its spot, so it's matched by the stronghold it belongs to, not by distance.
     */
    static BlockPos strongholdOf(net.minecraft.world.World world, double x, double z) {
        net.minecraft.world.gen.structure.StructureFeature f = strongholdFeature(world);
        if (f == null) return null;
        @SuppressWarnings("unchecked")
        java.util.List<BlockPos> list = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$potentialPositions();
        if (list == null || list.isEmpty()) return null;
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        for (java.util.Map.Entry<Long, net.minecraft.world.gen.structure.StructureStart> e : ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$getStructures().entrySet()) {
            net.minecraft.world.gen.structure.StructureStart st = e.getValue();
            if (st.getPieces().isEmpty()) continue;
            BlockPos c = ((net.minecraft.world.gen.structure.StructurePiece) st.getPieces().get(0)).getCenterPos();
            if (Math.abs(c.x - ix) <= 1 && Math.abs(c.z - iz) <= 1) {
                // this start's chunk is one of the spots
                for (BlockPos s : list) if (net.minecraft.util.math.ChunkPos.toLong(s.x >> 4, s.z >> 4) == e.getKey()) return s;
            }
        }
        BlockPos best = null;
        double d = Double.MAX_VALUE;
        for (BlockPos s : list) {
            double h = Math.hypot(s.x - x, s.z - z);
            if (h < d) { d = h; best = s; }
        }
        return best;
    }

    /** The world's closest stronghold to x z, of the three (as straight distance, not the eye's sum). */
    private static BlockPos closestStronghold(net.minecraft.world.World world, double x, double z) {
        net.minecraft.world.gen.structure.StructureFeature f = strongholdFeature(world);
        if (f == null) return null;
        @SuppressWarnings("unchecked")
        java.util.List<BlockPos> list = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$potentialPositions();
        BlockPos best = null;
        double d = Double.MAX_VALUE;
        if (list != null) for (BlockPos c : list) {
            double e = Math.hypot(c.x - x, c.z - z);
            if (e < d) { d = e; best = c; }
        }
        return best;
    }

    /** Which way an eye went, the way you'd call it from the sun: west, west-north-west, north and so on. */
    private static String direction(double dx, double dz) {
        // 0 is east, counter-clockwise; north is -z
        double a = Math.toDegrees(Math.atan2(-dz, dx));
        if (a < 0) a += 360;
        String[] names = {"east", "east-north-east", "north-east", "north-north-east", "north", "north-north-west", "north-west", "west-north-west",
                "west", "west-south-west", "south-west", "south-south-west", "south", "south-south-east", "south-east", "east-south-east"};
        return names[(int) Math.round(a / 22.5) % 16];
    }

    /** A guess at where the stronghold is. */
    static void guess(Minecraft mc, int x, int z) {
        if (!active) return;
        if (endTime >= 0) {
            EndPractice.chat(mc, "Already guessed. " + Config.keyName(EndPractice.cfg.keyEyeTrainer) + " for a new one.");
            return;
        }
        if (target == null) {
            EndPractice.chat(mc, "§cThis world has no stronghold to find.");
            return;
        }
        endTime = mc.world.getTime();
        long ticks = endTime - startTime;
        // scored against the portal room, which is where 1.0's eye flies once the stronghold has generated
        BlockPos room = portalRoom(mc.world, target);
        BlockPos at = room != null ? room : target;
        int off = (int) Math.round(Math.hypot(at.x - x, at.z - z));
        int far = (int) Math.round(Math.hypot(at.x - startPos.x, at.z - startPos.z));
        String grade = off <= 8 ? "§aspot on§r" : off <= 24 ? "§agood§r" : off <= 64 ? "§eclose§r" : "§cway off§r";
        result = off + " blocks off";
        EndPractice.log("guess " + x + " " + z + ", stronghold spot " + target.x + " " + target.z + ", portal room " + (room == null ? "none" : room.x + " " + room.y + " " + room.z));
        EndPractice.chat(mc, (room != null ? "Portal room at §e" : "Stronghold at §e") + at.x + " " + at.z + "§r, you guessed " + x + " " + z + ": §e" + off
                + " blocks off§r (" + grade + ")" + (off > 0 ? ", " + direction(at.x - x, at.z - z) + " of your guess" : "")
                + ", " + eyes + " eye" + (eyes == 1 ? "" : "s") + " in " + EndPractice.time(ticks)
                + ". It was " + far + " blocks from where you started.");
        if (!throwsList.isEmpty()) {
            // which stronghold the eyes led to (the last one's), and the closest from where you started
            double[] last = throwsList.get(throwsList.size() - 1);
            BlockPos near = closestStronghold(mc.world, startPos.x, startPos.z);
            BlockPos led = strongholdOf(mc.world, last[4], last[5]);
            boolean ledToTarget = led != null ? sameSpot(led, target) : Math.min(Math.hypot(last[4] - target.x, last[5] - target.z), Math.hypot(last[4] - at.x, last[5] - at.z)) <= 64;
            boolean targetClosest = near != null && Math.hypot(near.x - target.x, near.z - target.z) <= 64;
            EndPractice.chat(mc, "Your last eye led to " + (ledToTarget ? "this stronghold" : "§canother stronghold§r at " + (int) last[4] + " " + (int) last[5])
                    + (targetClosest ? ", the closest one to where you started." : ". §eThe closest to where you started was at " + near.x + " " + near.z + ".§r"));
        }
        if (Commands.cheated) {
            EndPractice.chat(mc, "§7Not saved: commands were used.§r");
        } else {
            save(off, ticks, far);
            long[] st = stats();
            if (st[0] > 0) EndPractice.chat(mc, "§7Average " + st[1] + " blocks off and " + String.format(Locale.ROOT, "%.1f", st[2] / 10.0)
                    + " eyes over " + st[0] + ", best " + st[3] + "§r");
        }
        EndPractice.chat(mc, Config.keyName(EndPractice.cfg.keyEyeTrainer) + " for a new one.");
    }

    // ---- the Keima method -------------------------------------------------------------------------------------------
    //
    // Line the side of your crosshair up with a thrown eye, jump sideways 4 times, throw again, and count how many of
    // the eye's 16 pixels the crosshair has moved by: the stronghold is about C / pixels away. The eye's sprite is half a
    // block wide (16 pixels, so a pixel is 1/32 of a block), and it hovers about L blocks out, so a pixel is (1/32)/L of
    // a radian, while moving d blocks sideways turns the eye's line by d / distance. So distance = 32 * L * d / pixels.
    // In 1.0 the eye hovers 12 blocks out and 4 sideways jumps cover 8.3 blocks (both measured in game).

    /** How far out the eye hovers (horizontally) while you count. */
    static final double KEIMA_L = 12.0;
    /** Four sideways jumps, holding strafe and jump from standing, to the fourth landing. */
    static final double KEIMA_D = 8.3;
    public static final int KEIMA_C = (int) Math.round(32 * KEIMA_L * KEIMA_D / 10.0) * 10;
    /** Blocks per sprint jump in 1.0, steady on flat ground (measured: 30 jumps, 112.5 blocks, 12 ticks each). */
    static final double SPRINT_JUMP = 3.75;
    /** Tries before the calibrated constant is used. */
    static final int CALIBRATE_MIN = 5;

    private static boolean keimaPending, keimaAnswered;
    private static EnderEyeEntity keimaEye;
    private static long keimaThrown;
    /** The first throw of the pair being asked about. */
    private static int keimaBase;
    /** Guided calibration: tries left (0: off), and the constants from this run. */
    private static int calibrating;
    private static final List<Double> calibrationRun = new ArrayList<>();

    /** Whether this throw makes a Keima pair to ask about. */
    private static boolean keimaPairDone() {
        int n = throwsList.size() - keimaBase;
        return n == 2 && (calibrating > 0 || EndPractice.cfg.keima && keimaBase == 0);
    }

    /** The pair's throws: true pixel offset, blocks moved sideways, real distance from the second throw. Null if fewer. */
    public static double[] keimaTruth() {
        if (throwsList.size() < keimaBase + 2) return null;
        double[] a = throwsList.get(keimaBase), b = throwsList.get(keimaBase + 1);
        double la = Math.hypot(a[2], a[3]), lb = Math.hypot(b[2], b[3]);
        if (la == 0 || lb == 0) return null;
        double ax = a[2] / la, az = a[3] / la, bx = b[2] / lb, bz = b[3] / lb;
        double angle = Math.abs(Math.atan2(cross(ax, az, bx, bz), ax * bx + az * bz));
        double side = Math.abs(cross(b[0] - a[0], b[1] - a[1], ax, az));
        double real = Math.hypot(b[4] - b[0], b[5] - b[1]);
        return new double[]{32 * KEIMA_L * angle, side, real};
    }

    /** The constant in use: your calibrated one (Calibrated, once there are 5 tries), else the fixed one. */
    public static int keimaConstant() {
        double[] c = calibration();
        if (EndPractice.cfg.keimaCalibrated && c[0] >= CALIBRATE_MIN) return (int) Math.round(c[1]);
        return EndPractice.cfg.keimaFixed;
    }

    private static String sprint(double blocks) {
        return Math.round(blocks) + " blocks ~ " + Math.round(blocks / SPRINT_JUMP) + " sprint jumps";
    }

    /** /keima calibrate: a guided run of 5 tries. */
    static void startCalibration(Minecraft mc) {
        if (!active) {
            EndPractice.chat(mc, "§c/keima calibrate works in the eye throw trainer.");
            return;
        }
        calibrating = CALIBRATE_MIN;
        calibrationRun.clear();
        keimaBase = throwsList.size();
        keimaPending = false;
        EndPractice.chat(mc, "§bKeima calibration§r: " + CALIBRATE_MIN + " tries. Each one: throw an eye, line the side of your crosshair up with it, hold strafe and jump 4 times without turning, throw again, then type how many pixels the crosshair moved. Jump the way you would in a run.");
    }

    /** /keima reset: starts the calibration over (the old tries are kept in a .old file). */
    static void resetCalibration(Minecraft mc) {
        File f = keimaFile();
        if (!f.isFile()) {
            EndPractice.chat(mc, "No Keima tries saved yet.");
            return;
        }
        File old = new File(f.getParentFile(), "endpractice-keima-" + System.currentTimeMillis() + ".old");
        EndPractice.chat(mc, f.renameTo(old) ? "Keima calibration and stats cleared (the old tries are in config/" + old.getName() + ")." : "§cCouldn't clear " + f.getName() + ".");
    }

    /** Your answer: pixels (under 100) or a distance guess in blocks (100 or more). */
    public static void keimaAnswer(Minecraft mc, double value) {
        double[] t = keimaTruth();
        if (!active || t == null) {
            EndPractice.chat(mc, "§cThrow two eyes first (line up with the first, 4 jumps sideways, throw again).");
            return;
        }
        if (value <= 0) {
            EndPractice.chat(mc, "§cThat's not a pixel count.");
            return;
        }
        boolean pixels = value < 100;
        double real = t[2];
        double[] cal = calibration();
        int use = keimaConstant();
        boolean usingCal = EndPractice.cfg.keimaCalibrated && cal[0] >= CALIBRATE_MIN;
        double est = pixels ? use / value : value;
        double err = est - real, pct = err * 100 / real;
        String grade = Math.abs(pct) <= 5 ? "§aspot on§r" : Math.abs(pct) <= 15 ? "§agood§r" : Math.abs(pct) <= 30 ? "§eclose§r" : "§cway off§r";
        if (pixels) {
            StringBuilder sb = new StringBuilder("§bKeima§r " + fmt(value) + " px: ");
            double fe = EndPractice.cfg.keimaFixed / value;
            sb.append(usingCal ? "" : "§e").append("fixed ").append(EndPractice.cfg.keimaFixed).append(" -> ").append(Math.round(fe))
                    .append(String.format(Locale.ROOT, " (%+.1f%%)", (fe - real) * 100 / real)).append(usingCal ? "" : "§r");
            if (cal[0] > 0) {
                double ce = cal[1] / value;
                sb.append(", ").append(usingCal ? "§e" : "").append("calibrated ").append(Math.round(cal[1])).append(" -> ").append(Math.round(ce))
                        .append(String.format(Locale.ROOT, " (%+.1f%%)", (ce - real) * 100 / real)).append(usingCal ? "§r" : "");
            }
            sb.append(". Real §e").append(Math.round(real)).append("§r blocks to the portal room (").append(grade).append(").");
            EndPractice.chat(mc, sb.toString());
        } else {
            EndPractice.chat(mc, "§bKeima§r you guessed §e" + Math.round(est) + "§r, real §e" + Math.round(real) + "§r blocks to the portal room: "
                    + (err >= 0 ? "+" : "") + Math.round(err) + String.format(Locale.ROOT, " (%+.1f%%, ", pct) + grade + ").");
        }
        EndPractice.chat(mc, "§7 ~" + sprint(est) + " (real: " + sprint(real) + ").§r");
        EndPractice.chat(mc, "§7 True offset " + fmt(t[0]) + " px. You moved " + fmt(t[1]) + " blocks sideways, so your jumps this time make the constant "
                + Math.round(32 * KEIMA_L * t[1]) + (pixels ? ", and with your count, " + Math.round(value * real) : "") + ".§r");
        if (keimaAnswered) {
            EndPractice.chat(mc, "§7Not saved: already answered for these throws.§r");
            return;
        }
        keimaAnswered = true;
        if (Commands.cheated) {
            EndPractice.chat(mc, "§7Not saved to your stats: commands were used.§r");
        } else {
            keimaSave(pixels ? value : -1, est, real, pct, t[1], pixels ? value * real : -1);
            double[] st = keimaStats();
            if (st[0] > 0) EndPractice.chat(mc, String.format(Locale.ROOT, "§7Keima: %.1f%% off on average over the last %d, best %.1f%%, %d tries§r", st[1], (int) Math.min(10, st[0]), st[2], (int) st[0]));
            cal = calibration();
            if (cal[0] > 0 && calibrating == 0) EndPractice.chat(mc, constantLine(cal) + (cal[0] < CALIBRATE_MIN ? " §7(" + (int) cal[0] + " of " + CALIBRATE_MIN + " tries: it's used once there are " + CALIBRATE_MIN + ")§r" : ""));
        }
        if (calibrating > 0) {
            // a guided run counts even with commands used: it's only working out your constant
            if (pixels) calibrationRun.add(value * real);
            calibrating--;
            keimaBase = throwsList.size();
            if (calibrating > 0) {
                EndPractice.chat(mc, "§bCalibration§r " + (CALIBRATE_MIN - calibrating) + "/" + CALIBRATE_MIN + " done. Next: throw, line up, 4 jumps sideways, throw again.");
            } else if (!calibrationRun.isEmpty()) {
                double m = median(calibrationRun);
                EndPractice.chat(mc, "§bCalibration done.§r Your constant: §e~" + Math.round(m) + "§r. In a run, distance ~ " + Math.round(m) + " / pixels."
                        + " §7(like 4 jumps of " + fmt(m / (32 * KEIMA_L)) + " blocks with exact counting, median of " + calibrationRun.size() + ")§r");
                if (Commands.cheated) EndPractice.chat(mc, "§7Commands were used, so it isn't saved as your calibrated constant: /keima fixed " + Math.round(m) + " uses it anyway.§r");
            }
        }
    }

    /** "Your constant: ~XXXX ..." from the calibration numbers. */
    static String constantLine(double[] cal) {
        return "§bYour constant: ~" + Math.round(cal[1]) + "§r (±" + Math.round(cal[2]) + ", last " + (int) Math.min(10, cal[0]) + " tries, like 4 jumps of "
                + fmt(cal[1] / (32 * KEIMA_L)) + " blocks). In a run, distance ~ " + Math.round(cal[1]) + " / pixels.";
    }

    private static double median(List<Double> v) {
        List<Double> s = new ArrayList<>(v);
        java.util.Collections.sort(s);
        int n = s.size();
        return n % 2 == 1 ? s.get(n / 2) : (s.get(n / 2 - 1) + s.get(n / 2)) / 2;
    }

    /** Tries with a pixel count, the median of their constants (count x real distance) over the last 10, and the spread (sd). */
    static double[] calibration() {
        List<Double> cs = new ArrayList<>();
        for (String[] p : keimaRows()) {
            if (p.length < 7) continue;
            try {
                double c = Double.parseDouble(p[6].trim());
                if (c > 0) cs.add(c);
            } catch (NumberFormatException ignored) {
            }
        }
        if (cs.isEmpty()) return new double[]{0, 0, 0};
        List<Double> last = cs.subList(Math.max(0, cs.size() - 10), cs.size());
        double m = median(last), mean = 0, sd = 0;
        for (double c : last) mean += c;
        mean /= last.size();
        for (double c : last) sd += (c - mean) * (c - mean);
        sd = Math.sqrt(sd / last.size());
        return new double[]{cs.size(), m, sd};
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static File keimaFile() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-keima.txt");
    }

    // config/endpractice-keima.txt: time,pixels (-1 for a distance guess),estimate,real,% off,blocks sideways,constant (pixels x real, -1)
    private static void keimaSave(double px, double est, double real, double pct, double side, double constant) {
        try {
            File f = keimaFile();
            f.getParentFile().mkdirs();
            String line = String.format(Locale.ROOT, "%d,%.1f,%d,%d,%.1f,%.2f,%.0f%n", System.currentTimeMillis(), px, Math.round(est), Math.round(real), pct, side, constant);
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            EndPractice.log("couldn't save the Keima try: " + e);
        }
    }

    private static List<String[]> keimaRows() {
        List<String[]> rows = new ArrayList<>();
        try {
            File f = keimaFile();
            if (f.isFile()) for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) rows.add(line.split(","));
        } catch (Exception e) {
            EndPractice.log("couldn't read the Keima tries: " + e);
        }
        return rows;
    }

    /** tries, average % off (absolute) over the last 10, best % off. */
    static double[] keimaStats() {
        List<Double> offs = new ArrayList<>();
        for (String[] p : keimaRows()) {
            if (p.length < 5) continue;
            try {
                offs.add(Math.abs(Double.parseDouble(p[4].trim())));
            } catch (NumberFormatException ignored) {
            }
        }
        if (offs.isEmpty()) return new double[]{0, 0, 0};
        double best = Double.MAX_VALUE, sum = 0;
        for (double o : offs) best = Math.min(best, o);
        int from = Math.max(0, offs.size() - 10);
        for (int i = from; i < offs.size(); i++) sum += offs.get(i);
        return new double[]{offs.size(), sum / (offs.size() - from), best};
    }

    static List<String> hud(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (!active || mc.world == null) return lines;
        long ticks = (endTime >= 0 ? endTime : mc.world.getTime()) - startTime;
        lines.add("§dEye trainer§r  " + EndPractice.time(ticks) + "   Eyes " + eyes);
        if (EndPractice.cfg.keimaOverlay) {
            double[] t = keimaTruth();
            if (t != null) lines.add("§bKeima§r true offset " + fmt(t[0]) + " px, moved " + fmt(t[1]) + " sideways");
        }
        if (mc.player != null) lines.add(String.format(Locale.ROOT, "XYZ %.1f / %.1f / %.1f", mc.player.x, mc.player.y, mc.player.z));
        if (result != null) lines.add(result);
        else lines.add("§7" + (EndPractice.cfg.keyGuess != org.lwjgl.input.Keyboard.KEY_NONE ? Config.keyName(EndPractice.cfg.keyGuess) + " guesses here, or " : "") + "/guess x z§r");
        return lines;
    }

    // ---- config/endpractice-eyes.txt: time,off,eyes,ticks,distance ------------------------------------------------

    private static File file() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-eyes.txt");
    }

    private static void save(int off, long ticks, int far) {
        try {
            File f = file();
            f.getParentFile().mkdirs();
            String line = String.format(Locale.ROOT, "%d,%d,%d,%d,%d%n", System.currentTimeMillis(), off, eyes, ticks, far);
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            EndPractice.log("couldn't save the eye guess: " + e);
        }
    }

    /** guesses, average blocks off, average eyes x10, best blocks off. */
    private static long[] stats() {
        long n = 0, off = 0, eyesTotal = 0, best = -1;
        try {
            File f = file();
            if (f.isFile()) {
                for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                    String[] p = line.split(",");
                    if (p.length < 3) continue;
                    try {
                        long o = Long.parseLong(p[1].trim());
                        eyesTotal += Long.parseLong(p[2].trim());
                        off += o;
                        n++;
                        if (best < 0 || o < best) best = o;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            EndPractice.log("couldn't read the eye guesses: " + e);
        }
        return n == 0 ? new long[]{0, 0, 0, 0} : new long[]{n, Math.round((double) off / n), Math.round(eyesTotal * 10.0 / n), best};
    }

    static List<String> pbLines() {
        List<String> out = new ArrayList<>();
        double[] ks = keimaStats();
        out.add("§bKeima method§r" + (ks[0] == 0 ? "   §7none§r" : String.format(Locale.ROOT, "   best §e%.1f%% off§r   last %d avg §e%.1f%%§r, %d tries", ks[2], (int) Math.min(10, ks[0]), ks[1], (int) ks[0])));
        double[] cal = calibration();
        if (cal[0] > 0) out.add("   " + constantLine(cal));
        long[] st = stats();
        out.add("§dEye throw trainer§r" + (st[0] == 0 ? "   §7none§r" : "   best §e" + st[3] + " blocks off§r   avg §e" + st[1]
                + "§r over " + st[0] + ", " + String.format(Locale.ROOT, "%.1f", st[2] / 10.0) + " eyes each"));
        return out;
    }
}
