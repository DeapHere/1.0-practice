package endpractice;

import endpractice.compat.Versions;
import endpractice.mixin.MinecraftAccessor;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SurvivalInteractionManager;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;
import net.minecraft.entity.mob.monster.EndermanEntity;
import net.minecraft.entity.EnderEyeEntity;
import net.minecraft.entity.mob.monster.boss.EnderDragonEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;

/**
 * Endgame practice: a new world with a flat plains or desert spawn and the tower kit, then split by split:
 * tower build (until you die in the pit), portal room, entering the End, killing the dragon. The tower build can also
 * be practiced on its own. The world has structures on, so the stronghold is the seed's real one.
 */
public final class Endgame {
    public static final String FOLDER = "EndgamePractice";

    public enum Split {
        TOWER("Tower build"), PORTAL_ROOM("Portal room"), ENTER_END("Enter End"), DRAGON("Dragon");

        final String label;

        Split(String label) {
            this.label = label;
        }
    }

    /** FULL: tower build to dragon. TOWER: just the tower build. STRONGHOLD: from the stronghold kit to the dragon. */
    public enum Mode {
        FULL, TOWER, STRONGHOLD
    }

    private static boolean active;
    private static boolean towerOnly;
    private static Mode mode = Mode.FULL;
    private static long startTime;
    private static long splitStart;
    private static int current; // index into Split.values(); == length when the run is over
    private static final long[] segment = new long[Split.values().length];
    /** World time each split finished, to show it for a few seconds after. */
    private static final long[] splitDoneAt = new long[Split.values().length];
    private static final long SPLIT_SHOWN_TICKS = 200;
    private static String result;
    private static long endTime;
    private static boolean wasDead;
    private static boolean respawned;
    /** The tower split only counts if you got at least this high above the ground before dying. */
    private static final int TOWER_MIN_HEIGHT = 20;
    private static double groundY;
    private static double towerTop;
    /** Ticks from night starting to the tower death reset (negative: before night). MIN_VALUE until then. */
    private static long nightAtDeath;
    private static int attempts;
    /** PBs per mode: "FULL/PORTAL_ROOM", and "FULL/RUN" for a whole attempt that killed the dragon. */
    private static final Map<String, Long> best = new HashMap<>();

    private static String key(Mode m, String what) {
        return m.name() + "/" + what;
    }
    private static boolean bestLoaded;

    private Endgame() {
    }

    public static boolean isActive() {
        return active;
    }

    /** True while the worlds for a new attempt are being made and checked. */
    static boolean preparing;

    /** In the End during a full run: the bed report and End HUD apply. */
    public static boolean inEndFight(Minecraft mc) {
        return active && !towerOnly && mc.world != null && mc.world.dimension.id == 1;
    }

    /** The pit plan shows while you build: a tower build or full run, until your death reset. */
    static boolean showPitPlan() {
        // only until the death reset in the pit: after that the pit is dug and the lines are in the way
        return active && mode != Mode.STRONGHOLD && !nightDone && EndPractice.cfg.pitPlanner
                && current < segment.length && Split.values()[current] == Split.TOWER;
    }

    static void stop() {
        active = false;
    }

    // ---- enderman tracker: endermen per full endgame run, config/endpractice-endermen.txt -------------------------

    /** Endermen seen in the overworld this run (each counted once). */
    private static final Set<Object> seenEndermen = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean endermenRecorded;
    /** The night is over or the first eye was thrown: endermen aren't counted or shown any more. */
    private static boolean nightDone;
    /** Totals over every recorded run: runs, spawned, killed, dropped, pearls. */
    private static long[] endermanTotals;

    /** Endermen that were under the open sky when first seen (not in a cave). */
    private static int surfaceEndermen;
    /** Death resets (die, respawn on the tower) in the overworld this run before the night was over: the cycles. */
    private static int cycles;
    private static final Set<Object> surfaceSeen = Collections.newSetFromMap(new IdentityHashMap<>());

    private static boolean withinRange(Minecraft mc, EndermanEntity e) {
        int r = EndPractice.cfg.endermanRange;
        if (r <= 0) return true;
        double dx = e.x - mc.player.x, dy = e.y - mc.player.y, dz = e.z - mc.player.z;
        return dx * dx + dy * dy + dz * dz <= (double) r * r;
    }

    /** Nothing above the enderman's head but air, leaves, plants, snow or vines: it's on the surface, not in a cave. */
    private static boolean openSky(Minecraft mc, EndermanEntity e) {
        int x = (int) Math.floor(e.x), z = (int) Math.floor(e.z);
        for (int y = (int) Math.floor(e.shape.maxY) + 1; y < 128; y++) {
            int id = mc.world.getBlock(x, y, z);
            if (id != 0 && id != 18 && id != 31 && id != 32 && id != 37 && id != 38 && id != 78 && id != 106) return false;
        }
        return true;
    }

    /** It has been dark enough for monsters to spawn on the surface this run. */
    private static boolean sawDark;

    /**
     * Counts the endermen, and with the tracker on saves a full endgame run's endermen once the night is over or the
     * first eye of ender is thrown, whichever comes first. Runs reset before that aren't saved.
     */
    private static void trackEndermen(Minecraft mc) {
        if (mc.world.dimension.id != 0 || nightDone) return;
        boolean eye = false;
        for (Object o : mc.world.entities) {
            if (!(o instanceof EndermanEntity)) continue;
            seenEndermen.add(o);
            // surface: under the open sky and within the range (87: as far as endermen are drawn), counted once
            EndermanEntity e = (EndermanEntity) o;
            if (!surfaceSeen.contains(o) && withinRange(mc, e) && (mc.world.getTime() + e.networkId) % 5 == 0 && openSky(mc, e)) {
                surfaceSeen.add(o);
                surfaceEndermen++;
            }
            if (o instanceof EnderEyeEntity) eye = true;
        }
        // monsters can spawn on the surface while the sky is dark enough: sky light 7 or less, which is ambient
        // darkness 8 or more (13187 to 22811 in clear weather; rain makes it longer, thunder lets them spawn any time).
        // The night is over the first tick after that, once it has been dark this run.
        boolean dark = mc.world.ambientDarkness >= 8 || mc.world.isThundering();
        if (dark) sawDark = true;
        String why = eye ? "first eye" : sawDark && !dark ? "night over" : null;
        if (why != null) {
            nightDone = true;
            recordEndermen(mc, why);
        }
    }

