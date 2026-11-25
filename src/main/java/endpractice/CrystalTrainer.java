package endpractice;

import endpractice.mixin.ArrowEntityAccessor;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EnderCrystalEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.monster.boss.EnderDragonEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/**
 * Crystal aim trainer: a new End with the dragon taken out, a bow and arrows, standing in the middle of the island.
 * The timer starts with the first arrow and stops when the last crystal goes. Every crystal gets a split, and the
 * arrows shot give the accuracy.
 * <p>
 * Random mode: one crystal at a time on a random tower, shot from a random spot on the island. Every hit moves you
 * to a new spot with a new crystal, and hits without a missed arrow in between make a streak.
 * <p>
 * Both modes say after every missed arrow where it went past the crystal, so the next one can go higher or lower.
 */
public final class CrystalTrainer {
    /** Arrow hitbox around a crystal's middle: the crystal is 2x2x2 and arrows hit anything within 0.3 of it. */
    static final double HIT_RADIUS = 1.3;
    /** Ticks between a hit and the move to the next crystal, so the explosion can be seen. */
    private static final int NEXT_DELAY = 10;

    private static final Random random = new Random();

    private static boolean active;
    private static boolean randomMode;
    private static int attempts;
    private static long startTime = -1; // the first arrow
    private static long lastCrystalTime;
    private static long endTime = -1;
    /** Every crystal seen this attempt: the island's chunks can still be loading at the start. */
    private static final Set<EnderCrystalEntity> crystals = new HashSet<>();
    private static int destroyed;
    private static int arrows;
    private static final Map<ArrowEntity, Track> tracks = new IdentityHashMap<>();
    /** The last tick a crystal went, to tell a hit arrow from one that despawned. */
    private static long crystalGoneTime = -10;
    private static String lastLine;
    private static String missLine;
    private static long pb = -1;
    private static boolean pbLoaded;

    // random mode
    /** Where the island's crystals were: the tops of the towers. */
    private static final List<double[]> towers = new ArrayList<>();
    private static int tower = -1;
    private static EnderCrystalEntity target;
    private static long roundStart = -1;
    private static long nextRoundAt = -1;
    private static int round;
    private static int hits;
    private static long hitTicks;
    private static int streak;
    private static int bestStreak = -1;
    private static boolean bestLoaded;
    private static boolean streakSaved = true;