    private static void recordEndermen(Minecraft mc, String why) {
        if (endermenRecorded || !EndPractice.cfg.trackerOn() || mode != Mode.FULL || Commands.cheated) return;
        endermenRecorded = true;
        long[] t = endermanTotals();
        t[0]++;
        t[1] += seenEndermen.size();
        t[2] += endermen;
        t[3] += dropped;
        t[4] += pearls;
        t[5] += surfaceEndermen;
        t[6]++;
        t[7] += cycles;
        t[8]++;
        try {
            File f = endermenFile();
            f.getParentFile().mkdirs();
            String line = String.format(Locale.ROOT, "%d,%d,%d,%d,%d,%d,%d%n", System.currentTimeMillis(), seenEndermen.size(), endermen, dropped, pearls, surfaceEndermen, cycles);
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            EndPractice.log("couldn't save the enderman stats: " + e);
        }
        if (mc.world != null) {
            EndPractice.chat(mc, "§dEndermen§r (" + why + "): " + seenEndermen.size() + " spawned (" + surfaceEndermen + " surface), "
                    + endermen + " killed, " + pearls + " pearls" + (EndPractice.cfg.cycleTracker ? ", " + cycles + " cycles" : "")
                    + ". §7Avg over " + t[0] + ": " + endermanAverages() + "§r");
        }
    }

    private static String avg(long total, long runs) {
        return String.format(Locale.ROOT, "%.1f", (double) total / runs);
    }

    private static String endermanAverages() {
        long[] t = endermanTotals();
        if (t[0] == 0) return "no runs yet";
        return avg(t[1], t[0]) + (t[6] > 0 ? " (" + avg(t[5], t[6]) + ")" : "") + ", " + avg(t[2], t[0]) + " killed, " + avg(t[4], t[0]) + " pearls ("
                + rate((int) t[3], (int) t[2]) + " dropped)" + (EndPractice.cfg.cycleTracker && t[8] > 0 ? ", " + avg(t[7], t[8]) + " cycles" : "");
    }

    /** The PB screen: every mode's whole run and split PBs. */
    static List<String> pbLines() {
        loadBest();
        List<String> out = new ArrayList<>();
        for (Mode m : Mode.values()) {
            String name = m == Mode.FULL ? "Full endgame" : m == Mode.TOWER ? "Tower build only" : "Stronghold to dragon";
            Long run = best.get(key(m, "RUN"));
            out.add("§d" + name + "§r" + (m != Mode.TOWER ? "   run " + (run != null ? "§e" + EndPractice.time(run) + "§r" : "§7none§r") : ""));
            StringBuilder sb = new StringBuilder();
            long sum = 0;
            boolean all = true;
            for (Split sp : Split.values()) {
                if (m == Mode.TOWER && sp != Split.TOWER || m == Mode.STRONGHOLD && sp == Split.TOWER) continue;
                Long pb = best.get(key(m, sp.name()));
                if (sb.length() > 0) sb.append("   ");
                sb.append(sp.label).append(" ").append(pb != null ? "§e" + EndPractice.time(pb) + "§r" : "§7-§r");
                if (pb != null) sum += pb; else all = false;
            }
            out.add(sb.toString());
            if (all && m != Mode.TOWER) out.add("§7Sum of best " + EndPractice.time(sum) + "§r");
            out.add("");
        }
        return out;
    }

    /** The stats screen: the enderman tracker's averages, range and last runs, and the best crystal streak. */
    static List<String> statsLines() {
        endermanTotals = null; // read the file again
        long[] t = endermanTotals();
        List<String> out = new ArrayList<>();
        out.add("§dEnderman tracker§r (full endgame, saved at night end or the first eye): "
                + (EndPractice.cfg.trackerOn() ? "§aon§r" : "§coff§r, turn it on in the Lab (needs vanilla pearl luck)"));
        out.add("");
        if (t[0] == 0) {
            out.add("No runs saved yet.");
        } else {
            List<long[]> runs = new ArrayList<>();
            try {
                for (String line : Files.readAllLines(endermenFile().toPath(), StandardCharsets.UTF_8)) {
                    String[] p = line.split(",");
                    if (p.length < 5) continue;
                    try {
                        long[] r = new long[7];
                        for (int i = 0; i < 5; i++) r[i] = Long.parseLong(p[i].trim());
                        r[5] = p.length >= 6 ? Long.parseLong(p[5].trim()) : -1;
                        r[6] = p.length >= 7 ? Long.parseLong(p[6].trim()) : -1;
                        runs.add(r);
                    } catch (NumberFormatException ignored) {
                    }
                }
            } catch (Exception e) {
                EndPractice.log("couldn't read the enderman stats: " + e);
            }
            long min = Long.MAX_VALUE, max = 0;
            for (long[] r : runs) {
                min = Math.min(min, r[1]);
                max = Math.max(max, r[1]);
            }
            out.add("Runs saved: §e" + t[0] + "§r");
            out.add("Spawned: avg §e" + avg(t[1], t[0]) + "§r" + (t[6] > 0 ? "   surface avg §e" + avg(t[5], t[6]) + "§r" : "")
                    + "   §7(" + min + " to " + max + ")§r");
            out.add("Killed: avg §e" + avg(t[2], t[0]) + "§r   Pearls: avg §e" + avg(t[4], t[0]) + "§r   Drop rate §e"
                    + rate((int) t[3], (int) t[2]) + "§r");
            if (t[8] > 0) {
                long cmin = Long.MAX_VALUE, cmax = 0;
                for (long[] r : runs) if (r[6] >= 0) { cmin = Math.min(cmin, r[6]); cmax = Math.max(cmax, r[6]); }
                out.add("Cycles a night (death resets): avg §e" + avg(t[7], t[8]) + "§r   §7(" + cmin + " to " + cmax + ")§r");
            }
            out.add("");
            out.add("§7Last runs: spawned (surface), killed, pearls, cycles§r");
            java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("d MMM HH:mm", Locale.ROOT);
            for (int i = runs.size() - 1; i >= 0 && i >= runs.size() - 8; i--) {
                long[] r = runs.get(i);
                out.add(f.format(new java.util.Date(r[0])) + "   " + r[1] + (r[5] >= 0 ? " (" + r[5] + ")" : "") + ", " + r[2] + ", " + r[4] + (r[6] >= 0 ? ", " + r[6] : ""));
            }
        }
        out.add("");
        out.add("§7Runs are in config/endpractice-endermen.txt. Delete it to start over.§r");
        int streak = CrystalTrainer.bestStreak();
        if (streak > 0) out.add("Best random crystal streak: §e" + streak + "§r");
        return out;
    }