    private CrystalTrainer() {
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isRandomMode() {
        return active && randomMode;
    }

    /** The crystal to hit in random mode (null otherwise). */
    public static EnderCrystalEntity target() {
        return active && randomMode ? target : null;
    }

    /** The crystal trainer's best time in ticks, -1 for none. */
    static long bestTime() {
        loadPb();
        return pb;
    }

    /** The best random crystal streak ever (0 for none). */
    static int bestStreak() {
        loadBest();
        return Math.max(0, bestStreak);
    }

    public static int streak() {
        return streak;
    }

    static void stop() {
        if (active && randomMode) endStreak(null);
        active = false;
    }

    static void start(Minecraft mc, boolean randomMode) {
        loadPb();
        loadBest();
        if (active && CrystalTrainer.randomMode) endStreak(null);
        attempts++;
        EndPractice.log("starting crystal trainer attempt " + attempts + (randomMode ? " (random)" : ""));
        active = false;
        BedReport.clear();
        EndPractice.newEnd(mc);

        PlayerEntity p = mc.player;
        // generate the whole island now, towers and crystals included, so none turn up halfway through
        for (int x = -128; x <= 128; x += 16) {
            for (int z = -128; z <= 128; z += 16) mc.world.getBlock(x, 64, z);
        }
        removeDragon(mc);
        // the middle of the island, where the crystals are shot from (on end stone, not on top of a tower)
        int[] spot = middle(mc);
        p.setPositionAndAngles(spot[0] + 0.5, spot[1] + p.eyeHeight + 0.1, spot[2] + 0.5, p.yaw, 0.0F);
        for (int i = 0; i < p.inventory.items.length; i++) p.inventory.items[i] = null;
        for (int i = 0; i < p.inventory.armor.length; i++) p.inventory.armor[i] = null;
        p.inventory.selectedSlot = 0;
        giveBow(p);
        p.setHealth(20);
        p.getHungerManager().add(20, 20.0F);
        p.velocityX = p.velocityY = p.velocityZ = 0.0;
        p.fallDistance = 0.0F;

        CrystalTrainer.randomMode = randomMode;
        active = true;
        startTime = -1;
        endTime = -1;
        arrows = 0;
        tracks.clear();
        crystals.clear();
        destroyed = 0;
        crystalGoneTime = -10;
        seeCrystals(mc);
        lastLine = null;
        missLine = null;
        if (!randomMode) {
            EndPractice.chat(mc, "§dCrystal aim trainer§r attempt " + attempts + ": " + crystals.size()
                    + " crystals. The timer starts with your first arrow. " + Config.keyName(EndPractice.cfg.keyCrystals) + " restarts.");
            return;
        }

        // random mode: remember where the crystals were, take them all out, and put one back at a time
        towers.clear();
        for (EnderCrystalEntity c : crystals) {
            towers.add(new double[]{c.x, c.y, c.z});
            c.remove();
        }
        crystals.clear();
        tower = -1;
        target = null;
        round = 0;
        hits = 0;
        hitTicks = 0;
        streak = 0;
        streakSaved = true;
        roundStart = -1;
        nextRoundAt = -1;
        if (towers.isEmpty()) {
            EndPractice.chat(mc, "§cNo crystals on this island.§r " + Config.keyName(EndPractice.cfg.keyRandomCrystals) + " for another.");
            return;
        }
        EndPractice.chat(mc, "§dRandom crystals§r: hit the crystal and you move to a new spot with a new one. Hits without a miss"
                + " make a streak" + (bestStreak > 0 ? " (best " + bestStreak + ")" : "") + ". "
                + Config.keyName(EndPractice.cfg.keyRandomCrystals) + " restarts.");
        nextRound(mc);
    }

    private static void giveBow(PlayerEntity p) {
        p.inventory.items[0] = new ItemStack(261, 1, 0); // bow
        for (int i = 1; i <= 3; i++) p.inventory.items[i] = new ItemStack(262, 64, 0); // arrows
    }

    /** The end stone surface nearest 0,0. */
    static int[] middle(Minecraft mc) {
        for (int r = 0; r <= 16; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != r) continue;
                    int y = mc.world.getHeight(x, z);
                    if (y > 0 && mc.world.getBlock(x, y - 1, z) == Block.END_STONE.id) return new int[]{x, y, z};
                }
            }
        }
        return new int[]{0, mc.world.getHeight(0, 0), 0};
    }

    private static void seeCrystals(Minecraft mc) {
        for (Object o : mc.world.entities) {
            if (o instanceof EnderCrystalEntity && !((Entity) o).removed) crystals.add((EnderCrystalEntity) o);
        }
    }

    private static void removeDragon(Minecraft mc) {
        EnderDragonEntity d = EndPractice.dragon(mc);
        if (d != null) d.remove();
    }

    // ---- random mode -------------------------------------------------------------------------------------------

    /** A new crystal on a random tower (never the same one twice in a row), and the player on a random spot. */
    private static void nextRound(Minecraft mc) {
        PlayerEntity p = mc.player;
        int t = random.nextInt(towers.size());
        if (towers.size() > 1 && t == tower) t = (t + 1 + random.nextInt(towers.size() - 1)) % towers.size();
        tower = t;
        double[] c = towers.get(t);
        target = new EnderCrystalEntity(mc.world, c[0], c[1], c[2]);
        mc.world.addEntity(target);

        int[] spot = shootingSpot(mc, c);
        double dx = c[0] - (spot[0] + 0.5), dz = c[2] - (spot[2] + 0.5);
        // facing roughly the right way, but the crystal still has to be found
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)) + (random.nextFloat() * 2 - 1) * 45.0F;
        p.setPositionAndAngles(spot[0] + 0.5, spot[1] + 0.1, spot[2] + 0.5, yaw, 0.0F);
        p.lastYaw = p.yaw;
        p.lastPitch = p.pitch;
        p.velocityX = p.velocityY = p.velocityZ = 0.0;
        p.fallDistance = 0.0F;
        p.setHealth(20);
        giveBow(p);
        round++;
        roundStart = mc.world.getTime();
        nextRoundAt = -1;
        EndPractice.log(String.format(Locale.ROOT, "random crystal %d: tower %d at %.1f %.1f %.1f, player at %d %d %d (%.0f blocks), %.2f degrees of pitch hit",
                round, t, c[0], c[1], c[2], spot[0], spot[1], spot[2], Math.sqrt(dx * dx + dz * dz), hitWindow));
    }

    /** Where to aim for the current crystal from the current spot (for the self test): yaw and pitch in degrees. */
    public static float aimYaw, aimPitch;
    /** Degrees of pitch that hit from the current spot. */
    private static double hitWindow;
    /** A spot needs at least this many degrees of pitch that hit with a fully drawn bow. */
    private static final double MIN_WINDOW = 1.5;

    /**
     * End stone 10 to 56 blocks from the tower where a fully drawn bow can really hit the crystal: shots are simulated
     * with the arrow's own speed, drag and gravity, against the blocks in the way (a wide tower's top edge hides most
     * of the crystal from close up and below) and the crystal's hitbox. The spot with the most room wins if none has
     * enough.
     */
    private static int[] shootingSpot(Minecraft mc, double[] c) {
        int[] best = null;
        double bestWindow = -1;
        long deadline = System.currentTimeMillis() + 1000;
        for (int i = 0; i < 400; i++) {
            int x = random.nextInt(161) - 80, z = random.nextInt(161) - 80;
            double dx = c[0] - (x + 0.5), dz = c[2] - (z + 0.5);
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < 10 || d > 56) continue;
            int y = surface(mc, x, z);
            if (y < 0) continue;
            double[] w = hitWindow(mc, x + 0.5, y + 0.1, z + 0.5, c);
            if (w[0] > bestWindow) {
                bestWindow = w[0];
                best = new int[]{x, y, z};
                hitWindow = w[0];
                aimPitch = (float) w[1];
                aimYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            }
            // the first random spot that can be hit: close shots stay in, only impossible ones are skipped
            if (bestWindow >= MIN_WINDOW || System.currentTimeMillis() > deadline) break;
        }
        if (best == null) {
            hitWindow = 0;
            return middle(mc);
        }
        return best;
    }

    /** The y to stand at on top of the highest block at x, z if that's end stone, else -1. */
    private static int surface(Minecraft mc, int x, int z) {
        mc.world.getBlock(x, 64, z); // loads the chunk
        for (int y = 126; y > 1; y--) {
            int id = mc.world.getBlock(x, y, z);
            if (id == 0) continue;
            return id == Block.END_STONE.id ? y + 1 : -1;
        }
        return -1;
    }

    /**
     * Shots with a fully drawn bow from feet at x, y, z at the crystal's middle, every quarter degree of pitch:
     * the degrees of pitch that hit, and the pitch in the middle of them (Minecraft's, negative is up).
     */
    private static double[] hitWindow(Minecraft mc, double x, double y, double z, double[] c) {
        double dx = c[0] - x, dz = c[2] - z;
        double h = Math.sqrt(dx * dx + dz * dz);
        double ux = dx / h, uz = dz / h;
        // where the arrow starts: the player's y (the eye) plus 0.12, less 0.1
        PlayerEntity p = mc.player;
        double sy = y + (p.y - p.shape.minY) + 0.02;
        double direct = Math.toDegrees(Math.atan2(c[1] - sy, h));
        int hitsCount = 0;
        double sum = 0;
        for (double e = direct - 4; e <= direct + 16; e += 0.25) {
            double r = Math.toRadians(e);
            if (simulate(mc, x, sy, z, ux * Math.cos(r) * 3.0, Math.sin(r) * 3.0, uz * Math.cos(r) * 3.0, c, h)) {
                hitsCount++;
                sum += e;
            }
        }
        return new double[]{hitsCount * 0.25, hitsCount == 0 ? 0 : -sum / hitsCount};
    }

    /** One arrow, tick by tick as ArrowEntity.tick moves it: does it reach the crystal's hitbox before a block? */
    private static boolean simulate(Minecraft mc, double x, double y, double z, double vx, double vy, double vz, double[] c, double dist) {
        double r = HIT_RADIUS;
        for (int t = 0; t < 100; t++) {
            double nx = x + vx, ny = y + vy, nz = z + vz;
            double hitBox = clip(x, y, z, nx, ny, nz, c[0] - r, c[1] - r, c[2] - r, c[0] + r, c[1] + r, c[2] + r);
            if (hitBox >= 0) {
                // a block before the crystal on this step stops it
                Object block = mc.world.rayTrace(Vec3d.fromPool(x, y, z), Vec3d.fromPool(x + vx * hitBox, y + vy * hitBox, z + vz * hitBox), false, true);
                return block == null;
            }
            if (mc.world.rayTrace(Vec3d.fromPool(x, y, z), Vec3d.fromPool(nx, ny, nz), false, true) != null) return false;
            x = nx;
            y = ny;
            z = nz;
            vx *= 0.99F;
            vy *= 0.99F;
            vz *= 0.99F;
            vy -= 0.05F;
            double ox = x - c[0], oz = z - c[2];
            if (y < 0 || ox * ox + oz * oz > (dist + 8) * (dist + 8)) return false;
        }
        return false;
    }

    /** The fraction along a to b where the segment enters the box, or -1 if it misses it. */
    private static double clip(double ax, double ay, double az, double bx, double by, double bz,
                               double x0, double y0, double z0, double x1, double y1, double z1) {
        double t0 = 0, t1 = 1;
        double[] a = {ax, ay, az}, d = {bx - ax, by - ay, bz - az}, lo = {x0, y0, z0}, hi = {x1, y1, z1};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-12) {
                if (a[i] < lo[i] || a[i] > hi[i]) return -1;
                continue;
            }
            double ta = (lo[i] - a[i]) / d[i], tb = (hi[i] - a[i]) / d[i];
            if (ta > tb) {
                double tmp = ta;
                ta = tb;
                tb = tmp;
            }
            t0 = Math.max(t0, ta);
            t1 = Math.min(t1, tb);
            if (t0 > t1) return -1;
        }
        return t0;
    }

    private static void tickRandom(Minecraft mc, long now) {
        // only the target crystal: anything else (a chunk loading late) goes
        for (Object o : mc.world.entities) {
            if (o instanceof EnderCrystalEntity && o != target && !((Entity) o).removed) ((Entity) o).remove();
        }
        if (target == null) return;
        if (nextRoundAt >= 0) {
            if (now >= nextRoundAt) nextRound(mc);
            return;
        }
        if (!target.removed) return;
        crystalGoneTime = now;
        // arrows still flying don't count against the next crystal
        for (Track tr : tracks.values()) tr.stale = true;
        long t = now - roundStart;
        hits++;
        hitTicks += t;
        streak++;
        streakSaved = false;
        String line = "§aHit§r in " + EndPractice.time(t) + ". Streak §e" + streak + "§r";
        if (bestStreak >= 0 && streak > bestStreak) line += " §6new best!§r";
        else if (bestStreak > 0) line += " (best " + bestStreak + ")";
        if (bestStreak < streak) bestStreak = streak;
        lastLine = line;
        missLine = null;
        EndPractice.chat(mc, line);
        nextRoundAt = now + NEXT_DELAY;
    }

    /** A missed arrow ends the streak; the streak is saved if it was 1 or more. */
    private static void endStreak(Minecraft mc) {
        if (streak > 0 && !streakSaved) saveStreak(streak);
        streakSaved = true;
        if (mc != null && streak > 0) {
            EndPractice.chat(mc, "Streak ended at " + streak + (streak >= bestStreak ? " §6(best)§r" : " (best " + bestStreak + ")"));
        }
        streak = 0;
    }

    // ---- arrows: hits, misses and where the misses went ----------------------------------------------------------

    /** The path of one arrow, sampled every tick. */
    private static final class Track {
        final List<double[]> path = new ArrayList<>();
        boolean stale;
        boolean done;
    }

    private static void trackArrows(Minecraft mc, long now) {
        for (Object o : mc.world.entities) {
            if (!(o instanceof ArrowEntity)) continue;
            ArrowEntity a = (ArrowEntity) o;
            if (a.shooter != mc.player || tracks.containsKey(a)) continue;
            Track t = new Track();
            // during the moment before the next crystal, shots don't count
            t.stale = randomMode && (nextRoundAt >= 0 || target == null);
            t.path.add(new double[]{a.lastX, a.lastY, a.lastZ});
            tracks.put(a, t);
            if (t.stale) continue;
            arrows++;
            if (!randomMode && startTime < 0) startTime = lastCrystalTime = now;
        }
        Iterator<Map.Entry<ArrowEntity, Track>> it = tracks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ArrowEntity, Track> e = it.next();
            ArrowEntity a = e.getKey();
            Track t = e.getValue();
            if (a.removed) {
                // gone without sticking: it hit a crystal (one went this tick) or fell out of the world
                if (!t.done && !t.stale && now - crystalGoneTime > 1) {
                    t.path.add(new double[]{a.x, a.y, a.z});
                    miss(mc, t);
                }
                it.remove();
                continue;
            }
            if (t.done) continue;
            t.path.add(new double[]{a.x, a.y, a.z});
            if (((ArrowEntityAccessor) a).endpractice$isInGround()) {
                t.done = true;
                if (!t.stale) miss(mc, t);
            } else if (randomMode && !t.stale && target != null && !target.removed) {
                // flown past the crystal: a miss straight away, without waiting for it to land
                Miss m = Miss.of(t.path, new double[]{target.x, target.y, target.z});
                if (m != null && m.passed) {
                    t.done = true;
                    miss(mc, t);
                }
            }
        }
    }

    private static void miss(Minecraft mc, Track t) {
        List<double[]> aims = new ArrayList<>();
        if (randomMode) {
            if (target != null) aims.add(new double[]{target.x, target.y, target.z});
        } else {
            for (EnderCrystalEntity c : crystals) if (!c.removed) aims.add(new double[]{c.x, c.y, c.z});
        }
        Miss best = null;
        for (double[] c : aims) {
            Miss m = Miss.of(t.path, c);
            if (m != null && (best == null || m.distance() < best.distance())) best = m;
        }
        missLine = best != null && best.distance() < 24 ? best.describe() : "nowhere near a crystal";
        EndPractice.chat(mc, "§cMiss§r: " + missLine);
        if (randomMode) endStreak(mc);
    }

    /** Where an arrow went past a crystal: at the point of its path closest to the crystal sideways. */
    public static final class Miss {
        /** Height of the arrow there above the crystal's middle (negative: below). */
        public final double up;
        /** How far right of the crystal it went, seen along the arrow's flight (negative: left). */
        public final double right;
        /** It stopped (hit a block) before it got level with the crystal. */
        public final boolean shortOf;
        /** It's already more than 3 blocks past the crystal: it can't hit it any more. */
        public final boolean passed;

        Miss(double up, double right, boolean shortOf, boolean passed) {
            this.up = up;
            this.right = right;
            this.shortOf = shortOf;
            this.passed = passed;
        }

        double distance() {
            return Math.sqrt(up * up + right * right);
        }

        /** The arrow's path (points a tick apart) against a crystal's middle. */
        public static Miss of(List<double[]> path, double[] c) {
            if (path.size() < 2) return null;
            double bestH = Double.MAX_VALUE, y = 0, side = 0, ahead = 0, beyond = 0;
            for (int i = 1; i < path.size(); i++) {
                double[] a = path.get(i - 1), b = path.get(i);
                double dx = b[0] - a[0], dz = b[2] - a[2];
                double len2 = dx * dx + dz * dz;
                if (len2 < 1e-9) continue;
                double f = ((c[0] - a[0]) * dx + (c[2] - a[2]) * dz) / len2;
                f = Math.max(0, Math.min(1, f));
                beyond = ((b[0] - c[0]) * dx + (b[2] - c[2]) * dz) / Math.sqrt(len2);
                double ox = a[0] + dx * f - c[0], oz = a[2] + dz * f - c[2];
                double h = Math.sqrt(ox * ox + oz * oz);
                if (h < bestH) {
                    bestH = h;
                    y = a[1] + (b[1] - a[1]) * f;
                    double len = Math.sqrt(len2);
                    double ux = dx / len, uz = dz / len;
                    // x is east and z is south: facing along (ux, uz), the right hand side is (-uz, ux)
                    side = -uz * ox + ux * oz;
                    // how much further on the crystal still was
                    ahead = -(ox * ux + oz * uz);
                }
            }
            if (bestH == Double.MAX_VALUE) return null;
            return new Miss(y - c[1], side, ahead > HIT_RADIUS, beyond > 3);
        }

        public String describe() {
            if (shortOf) {
                if (up < -HIT_RADIUS) return String.format(Locale.ROOT, "hit a block %.1f below it before it got there: aim §ehigher§r", -up);
                return "hit a block before it got there";
            }
            boolean high = up > HIT_RADIUS, low = up < -HIT_RADIUS;
            boolean wide = Math.abs(right) > HIT_RADIUS;
            if (!high && !low && !wide) {
                return String.format(Locale.ROOT, "only just past it (%.1f %s, %.1f %s)", Math.abs(up), up >= 0 ? "high" : "low",
                        Math.abs(right), right >= 0 ? "right" : "left");
            }
            List<String> where = new ArrayList<>();
            List<String> fix = new ArrayList<>();
            if (high) {
                where.add(String.format(Locale.ROOT, "%.1f high", up));
                fix.add("lower");
            }
            if (low) {
                where.add(String.format(Locale.ROOT, "%.1f low", -up));
                fix.add("higher");
            }
            if (wide) {
                where.add(String.format(Locale.ROOT, "%.1f %s", Math.abs(right), right > 0 ? "right" : "left"));
                fix.add(right > 0 ? "left" : "right");
            }
            String s = String.join(" and ", where) + ": aim §e" + String.join(" and ", fix) + "§r";
            if (!wide) s += " (left/right was fine)";
            else if (!high && !low) s += " (height was fine)";
            return s;
        }
    }

    // ---- ticks and HUD -----------------------------------------------------------------------------------------

    static void tick(Minecraft mc) {
        if (!active || mc.player == null || mc.world == null) return;
        if (mc.world.dimension.id != 1) return;
        removeDragon(mc); // in case its chunk loaded after the start
        long now = mc.world.getTime();
        if (randomMode) {
            tickRandom(mc, now);
            trackArrows(mc, now);
            return;
        }
        if (endTime >= 0) return;
        seeCrystals(mc);
        int nowDestroyed = 0;
        for (EnderCrystalEntity c : crystals) if (c.removed) nowDestroyed++;
        if (nowDestroyed > destroyed) crystalGoneTime = now;
        trackArrows(mc, now);
        for (int n = destroyed + 1; n <= nowDestroyed; n++) {
            if (startTime < 0) startTime = lastCrystalTime = now; // one went without an arrow seen
            lastLine = "Crystal " + n + "/" + crystals.size() + " " + EndPractice.time(now - startTime)
                    + " (+" + EndPractice.time(now - lastCrystalTime) + ")";
            EndPractice.chat(mc, lastLine);
            lastCrystalTime = now;
        }
        destroyed = nowDestroyed;
        if (destroyed > 0 && destroyed == crystals.size()) finish(mc, now);
    }

    private static void finish(Minecraft mc, long now) {
        endTime = now;
        long t = endTime - startTime;
        String line = "§aAll " + crystals.size() + " crystals in " + EndPractice.time(t) + "§r, " + arrows + " arrows ("
                + accuracy(destroyed) + "% hit, " + EndPractice.time(t / crystals.size()) + " per crystal)";
        if (Commands.cheated) {
            EndPractice.chat(mc, line + " §7(not saved: commands were used)§r");
            return;
        }
        if (pb < 0 || t < pb) {
            line += pb < 0 ? " §6first time§r" : " §6New PB!§r was " + EndPractice.time(pb);
            pb = t;
        } else {
            line += " PB " + EndPractice.time(pb) + " (+" + EndPractice.time(t - pb) + ")";
        }
        EndPractice.chat(mc, line);
        EndPractice.chat(mc, Config.keyName(EndPractice.cfg.keyCrystals) + " for a new island.");
        save(t);
    }

    private static int accuracy(int hit) {
        return arrows == 0 ? 0 : Math.round(100.0F * Math.min(hit, arrows) / arrows);
    }

    static List<String> hud(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (!active || mc.world == null) return lines;
        long now = mc.world.getTime();
        if (randomMode) {
            lines.add("§dRandom crystals§r  Streak §e" + streak + "§r" + (bestStreak > 0 ? "  §7best " + bestStreak + "§r" : ""));
            long t = roundStart < 0 || nextRoundAt >= 0 ? 0 : now - roundStart;
            lines.add("Crystal " + round + "  " + EndPractice.time(t)
                    + (hits > 0 ? "   §7avg " + EndPractice.time(hitTicks / hits) + "§r" : ""));
            lines.add("Hits " + hits + "   Arrows " + arrows + (arrows > 0 ? " (" + accuracy(hits) + "% hit)" : ""));
        } else {
            long t = startTime < 0 ? 0 : (endTime >= 0 ? endTime : now) - startTime;
            lines.add("§dCrystal trainer§r  " + EndPractice.time(t) + (pb >= 0 ? "  §7PB " + EndPractice.time(pb) + "§r" : ""));
            lines.add("Crystals " + destroyed + "/" + crystals.size() + "   Arrows " + arrows
                    + (arrows > 0 ? " (" + accuracy(destroyed) + "% hit)" : ""));
        }
        if (lastLine != null) lines.add(lastLine);
        if (missLine != null) lines.add("§cLast miss§r: " + missLine);
        return lines;
    }

    // ---- PB, config/endpractice-crystals.txt: time,ticks,crystals,arrows -------------------------------------------

    private static File file() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-crystals.txt");
    }

    private static void save(long ticks) {
        append(file(), String.format(Locale.ROOT, "%d,%d,%d,%d%n", System.currentTimeMillis(), ticks, crystals.size(), arrows));
    }

    private static void append(File f, String line) {
        if (Commands.cheated) return;
        try {
            f.getParentFile().mkdirs();
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            EndPractice.log("couldn't save to " + f + ": " + e);
        }
    }

    private static void loadPb() {
        if (pbLoaded) return;
        pbLoaded = true;
        pb = readBest(file(), true);
    }

    // ---- best streak, config/endpractice-streaks.txt: time,streak --------------------------------------------------

    private static File streakFile() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-streaks.txt");
    }

    private static void saveStreak(int n) {
        append(streakFile(), String.format(Locale.ROOT, "%d,%d%n", System.currentTimeMillis(), n));
    }

    private static void loadBest() {
        if (bestLoaded) return;
        bestLoaded = true;
        bestStreak = (int) readBest(streakFile(), false);
    }

    /** The lowest (or highest) second column of a file of comma separated lines, -1 for none. */
    private static long readBest(File f, boolean lowest) {
        long best = -1;
        try {
            if (!f.isFile()) return -1;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String[] p = line.split(",");
                if (p.length < 2) continue;
                try {
                    long t = Long.parseLong(p[1].trim());
                    if (best < 0 || (lowest ? t < best : t > best)) best = t;
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            EndPractice.log("couldn't read " + f + ": " + e);
        }
        return best;
    }
}