    private static File endermenFile() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-endermen.txt");
    }

    /** time,spawned,killed,dropped,pearls[,surface[,cycles]] per line (surface since 0.5.1, cycles since 1.0.0). */
    private static long[] endermanTotals() {
        if (endermanTotals != null) return endermanTotals;
        endermanTotals = new long[9];
        try {
            File f = endermenFile();
            if (f.isFile()) {
                for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                    String[] p = line.split(",");
                    if (p.length < 5) continue;
                    try {
                        long[] v = new long[4];
                        for (int i = 0; i < 4; i++) v[i] = Long.parseLong(p[i + 1].trim());
                        endermanTotals[0]++;
                        for (int i = 0; i < 4; i++) endermanTotals[i + 1] += v[i];
                        if (p.length >= 6) {
                            endermanTotals[5] += Long.parseLong(p[5].trim());
                            endermanTotals[6]++;
                        }
                        if (p.length >= 7) {
                            endermanTotals[7] += Long.parseLong(p[6].trim());
                            endermanTotals[8]++;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            EndPractice.log("couldn't read the enderman stats: " + e);
        }
        return endermanTotals;
    }

    /** Pearls owed so far, in 1/den of a pearl: average or lucky give the same share of every kill, never more or less. */
    private static int pearlOwed;
    private static int endermen, pearls, dropped;

    /** Pearls an enderman drops with average or lucky pearls, or -1 for vanilla's random amount. */
    public static int averagePearls(int bound) {
        if (!active) return -1;
        String luck = EndPractice.cfg.pearlLuck;
        int num, den; // pearls per enderman with no looting: average 1/2, lucky 2/3
        if (luck.equals("average")) {
            num = 1;
            den = 2;
        } else if (luck.equals("lucky")) {
            num = 2;
            den = 3;
        } else {
            return -1;
        }
        pearlOwed += num * (bound - 1); // looting raises vanilla's bound, and the share with it
        int n = pearlOwed / den;
        pearlOwed -= n * den;
        return n;
    }

    /** An enderman killed by the player during an endgame attempt, and the pearls it dropped (any pearl luck). */
    public static void onEndermanKilled(int n) {
        if (!active) return;
        endermen++;
        pearls += n;
        if (n > 0) {
            dropped++;
        }
        EndPractice.log("enderman " + endermen + ": " + n + " pearl" + (n == 1 ? "" : "s") + ", " + pearls + " so far");
    }

    private static String rate(int part, int whole) {
        return whole == 0 ? "-" : Math.round(100.0 * part / whole) + "%";
    }

    static Mode mode() {
        return mode;
    }

    static void start(Minecraft mc, Mode m) {
        loadBest();
        attempts++;
        mode = m;
        boolean onlyTower = m == Mode.TOWER;
        towerOnly = onlyTower;
        EndPractice.log("starting endgame attempt " + attempts + " (" + m + ")");
        BedReport.clear();
        active = false;
        mc.setWorld(null);
        mc.interactionManager = new SurvivalInteractionManager(mc);
        preparing = true;
        try {
            makeWorld(mc);
        } finally {
            preparing = false;
        }
        mc.openScreen(null);


        PlayerEntity p = mc.player;
        EndPractice.giveKit(p, m == Mode.STRONGHOLD ? EndPractice.cfg.strongholdKit : EndPractice.cfg.towerKit);
        p.setHealth(20);
        p.getHungerManager().add(20, 20.0F);

        long setTo = m == Mode.STRONGHOLD ? EndPractice.cfg.strongholdStartTime : EndPractice.cfg.towerStartTime;
        if (setTo >= 0) mc.world.setTime(setTo);
        nightAtDeath = Long.MIN_VALUE;
        active = true;
        startTime = splitStart = mc.world.getTime();
        current = m == Mode.STRONGHOLD ? Split.PORTAL_ROOM.ordinal() : 0;
        for (int i = 0; i < segment.length; i++) segment[i] = -1;
        result = null;
        wasDead = false;
        respawned = false;
        pearlOwed = 1; // the first enderman drops one
        endermen = pearls = dropped = 0;
        seenEndermen.clear();
        surfaceEndermen = 0;
        cycles = 0;
        lowestSince = respawnTarget = -1;
        surfaceSeen.clear();
        endermenRecorded = false;
        nightDone = false;
        sawDark = false;
        groundY = towerTop = p.y;
        if (EndPractice.cfg.pitPlanner && m != Mode.STRONGHOLD) {
            net.minecraft.util.math.Vec3i sp = mc.world.getSpawnPoint();
            PitPlanner.plan(mc, sp.x, sp.z, false);
        } else {
            PitPlanner.clear();
        }
        String what = m == Mode.TOWER ? ", tower build only. " : m == Mode.STRONGHOLD ? ", stronghold to dragon. " : ". ";
        EndPractice.chat(mc, "§dEndgame practice§r attempt " + attempts + what
                + Config.keyName(restartKey()) + " restarts, " + Config.keyName(EndPractice.cfg.keySaveKit)
                + " twice saves your inventory as the " + (m == Mode.STRONGHOLD ? "stronghold" : "tower") + " kit.");
    }

    /** The eye throw trainer's world: the same as an endgame one, a flat spot 128 to 384 blocks from a stronghold. */
    static void makeTrainerWorld(Minecraft mc) {
        mc.setWorld(null);
        mc.interactionManager = new SurvivalInteractionManager(mc);
        preparing = true;
        trainerSpot = true;
        // half the time a spot where an eye thrown from the ground goes to the wrong stronghold, so triangulating matters
        trainerWantsBug = Integer.getInteger("endpractice.eyes", 0) == 2 || EndPractice.cfg.vanillaEyes && new Random().nextBoolean(); // 2: the self test wants a bugged spot
        spotStronghold = null;
        try {
            makeWorld(mc);
        } finally {
            preparing = false;
            trainerSpot = false;
        }
        mc.openScreen(null);
    }

    /**
     * A practice world from the seeds given, each checked by the finder (which may generate terrain) until one gives a
     * spot, then the player is put there and the world is prepared as usual. Returns the spot, or null.
     */
    static Vec3i makeCustomWorld(Minecraft mc, String name, java.util.function.LongSupplier seeds, java.util.function.Function<World, Vec3i> finder, int maxTries) {
        mc.setWorld(null);
        mc.interactionManager = new SurvivalInteractionManager(mc);
        preparing = true;
        Vec3i found = null;
        try {
            long t0 = System.currentTimeMillis();
            int tries = 0;
            while (true) {
                tries++;
                mc.getWorldStorageSource().delete(FOLDER);
                EndPractice.resetBiomeArrayPool();
                EndPractice.skipOverworldPrep = true;
                try {
                    mc.startGame(FOLDER, name, Versions.newWorldSettings(seeds.getAsLong(), true));
                } finally {
                    EndPractice.skipOverworldPrep = false;
                }
                found = finder.apply(mc.world);
                if (found != null || tries >= maxTries) break;
                mc.setWorld(null);
            }
            EndPractice.log(name + ": spot after " + tries + " worlds, " + (System.currentTimeMillis() - t0) + " ms" + (found == null ? ", none found" : ""));
            if (found != null) {
                mc.world.setSpawnPoint(found);
                PlayerEntity p = mc.player;
                p.setPositionAndAngles(found.x + 0.5, found.y + 0.01, found.z + 0.5, p.yaw, 0.0F);
                p.velocityX = p.velocityY = p.velocityZ = 0.0;
                p.fallDistance = 0.0F;
            }
            ((MinecraftAccessor) mc).endpractice$prepareWorld("Building terrain");
        } finally {
            preparing = false;
        }
        mc.openScreen(null);
        return found;
    }

    /** The pit sandbox's world: the same kind of flat spot as an endgame one. */
    static void makeSandboxWorld(Minecraft mc) {
        mc.setWorld(null);
        mc.interactionManager = new SurvivalInteractionManager(mc);
        preparing = true;
        try {
            if (PitSandbox.flatGen) makeFlatWorld(mc);
            else makeWorld(mc);
        } finally {
            preparing = false;
        }
        mc.openScreen(null);
    }

    /** The pit sandbox as a flat sand desert, spawn at 0 0. */
    private static void makeFlatWorld(Minecraft mc) {
        mc.getWorldStorageSource().delete(FOLDER);
        EndPractice.resetBiomeArrayPool();
        EndPractice.skipOverworldPrep = true;
        try {
            mc.startGame(FOLDER, "Endgame Practice", Versions.newWorldSettings(new Random().nextLong(), false));
        } finally {
            EndPractice.skipOverworldPrep = false;
        }
        Vec3i spawn = new Vec3i(0, PitSandbox.FLAT_TOP + 1, 0);
        mc.world.setSpawnPoint(spawn);
        PlayerEntity p = mc.player;
        p.setPositionAndAngles(0.5, spawn.y + p.eyeHeight + 0.1, 0.5, p.yaw, 0.0F);
        p.velocityX = p.velocityY = p.velocityZ = 0.0;
        p.fallDistance = 0.0F;
        EndPractice.log("flat desert sandbox");
        ((MinecraftAccessor) mc).endpractice$prepareWorld("Building terrain");
    }

    private static final List<Biome> FLAT_BIOMES = Arrays.asList(Biome.PLAINS, Biome.DESERT);

    private static void makeWorld(Minecraft mc) {
        Random random = new Random();
        long t0 = System.currentTimeMillis();
        int tries = 0;
        Vec3i found = null;
        while (true) {
            tries++;
            mc.getWorldStorageSource().delete(FOLDER);
            EndPractice.resetBiomeArrayPool();
            EndPractice.skipOverworldPrep = true;
            try {
                mc.startGame(FOLDER, "Endgame Practice", Versions.newWorldSettings(random.nextLong(), true));
            } finally {
                EndPractice.skipOverworldPrep = false;
            }
            found = findFlatSpot(mc.world);
            if (found != null || tries >= 60) break;
            mc.setWorld(null);
        }
        EndPractice.log("spawn after " + tries + " worlds, " + (System.currentTimeMillis() - t0) + " ms"
                + (found == null ? ", no flat plains or desert found" : ""));
        if (found != null) {
            mc.world.setSpawnPoint(found);
            PlayerEntity p = mc.player;
            p.setPositionAndAngles(found.x + 0.5, found.y + p.eyeHeight + 0.1, found.z + 0.5, p.yaw, 0.0F);
            p.velocityX = p.velocityY = p.velocityZ = 0.0;
            p.fallDistance = 0.0F;
        }
        // the eye trainer: forget the strongholds the search laid out (all three), so eyes work as in a fresh world,
        // where only strongholds near what's been generated count and the rest are the three spots at y 64
        if (trainerSpot) EyeTrainer.forgetStrongholds(mc.world);
        // now the normal Building terrain and Simulating world for the accepted world
        ((MinecraftAccessor) mc).endpractice$prepareWorld("Building terrain");
    }

    /**
     * A flat, dry plains or desert spot 128 to 384 blocks from one of the world's strongholds, so the stronghold is
     * never more than about 400 blocks away. The biome map is cheap to read, so only a few likely spots have their
     * terrain generated and checked.
     */
    private static Vec3i findFlatSpot(World world) {
        Vec3i spawn = world.getSpawnPoint();
        BlockPos first = world.findNearestStructure("Stronghold", spawn.x, 64, spawn.z);
        if (first == null) return null;
        // the world's three strongholds are about 120 degrees apart around 0,0: find the other two the same way
        List<BlockPos> strongholds = new ArrayList<>();
        strongholds.add(first);
        double angle = Math.atan2(first.z, first.x), dist = Math.sqrt((double) first.x * first.x + (double) first.z * first.z);
        for (int i = 1; i < 3; i++) {
            double a = angle + i * Math.PI * 2 / 3;
            BlockPos sh = world.findNearestStructure("Stronghold", (int) (Math.cos(a) * dist), 64, (int) (Math.sin(a) * dist));
            if (sh != null && !strongholds.contains(sh)) strongholds.add(sh);
        }
        BiomeSource biomes = world.getBiomeSource();
        for (BlockPos sh : strongholds) {
            Vec3i v = nearStronghold(world, biomes, sh);
            if (v != null) return v;
        }
        return null;
    }

    /** Making the eye trainer's world: spots 150 to 600 blocks out, where the eye bug can happen (see trainerOk). */
    private static boolean trainerSpot, trainerWantsBug;
    /** The stronghold the last spot was picked around (the one the eyes should find). */
    public static BlockPos spotStronghold;

    /**
     * The eye trainer: an eye thrown from some height between 64 and 100 at the spot finds this stronghold (so it can
     * be done), and if a bugged spot is wanted, one thrown from the ground (y 64) goes to another one.
     */
    private static boolean trainerOk(World world, BlockPos sh, int x, int z, boolean wantBug) {
        // a fresh spot: nothing has generated near it yet, so the eye picks from the three stronghold spots (y 64)
        boolean anyRight = false;
        for (int y = 64; y <= 100 && !anyRight; y += 2) {
            BlockPos f = EyeTrainer.eyeTarget(world, x, y, z, true);
            if (f != null && Math.abs(f.x - sh.x) <= 64 && Math.abs(f.z - sh.z) <= 64) anyRight = true;
        }
        if (!anyRight) return false;
        if (!wantBug) return true;
        BlockPos g = EyeTrainer.eyeTarget(world, x, 66, z, true);
        return g != null && (Math.abs(g.x - sh.x) > 64 || Math.abs(g.z - sh.z) > 64);
    }

    private static Vec3i nearStronghold(World world, BiomeSource biomes, BlockPos sh) {
        // random spots, spread evenly over the ring, so the stronghold can be any way from the tower and anywhere from 128
        // to 384 blocks off (a fixed search order always put it 128 blocks east, closer than the pit, so the eye from the
        // pit always pointed back)
        Random random = new Random();
        int checked = 0;
        // the eye trainer: past 300 blocks, so the stronghold hasn't generated when the area round you loads (an eye
        // always finds one that has, wherever you throw from), which is when the eye bug can happen
        double rMin = trainerSpot ? 300 : 128, rMax = trainerSpot ? 650 : 384;
        for (int tries = 0; tries < 400 && checked < 6; tries++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(rMin * rMin + random.nextDouble() * (rMax * rMax - rMin * rMin));
            int x = sh.x + (int) Math.round(Math.cos(a) * r), z = sh.z + (int) Math.round(Math.sin(a) * r);
            if (!biomes.isBiomeWithin(x, z, 20, FLAT_BIOMES) || waterNear(biomes, x, z)) continue;
            // the eye trainer wants a bugged spot for the first 200 tries, then takes any spot that can be done
            if (trainerSpot ? !trainerOk(world, sh, x, z, trainerWantsBug && tries < 200) : !eyesFind(world, sh, x, z)) continue;
            checked++;
            Integer y = flatAt(world, x, z);
            if (y != null && dryAround(world, x, z)) {
                EndPractice.log("spawn " + (int) r + " blocks from the stronghold");
                spotStronghold = sh;
                return new Vec3i(x, y, z);
            }
        }
        return null;
    }

    /**
     * Whether an eye of ender thrown at the spot, or anywhere 205 blocks around it (where the pit could be), goes to this
     * stronghold. 1.0's nearest stronghold search has a bug (it adds dx * dy^2 instead of squaring dx, so the height you throw from matters too), so it can pick
     * one over 600 blocks away, mostly to the west, over the one right next to you.
     */
    private static boolean eyesFind(World world, BlockPos sh, int x, int z) {
        for (int i = -1; i < 8; i++) {
            int px = x, pz = z;
            if (i >= 0) {
                px += (int) (Math.cos(i * Math.PI / 4) * 205);
                pz += (int) (Math.sin(i * Math.PI / 4) * 205);
            }
            // on the ground, and (at the tower) from the top too, since the height changes where the eye goes
            for (int y = 64; y <= (i < 0 ? 128 : 64); y += 32) {
                BlockPos found = world.findNearestStructure("Stronghold", px, y, pz);
                if (found == null || Math.abs(found.x - sh.x) > 64 || Math.abs(found.z - sh.z) > 64) return false;
            }
        }
        return true;
    }

    private static final List<Biome> WATER_BIOMES = Arrays.asList(Biome.OCEAN, Biome.FROZEN_OCEAN, Biome.RIVER,
            Biome.FROZEN_RIVER, Biome.SWAMPLAND);

    /** Ocean, river or swamp within 96 blocks. */
    private static boolean waterNear(BiomeSource biomes, int x, int z) {
        for (int dx = -96; dx <= 96; dx += 8) {
            for (int dz = -96; dz <= 96; dz += 8) {
                if (WATER_BIOMES.contains(biomes.getBiome(x + dx, z + dz))) return true;
            }
        }
        return false;
    }

    /** Under 1% of the surface within 48 blocks is water or lava (generates those chunks). */
    private static boolean dryAround(World world, int sx, int sz) {
        int wet = 0, all = 0;
        for (int dx = -48; dx <= 48; dx += 4) {
            for (int dz = -48; dz <= 48; dz += 4) {
                int x = sx + dx, z = sz + dz;
                world.getBlock(x, 64, z);
                all++;
                if (world.getMaterial(x, world.getHeight(x, z) - 1, z).isLiquid()) wet++;
            }
        }
        return wet * 100 < all;
    }

    /** Dry and within 2 blocks of level in the 21x21 around x, z: the height there, or null. */
    private static Integer flatAt(World world, int sx, int sz) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -10; dx <= 10; dx += 2) {
            for (int dz = -10; dz <= 10; dz += 2) {
                int x = sx + dx, z = sz + dz;
                world.getBlock(x, 64, z); // loads (generates) the chunk
                int y = world.getHeight(x, z);
                Material m = world.getMaterial(x, y - 1, z);
                if (m.isLiquid() || m == Material.LEAVES || m == Material.WOOD) return null;
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        if (max - min > 2) return null;
        return world.getHeight(sx, sz);
    }

    static void tick(Minecraft mc) {
        if (!active || mc.player == null || mc.world == null) return;
        PitPlanner.tick(mc);
        trackEndermen(mc);
        if (current >= segment.length) {
            if (mc.world.dimension.id == 1) BedReport.tick(mc);
            return;
        }
        Split split = Split.values()[current];
        boolean dead = mc.player.getHealth() <= 0;
        if (mc.world.dimension.id == 0) {
            if (respawnTarget >= 0 && lowestSince < 0 && mc.world.getTime() >= respawnTarget) lowestSince = respawnTarget;
        }
        // a cycle: every death reset in the overworld until the night is over (the tower death is the first one).
        // An accident on the way up (under the tower height) ends the attempt and isn't one.
        if (dead && !wasDead && !nightDone && mc.world.dimension.id == 0 && (split != Split.TOWER || towerTop - groundY >= TOWER_MIN_HEIGHT)) cycles++;
        switch (split) {
            case TOWER:
                if (!dead) towerTop = Math.max(towerTop, mc.player.y);
                if (dead && towerTop - groundY < TOWER_MIN_HEIGHT) {
                    // an accident on the way up, not the death reset in the pit
                    current = segment.length;
                    endTime = mc.world.getTime();
                    result = "Died " + (int) (towerTop - groundY) + " blocks up, not counted";
                    EndPractice.chat(mc, "Died " + (int) (towerTop - groundY) + " blocks up, before the tower was built, so it doesn't count. "
                            + Config.keyName(restartKey()) + " for a new attempt.");
                    break;
                }
                if (dead) {
                    nightAtDeath = mc.world.getTime() - EndPractice.cfg.nightTime;
                    finishSplit(mc, split);
                    EndPractice.chat(mc, "Death reset " + nightText(nightAtDeath) + ".");
                    if (towerOnly) end(mc, "Tower build done");
                }
                break;
            case PORTAL_ROOM:
                if (wasDead && !dead && !respawned) {
                    // respawned on top of the tower: the stronghold part starts now
                    respawned = true;
                    if (EndPractice.cfg.respawnTimer && mode != Mode.STRONGHOLD && respawnTarget >= 0) {
                        long off = mc.world.getTime() - respawnTarget;
                        EndPractice.chat(mc, "Respawned " + (Math.abs(off) <= 20 ? "§aright at the lowest light§r"
                                : Math.abs(off) + " ticks (" + EndPractice.time(Math.abs(off)) + ") " + (off < 0 ? "before" : "after") + " the lowest light") + ".");
                    }
                    splitStart = mc.world.getTime();
                }
                // later deaths are death resets in the pit (checking pearls, refreshing items): the run goes on
                if (!dead && mc.world.getTime() % 5 == 0 && nearPortalFrame(mc)) finishSplit(mc, split);
                if (mc.world.dimension.id == 1) {
                    // straight into the End without the portal room being seen (a portal room split of 0)
                    finishSplit(mc, split);
                }
                break;
            case ENTER_END:
                if (mc.world.dimension.id == 1) finishSplit(mc, split);
                break;
            case DRAGON:
                BedReport.tick(mc);
                if (dead) {
                    end(mc, "You died in the End");
                    break;
                }
                EnderDragonEntity d = EndPractice.dragon(mc);
                if (d != null && d.getHealth() <= 0) {
                    finishSplit(mc, split);
                    end(mc, "Dragon killed");
                    finishRun(mc);
                }
                break;
        }
        wasDead = dead;
    }

    /** 1.0's World.calculateAmbientDarkness at another world time, with the weather as it is now (0 day to 11 darkest). */
    private static int darknessAt(Minecraft mc, long time) {
        float f = mc.world.dimension.getTimeOfDay(time, 0.0F);
        float g = 1.0F - (net.minecraft.util.math.MathHelper.cos(f * (float) Math.PI * 2.0F) * 2.0F + 0.5F);
        g = Math.max(0.0F, Math.min(1.0F, g));
        g = 1.0F - g;
        g = (float) (g * (1.0 - mc.world.getRain(0.0F) * 5.0F / 16.0));
        g = (float) (g * (1.0 - mc.world.getThunder(0.0F) * 5.0F / 16.0));
        g = 1.0F - g;
        return (int) (g * 11.0F);
    }

    /** The first tick from now (up to half a day) the sky light is at its lowest for tonight, or now if it already is. */
    private static long lowestLightAt(Minecraft mc) {
        long now = mc.world.getTime();
        int max = 0;
        for (long t = now; t < now + 12000; t += 20) max = Math.max(max, darknessAt(mc, t));
        if (darknessAt(mc, now) >= max) return lowestSince >= 0 ? lowestSince : now;
        long t = now;
        while (t < now + 12000 && darknessAt(mc, t) < max) t += 20;
        while (t > now && darknessAt(mc, t - 1) >= max) t--; // the exact tick
        return t;
    }

    /** The first tick the light has been at its lowest this night (-1 before), and the target when you died. */
    private static long lowestSince = -1, respawnTarget = -1;

    private static String respawnLine(Minecraft mc) {
        long now = mc.world.getTime();
        long target = lowestLightAt(mc);
        respawnTarget = target;
        if (now >= target) {
            long late = now - target;
            return late <= 20 ? "§a>> RESPAWN NOW: lowest light <<§r" : "§aRespawn: lowest light for " + EndPractice.time(late) + "§r";
        }
        int sky = 15 - mc.world.ambientDarkness;
        return "Respawn in §e" + (target - now) + " ticks§r (" + EndPractice.time(target - now) + ")  §7sky light " + sky + "§r";
    }

    private static boolean nearPortalFrame(Minecraft mc) {
        int px = (int) Math.floor(mc.player.x), py = (int) Math.floor(mc.player.y), pz = (int) Math.floor(mc.player.z);
        int id = Block.END_PORTAL_FRAME.id;
        for (int x = px - 4; x <= px + 4; x++) {
            for (int y = py - 3; y <= py + 1; y++) {
                for (int z = pz - 4; z <= pz + 4; z++) {
                    if (mc.world.getBlock(x, y, z) == id) return true;
                }
            }
        }
        return false;
    }

    private static void finishSplit(Minecraft mc, Split split) {
        long now = mc.world.getTime();
        long seg = now - splitStart;
        segment[split.ordinal()] = seg;
        splitDoneAt[split.ordinal()] = now;
        splitStart = now;
        current = split.ordinal() + 1;
        Long pb = best.get(key(mode, split.name()));
        String line = "§e" + split.label + "§r " + EndPractice.time(seg) + " (run " + EndPractice.time(now - startTime) + ")";
        if (Commands.cheated) {
            EndPractice.chat(mc, line + " §7(not saved: commands were used)§r");
            return;
        }
        if (pb == null || seg < pb) {
            line += pb == null ? " §6first time§r" : " §6PB!§r was " + EndPractice.time(pb);
            best.put(key(mode, split.name()), seg);
        } else {
            line += " PB " + EndPractice.time(pb) + " (+" + EndPractice.time(seg - pb) + ")";
        }
        EndPractice.chat(mc, line);
        appendSplit(split.name(), seg);
    }

    /** The whole attempt's time against this mode's PB. */
    private static void finishRun(Minecraft mc) {
        long total = endTime - startTime;
        if (Commands.cheated) return;
        Long pb = best.get(key(mode, "RUN"));
        if (pb == null || total < pb) {
            best.put(key(mode, "RUN"), total);
            EndPractice.chat(mc, pb == null ? "§6First finished run§r for this mode" : "§6New PB!§r was " + EndPractice.time(pb));
        } else {
            EndPractice.chat(mc, "PB " + EndPractice.time(pb) + " (+" + EndPractice.time(total - pb) + ")");
        }
        appendSplit("RUN", total);
    }

    private static void end(Minecraft mc, String why) {
        current = segment.length;
        endTime = mc.world.getTime();
        result = why + " in " + EndPractice.time(endTime - startTime);
        EndPractice.chat(mc, result + ". " + Config.keyName(restartKey()) + " for a new attempt.");
    }

    /** "0:12.00 before night" or "0:05.00 after night started". */
    private static String nightText(long d) {
        return d < 0 ? EndPractice.time(-d) + " before night" : "+" + EndPractice.time(d) + " after night started";
    }

    static int restartKey() {
        switch (mode) {
            case TOWER: return EndPractice.cfg.keyTower;
            case STRONGHOLD: return EndPractice.cfg.keyStronghold;
            default: return EndPractice.cfg.keyEndgame;
        }
    }

    static List<String> hud(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (!active || mc.world == null) return lines;
        long now = mc.world.getTime();
        Long runPb = best.get(key(mode, "RUN"));
        lines.add("§dEndgame§r" + (mode == Mode.TOWER ? " (tower)" : mode == Mode.STRONGHOLD ? " (stronghold)" : "") + "  " + EndPractice.time(
                (current >= segment.length && result != null ? endTime : now) - startTime)
                + (runPb != null && mode != Mode.TOWER ? "  §7PB " + EndPractice.time(runPb) + "§r" : ""));
        // only the split you're on, and the one just finished for a few seconds (its time and PB are in chat too)
        Split[] splits = Split.values();
        int shown = towerOnly ? 1 : splits.length;
        for (int i = mode == Mode.STRONGHOLD ? 1 : 0; i < shown; i++) {
            boolean justDone = segment[i] >= 0 && now - splitDoneAt[i] < SPLIT_SHOWN_TICKS;
            if (i != current && !justDone) continue;
            Long pb = best.get(key(mode, splits[i].name()));
            String time = segment[i] >= 0 ? "§e" + EndPractice.time(segment[i]) + "§r" : "§7" + EndPractice.time(now - splitStart) + "§r";
            lines.add((i == current ? "> " : "  ") + splits[i].label + " " + time + (pb != null ? "  §7PB " + EndPractice.time(pb) + "§r" : ""));
        }
        if (mode != Mode.STRONGHOLD && EndPractice.cfg.nightTime >= 0) {
            if (nightAtDeath != Long.MIN_VALUE) lines.add("Reset " + nightText(nightAtDeath));
            else if (current == 0) {
                long d = now - EndPractice.cfg.nightTime;
                lines.add(d < 0 ? "Night in " + EndPractice.time(-d) : "§cNight +" + EndPractice.time(d) + "§r");
            }
        }
        if (EndPractice.cfg.respawnTimer && mode != Mode.STRONGHOLD && !towerOnly && mc.world.dimension.id == 0
                && current == Split.PORTAL_ROOM.ordinal() && !respawned && mc.player.getHealth() <= 0) lines.add(respawnLine(mc));
        if (mc.world.dimension.id == 0) lines.addAll(PitPlanner.hud(mc, showPitPlan()));
        // endermen: only through the night (from night or the first enderman until the night ends or the first eye)
        boolean night = endermen > 0 || !seenEndermen.isEmpty() || (EndPractice.cfg.nightTime >= 0 && now % 24000 >= EndPractice.cfg.nightTime);
        if (night && !nightDone && mc.world.dimension.id == 0) {
            lines.add("Endermen " + endermen + "   Pearls " + pearls + (endermen > 0 ? " (" + rate(dropped, endermen) + ")" : ""));
            if (EndPractice.cfg.trackerOn() && mode == Mode.FULL) {
                long[] t = endermanTotals();
                lines.add("Spawned " + seenEndermen.size() + " (" + surfaceEndermen + " surface)"
                        + (t[0] > 0 ? "   §7avg " + avg(t[1], t[0]) + (t[6] > 0 ? " (" + avg(t[5], t[6]) + ")" : "") + "§r" : ""));
                if (EndPractice.cfg.cycleTracker) lines.add("Cycles " + cycles + (t[8] > 0 ? "   §7avg " + avg(t[7], t[8]) + "§r" : ""));
            }
        }
        if (result != null) lines.add(result);
        if (mc.world.dimension.id == 1 && !towerOnly) {
            EnderDragonEntity d = EndPractice.dragon(mc);
            if (d != null) lines.add("Dragon " + Math.max(0, d.getHealth()) + " / 200   Crystals " + EndPractice.crystals(mc));
            String last = BedReport.lastShort();
            if (last != null) lines.add(last);
            String cue = DoubleBed.hud(mc);
            if (cue != null) lines.add(cue);
        }
        return lines;
    }

    // ---- split PBs, config/endpractice-splits.txt: time,split,ticks ------------------------------------------------

    private static File splitsFile() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-splits.txt");
    }

    /** time,SPLIT,ticks,2,MODE */
    private static void appendSplit(String what, long ticks) {
        try {
            File f = splitsFile();
            f.getParentFile().mkdirs();
            String line = System.currentTimeMillis() + "," + what + "," + ticks + ",2," + mode.name() + "\n";
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            EndPractice.log("couldn't save the split: " + e);
        }
    }

    private static void loadBest() {
        if (bestLoaded) return;
        bestLoaded = true;
        try {
            File f = splitsFile();
            if (!f.isFile()) return;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String[] p = line.split(",");
                if (p.length < 3) continue;
                try {
                    String what = p[1].trim();
                    if (!what.equals("RUN")) Split.valueOf(what);
                    // tower times from before 0.2.2 could be accidental deaths, so they don't count for the PB
                    if (what.equals("TOWER") && p.length < 4) continue;
                    long t = Long.parseLong(p[2].trim());
                    List<Mode> modes = new ArrayList<>();
                    if (p.length >= 5) {
                        modes.add(Mode.valueOf(p[4].trim()));
                    } else if (what.equals("TOWER")) {
                        // from before PBs were kept per mode: the tower build is the same in both modes that have it
                        modes.add(Mode.FULL);
                        modes.add(Mode.TOWER);
                    } else {
                        // the rest can't be told apart, so they stay with stronghold attempts, where the fast ones came from
                        modes.add(Mode.STRONGHOLD);
                    }
                    for (Mode m : modes) {
                        Long old = best.get(key(m, what));
                        if (old == null || t < old) best.put(key(m, what), t);
                    }
                } catch (RuntimeException ignored) {
                }
            }
        } catch (Exception e) {
            EndPractice.log("couldn't read the splits: " + e);
        }
    }
}
