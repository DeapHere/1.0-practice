package endpractice;

import endpractice.mixin.PathAccessor;
import endpractice.mixin.PathMobAccessor;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathFinderMobEntity;
import net.minecraft.entity.mob.monster.EndermanEntity;
import net.minecraft.entity.mob.monster.Monster;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/**
 * Pit sandbox: a night world in creative to build and try out pit designs. It can draw where mobs are walking,
 * where endermen teleport, where monsters just spawned, which blocks monsters can spawn on, and the 24 / 32 / 128
 * block spawn and despawn rings. Named tests count endermen per minute, how many reach you and how fast, kills and
 * pearls, and are saved so designs can be compared.
 */
public final class PitSandbox {
    private static boolean active;
    /** Profiling: nanoseconds spent in 0 tick, 1 planner, 2 spawn overlay, 3 spawnable scan, 4 render spawnable, 5 rings, 6 paths, 7 teleports+spawns; 8 frames, 9 ticks. */
    public static final long[] prof = new long[10];
    private static int attempts;
    /** The time of day the world is held at, or -1 when time runs. */
    public static long lockedTime = -1;

    private PitSandbox() {
    }

    public static boolean isActive() {
        return active;
    }

    /** The sandbox, or a normal world (not one of the practice modes): /test, /pit and the enderman key work here. */
    public static boolean tracking(Minecraft mc) {
        if (endpractice.compat.RankedCompat.busy()) return false;
        return active || mc != null && mc.world != null && mc.player != null && !EndPractice.inPracticeWorld();
    }

    /** Whether the overlays draw: always in the sandbox, in normal worlds when turned on (/sandbox on). */
    static boolean overlays(Minecraft mc) {
        if (endpractice.compat.RankedCompat.busy()) return false;
        return active || EndPractice.cfg.sbInWorlds && tracking(mc);
    }

    private static World trackedWorld;
    /** /tests: open the stats page next tick (chat is still closing this one). */
    static boolean openTests;

    static void stop() {
        if (active && test != null) endTest(EndPractice.mc, "left the sandbox");
        active = false;
        flatGen = false;
        lockedTime = -1;
        teleports.clear();
        spawns.clear();
        seen.clear();
        summoned.clear();
        spawnable = null;
    }

    /** The sandbox world is being made or played as a flat desert: the terrain generator makes flat sand. */
    public static boolean flatGen;
    /** The flat desert's top block: sand at y 63, so you stand at 64 like the sea level ground of a real desert. */
    public static final int FLAT_TOP = 63;

    /** A flat desert chunk: bedrock, stone, then sandstone and sand on top. */
    public static void flatChunk(byte[] blocks, int height) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int base = (x * 16 + z) * height;
            blocks[base] = 7;
            for (int y = 1; y <= FLAT_TOP; y++) blocks[base + y] = (byte) (y > FLAT_TOP - 4 ? 12 : y > FLAT_TOP - 8 ? 24 : 1);
        }
    }

    static void start(Minecraft mc) {
        attempts++;
        test = null;
        flatGen = EndPractice.cfg.sbFlat;
        Endgame.makeSandboxWorld(mc);
        PlayerEntity p = mc.player;
        Commands.gamemode(mc, "c", false);
        EndPractice.giveKit(p, EndPractice.cfg.towerKit);
        p.setHealth(20);
        p.getHungerManager().add(20, 20.0F);
        lockedTime = 18000; // midnight, held (the sky and spawning see it, the world clock keeps going)
        seen.clear();
        summoned.clear();
        spawns.clear();
        teleports.clear();
        spawnable = null;
        active = true;
        for (Object o : mc.world.entities) seen.add(o);
        // the pit planner loads a lot of terrain while it works, so here it only runs when asked (J or /pit plan)
        PitPlanner.clear();
        if (EndPractice.cfg.sbNoAnimals) removeAnimals(mc);
        EndPractice.chat(mc, "§dPit sandbox§r " + attempts + ": creative, held at midnight. Build a pit and try it out.");
        EndPractice.chat(mc, "§7/summon <mob> [count], /killall, /time unlock, /pit save|load <name> for designs, /test <name> to time one, /tests to compare, /sandbox for the overlays, J plans the pit from here, X saved pits, M an enderman.§r");
    }

    static void setTimeOfDay(Minecraft mc, long t) {
        // the nearest time with that time of day, back or forward, so holding the time never skips days
        long now = mc.world.getTime();
        long diff = Math.floorMod(t - now + 12000, 24000L) - 12000;
        mc.world.setTime(now + diff);
    }

    /** Ticks since the sandbox started: the clock for tests and markers, since the world time can be held. */
    private static long clock;

    // ---- what's happening -------------------------------------------------------------------------------------------

    /** Entities already looked at (so new ones are spawns). */
    private static final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Mobs from /summon: not natural spawns. */
    static final Set<Object> summoned = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Spawn markers: x, y, z, world time, colour. */
    private static final List<double[]> spawns = new ArrayList<>();
    /** Enderman teleports: from x y z, to x y z, world time. */
    private static final List<double[]> teleports = new ArrayList<>();
    private static final int SPAWN_SHOWN = 600, TELEPORT_SHOWN = 200;

    public static void onTeleport(EndermanEntity e, double fx, double fy, double fz) {
        if (!tracking(EndPractice.mc) || e.world == null) return;
        teleports.add(new double[]{fx, fy, fz, e.x, e.y, e.z, clock});
        if (test != null && !summoned.contains(e)) test.teleports++;
    }

    public static void onEndermanKilled(EndermanEntity e, int pearls) {
        if (!tracking(EndPractice.mc) || test == null) return;
        test.killed++;
        test.pearls += pearls;
        // one you killed got to you: it came for you and reached you
        PlayerEntity p = EndPractice.mc.player;
        if (summoned.contains(e) || p == null || test.reached.contains(e)) return;
        double dx = e.x - p.x, dz = e.z - p.z;
        if (dx * dx + dz * dz > 8 * 8) return;
        test.cameFor.add(e);
        test.firstSeen.putIfAbsent(e, clock);
        test.reached.add(e);
        test.reachTicks += clock - test.firstSeen.get(e);
    }

    static void tick(Minecraft mc) {
        if (openTests && mc.screen == null) {
            openTests = false;
            mc.openScreen(new TestsScreen(null));
        }
        if (!tracking(mc)) return;
        // paused (Esc menu): the world stands still, so the test clock does too
        if (mc.paused) return;
        if (mc.world != trackedWorld) {
            // a different world (or dimension): start fresh, and what's already here isn't a new spawn
            trackedWorld = mc.world;
            if (!active) test = null;
            seen.clear();
            summoned.clear();
            spawns.clear();
            teleports.clear();
            spawnable = null;
            for (Object o : mc.world.entities) seen.add(o);
        }
        long t0 = System.nanoTime();
        PitPlanner.tick(mc);
        long t1 = System.nanoTime();
        prof[1] += t1 - t0;
        prof[9]++;
        long now = ++clock;
        PlayerEntity p = mc.player;
        for (Object o : mc.world.entities) {
            if (o == p || !(o instanceof Entity)) continue;
            Entity e = (Entity) o;
            if (seen.add(o)) {
                if (o instanceof Monster && !summoned.contains(o)) {
                    spawns.add(new double[]{e.x, e.y, e.z, now, color(e)});
                    if (test != null) {
                        test.monsters++;
                        if (o instanceof EndermanEntity) {
                            // only surface endermen count: cave ones never come for you
                            if (onSurface(mc, e, EndPractice.cfg.endermanRange)) {
                                test.endermen++;
                                test.firstSeen.put(o, now);
                            }
                        }
                    }
                }
            }
            if (test != null && o instanceof EndermanEntity && !summoned.contains(o) && !test.reached.contains(o)) {
                double dx = e.x - p.x, dy = e.y - (p.y - p.eyeHeight), dz = e.z - p.z;
                // any enderman walking at you counts as coming for you, wherever and whenever it spawned
                if (((PathMobAccessor) o).endpractice$getWalkTarget() == p && test.cameFor.add(o)) test.firstSeen.putIfAbsent(o, now);
                if (test.cameFor.contains(o)) test.closest.merge(o, Math.sqrt(dx * dx + dy * dy + dz * dz), Math::min);
                if (dx * dx + dz * dz <= 16 && Math.abs(dy) <= 3.5) {
                    test.cameFor.add(o);
                    test.firstSeen.putIfAbsent(o, now);
                    test.reached.add(o);
                    test.reachTicks += now - test.firstSeen.get(o);
                }
            }
        }
        seen.removeIf(o -> ((Entity) o).removed);
        summoned.removeIf(o -> ((Entity) o).removed);
        spawns.removeIf(s -> now - s[3] > SPAWN_SHOWN || now < s[3]);
        teleports.removeIf(t -> now - t[6] > TELEPORT_SHOWN || now < t[6]);
        if (EndPractice.cfg.sbSpawnable && overlays(mc) && now % 20 == 0) {
            long a = System.nanoTime();
            spawnable = findSpawnable(mc);
            spawnableDirty = true;
            prof[3] += System.nanoTime() - a;
        }
        if (active && EndPractice.cfg.sbNoAnimals && now % 40 == 0) removeAnimals(mc);
        if (test != null && test.length > 0 && now - test.start >= test.length) {
            // the night's over, but endermen still after you get finished off: overtime until none has been after you
            // for 5 seconds (2 minutes at most)
            int aggro = 0;
            for (Object o : mc.world.entities) {
                if (o instanceof EndermanEntity && !summoned.contains(o) && !((Entity) o).removed && ((EndermanEntity) o).getHealth() > 0
                        && ((PathMobAccessor) o).endpractice$getWalkTarget() == p) aggro++;
            }
            test.aggro = aggro;
            if (aggro > 0) test.lastAggro = now;
            long over = now - test.start - test.length;
            if (aggro == 0 && (test.lastAggro < 0 || now - test.lastAggro >= 100) || over >= 2400) {
                endTest(mc, test.length >= NIGHT_END - NIGHT_START ? "night over" : "time's up");
            }
        }
        prof[0] += System.nanoTime() - t1;
    }

    // ---- tests ------------------------------------------------------------------------------------------------------

    private static final class Test {
        final String name;
        final long start, length;
        /** The time hold before the test, put back after it. */
        long heldBefore = -1;
        /** Overtime: endermen still after you once the night's over, and when one last was. */
        int aggro;
        long lastAggro = -1;
        int endermen, monsters, killed, pearls, teleports;
        long reachTicks;
        final Map<Object, Long> firstSeen = new IdentityHashMap<>();
        /** Endermen that came for you (walking at you), and how close each one got. */
        final Set<Object> cameFor = Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<Object, Double> closest = new IdentityHashMap<>();
        final Set<Object> reached = Collections.newSetFromMap(new IdentityHashMap<>());

        Test(String name, long start, long length) {
            this.name = name;
            this.start = start;
            this.length = length;
        }
    }

    private static Test test;

    /**
     * A test is one real night: from 11:20 into the day (13600 ticks, when the sky is dark enough that monsters spawn
     * on open ground most of the time) to the last tick they can spawn there at all (22651: after that the surface is
     * light 8 or more till the next night). The time runs for it, so it's the same night you'd get in a run.
     */
    static final long NIGHT_START = 13600, NIGHT_END = 22652;

    /** The time of day as minutes:seconds into the day, the way a run's timer reads it (13600 is 11:20). */
    static String dayClock(long tod) {
        long s = Math.floorMod(tod, 24000L) / 20;
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
    }

    static void startTest(Minecraft mc, String name, long minutes) {
        if (!tracking(mc)) {
            EndPractice.chat(mc, "§c/test works in the pit sandbox or a normal world, not in the practice modes.");
            return;
        }
        if (test != null) endTest(mc, "new test");
        deathReset(mc);
        // 11:20 in a run comes after a day: no monsters around yet
        int cleared = killAll(mc, "monsters");
        long night = NIGHT_END - NIGHT_START;
        test = new Test(name, clock, minutes > 0 ? Math.min(night, minutes * 1200) : night);
        // the night runs for real: 11:20 onwards, the time not held
        test.heldBefore = lockedTime;
        lockedTime = -1;
        long now = mc.world.getTime();
        mc.world.setTime(now - Math.floorMod(now, 24000L) + NIGHT_START + (Math.floorMod(now, 24000L) > NIGHT_START ? 24000 : 0));
        // only what spawns from now on counts
        for (Object o : mc.world.entities) seen.add(o);
        EndPractice.chat(mc, "§dTest \"" + name + "\"§r: one full night, " + dayClock(NIGHT_START) + " to " + dayClock(NIGHT_END)
                + (minutes > 0 && minutes * 1200 < night ? " (just the first " + minutes + " min)" : "") + ", the last tick monsters can spawn. Cleared " + cleared + " monsters for a fresh night. You've been reset to spawn like a death in the pit, in survival: head to your pit."
                + " Only surface endermen count; \"reached you\" is ones that get within 4 blocks of you. /test stop ends it early. Summoned mobs don't count.");
    }

    /**
     * Like dying in the pit in a run: you respawn at spawn (on top of the tower, if you built one there) the game's own
     * way, so everything far from spawn despawns the same as it would. Your inventory and game mode come with you.
     */
    private static void deathReset(Minecraft mc) {
        PlayerEntity old = mc.player;
        net.minecraft.item.ItemStack[] main = old.inventory.items.clone(), armor = old.inventory.armor.clone();
        int slot = old.inventory.selectedSlot;
        old.respawn();
        PlayerEntity p = mc.player;
        if (p == null || p == old) return;
        System.arraycopy(main, 0, p.inventory.items, 0, Math.min(main.length, p.inventory.items.length));
        System.arraycopy(armor, 0, p.inventory.armor, 0, Math.min(armor.length, p.inventory.armor.length));
        p.inventory.selectedSlot = slot;
        // a test is played for real: survival straight away
        Commands.gamemode(mc, "s", false);
    }

    static void endTest(Minecraft mc, String why) {
        Test t = test;
        test = null;
        if (t != null) lockedTime = t.heldBefore;
        if (t == null || mc == null || mc.world == null) {
            if (mc != null && t == null) EndPractice.chat(mc, "No test running. /test <name> [minutes] starts one.");
            return;
        }
        long ticks = clock - t.start;
        if (ticks < 200) {
            EndPractice.chat(mc, "Test \"" + t.name + "\" stopped (" + why + "), too short to save.");
            return;
        }
        EndPractice.chat(mc, "§dTest \"" + t.name + "\"§r (" + why + "): " + summary(ticks, t.endermen, t.reached.size(), t.reachTicks, t.killed, t.pearls, t.teleports, t.monsters));
        try {
            File f = file();
            f.getParentFile().mkdirs();
            String line = String.format(Locale.ROOT, "%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d%n", System.currentTimeMillis(), t.name.replace(",", " "), ticks,
                    t.endermen, t.reached.size(), t.reachTicks, t.killed, t.pearls, t.teleports, t.monsters, t.cameFor.size());
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            EndPractice.log("couldn't save the pit test: " + e);
        }
        EndPractice.chat(mc, "§7Saved. /tests opens the stats for every design.§r");
    }

    /** How the pit funnels them: of the surface endermen that came for you, how many made it, and how close the rest got. */
    private static String cameFor(Test t) {
        int came = t.cameFor.size(), made = 0;
        double stuckSum = 0;
        int stuck = 0;
        for (Object o : t.cameFor) {
            if (t.reached.contains(o)) made++;
            else if (t.closest.containsKey(o)) {
                stuckSum += t.closest.get(o);
                stuck++;
            }
        }
        if (came == 0) return "§7No surface enderman came for you.§r";
        return came + " came for you: " + made + " reached you (" + Math.round(made * 100.0 / came) + "%)"
                + (stuck > 0 ? ", " + stuck + " got stuck or gave up, " + String.format(Locale.ROOT, "%.1f", stuckSum / stuck) + " blocks away at closest (avg)" : "");
    }

    private static String perMin(long n, long ticks) {
        return String.format(Locale.ROOT, "%.1f", n * 1200.0 / Math.max(1, ticks));
    }

    private static String summary(long ticks, long endermen, long reached, long reachTicks, long killed, long pearls, long tps, long monsters) {
        return EndPractice.time(ticks) + ", killed " + killed + " (" + perMin(killed, ticks) + "/min), " + pearls + " pearls (" + perMin(pearls, ticks) + "/min)"
                + "; " + endermen + " surface endermen spawned (" + perMin(endermen, ticks) + "/min)";
    }


    private static File file() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-pits.txt");
    }

    /** Every design tested, totals over its tests: name -> ticks, endermen, reached, reachTicks, killed, pearls, teleports, monsters, tests. */
    static Map<String, long[]> designs() {
        Map<String, long[]> out = new LinkedHashMap<>();
        try {
            File f = file();
            if (!f.exists()) return out;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String[] s = line.split(",");
                if (s.length < 10) continue;
                long[] t = out.computeIfAbsent(s[1], k -> new long[9]);
                for (int i = 0; i < 8; i++) t[i] += Long.parseLong(s[i + 2].trim());
                t[8]++;
            }
        } catch (Exception e) {
            EndPractice.log("couldn't read the pit tests: " + e);
        }
        return out;
    }

    /** Every saved test, oldest first: time, name, ticks, endermen, reached, reachTicks, killed, pearls, teleports, monsters[, came]. */
    static List<String[]> testRows() {
        List<String[]> out = new ArrayList<>();
        try {
            File f = file();
            if (!f.exists()) return out;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String[] s = line.split(",");
                if (s.length >= 10) out.add(s);
            }
        } catch (Exception e) {
            EndPractice.log("couldn't read the pit tests: " + e);
        }
        return out;
    }

    /** Deletes the tests saved at these times (the first column), or every test of a design when name isn't null. */
    static int deleteTests(Set<String> times, String name) {
        int n = 0;
        try {
            File f = file();
            if (!f.exists()) return 0;
            List<String> keep = new ArrayList<>();
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String[] s = line.split(",");
                if (s.length >= 2 && (times != null && times.contains(s[0]) || name != null && name.equals(s[1]))) n++;
                else keep.add(line);
            }
            Files.write(f.toPath(), keep, StandardCharsets.UTF_8);
        } catch (Exception e) {
            EndPractice.log("couldn't delete pit tests: " + e);
        }
        return n;
    }

    /**
     * Copies the sandbox world as it is now into your saves, as "Pit <name>", so you can open it again from Singleplayer
     * (the sandbox itself is overwritten by the next one). /test, /pit and the M key work there too.
     */
    static void saveWorld(Minecraft mc, String name) {
        if (!active) {
            EndPractice.chat(mc, "§cOnly the pit sandbox world can be saved like this (your own worlds are saved already).");
            return;
        }
        String folder = "Pit_" + name.trim().replaceAll("[^A-Za-z0-9 _.-]", "").replace(' ', '_');
        File saves = new File(Minecraft.getWorkingDirectory(), "saves");
        File from = new File(saves, Endgame.FOLDER), to = new File(saves, folder);
        if (to.exists()) {
            EndPractice.chat(mc, "§cThere's already a world called " + folder + ".§r Pick another name.");
            return;
        }
        try {
            mc.world.forceSave(null);
            java.nio.file.Path src = from.toPath();
            try (java.util.stream.Stream<java.nio.file.Path> walk = Files.walk(src)) {
                for (java.nio.file.Path p : (Iterable<java.nio.file.Path>) walk::iterator) {
                    if (p.getFileName().toString().equals("session.lock")) continue;
                    java.nio.file.Path dst = to.toPath().resolve(src.relativize(p).toString());
                    if (Files.isDirectory(p)) Files.createDirectories(dst);
                    else Files.copy(p, dst);
                }
            }
            mc.getWorldStorageSource().rename(folder, "Pit " + name.trim());
            EndPractice.chat(mc, "Saved this world as §ePit " + name.trim() + "§r. Open it from Singleplayer any time; /test, /pit and M work there, /sandbox on for the overlays.");
        } catch (Exception e) {
            EndPractice.chat(mc, "§cCouldn't save the world: " + e.getMessage());
        }
    }

    static void listTests(Minecraft mc) {
        Map<String, long[]> d = designs();
        if (d.isEmpty()) {
            EndPractice.chat(mc, "No designs tested yet. In the pit sandbox, /test <name> [minutes].");
            return;
        }
        String bestName = null;
        double best = -1;
        for (Map.Entry<String, long[]> e : d.entrySet()) {
            double r = e.getValue()[2] * 1200.0 / Math.max(1, e.getValue()[0]);
            if (r > best) {
                best = r;
                bestName = e.getKey();
            }
        }
        EndPractice.chat(mc, "§dPit designs§r (totals over each design's tests, best for endermen reaching you in green):");
        for (Map.Entry<String, long[]> e : d.entrySet()) {
            long[] t = e.getValue();
            EndPractice.chat(mc, (e.getKey().equals(bestName) ? "§a" : "§e") + e.getKey() + "§r x" + t[8] + ": " + summary(t[0], t[1], t[2], t[3], t[4], t[5], t[6], t[7]));
        }
    }

    // ---- summon and kill --------------------------------------------------------------------------------------------

    private static final String[][] MOBS = {
            {"Enderman", "enderman", "endermen", "ender"}, {"Zombie", "zombie"}, {"Skeleton", "skeleton", "skelly"}, {"Creeper", "creeper"},
            {"Spider", "spider"}, {"CaveSpider", "cavespider", "cave_spider"}, {"Slime", "slime"}, {"PigZombie", "pigzombie", "pigman", "zombiepigman"},
            {"Blaze", "blaze"}, {"Ghast", "ghast"}, {"LavaSlime", "lavaslime", "magmacube", "magma"}, {"Silverfish", "silverfish"},
            {"Giant", "giant"}, {"Pig", "pig"}, {"Cow", "cow"}, {"Sheep", "sheep"}, {"Chicken", "chicken"}, {"Squid", "squid"}, {"Wolf", "wolf"},
            {"MushroomCow", "mushroomcow", "mooshroom"}, {"SnowMan", "snowman", "snowgolem"}, {"Villager", "villager"},
            {"EnderDragon", "enderdragon", "dragon"}, {"EnderCrystal", "endercrystal", "crystal"}, {"PrimedTnt", "tnt", "primedtnt"}};

    static String mobKey(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        for (String[] m : MOBS) for (String a : m) if (a.equalsIgnoreCase(n)) return m[0];
        return null;
    }

    static String mobList() {
        StringBuilder sb = new StringBuilder();
        for (String[] m : MOBS) sb.append(sb.length() == 0 ? "" : ", ").append(m[1]);
        return sb.toString();
    }

    /** Where /summon puts things: the block you're looking at (up to 64 away), or where you stand. */
    static double[] summonSpot(Minecraft mc) {
        PlayerEntity p = mc.player;
        net.minecraft.world.HitResult h = p.rayTrace(64.0, 1.0F);
        if (h != null && h.type == net.minecraft.world.HitResult.Type.BLOCK) {
            int x = h.x, y = h.y, z = h.z;
            switch (h.face) {
                case 0: y--; break;
                case 1: y++; break;
                case 2: z--; break;
                case 3: z++; break;
                case 4: x--; break;
                case 5: x++; break;
            }
            return new double[]{x + 0.5, y, z + 0.5};
        }
        return new double[]{p.x, p.y - p.eyeHeight, p.z};
    }

    public static int summon(Minecraft mc, String key, int count, double x, double y, double z) {
        int n = 0;
        for (int i = 0; i < count; i++) {
            Entity e = Entities.createSilently(key, mc.world);
            if (e == null) break;
            double ox = count > 1 ? (mc.world.random.nextDouble() - 0.5) * 2 : 0, oz = count > 1 ? (mc.world.random.nextDouble() - 0.5) * 2 : 0;
            e.setPositionAndAngles(x + ox, y, z + oz, mc.world.random.nextFloat() * 360.0F, 0.0F);
            summoned.add(e);
            if (mc.world.addEntity(e)) n++;
        }
        return n;
    }

    /** Removes mobs: all of them, or monsters, or one kind. Returns how many. */
    static int killAll(Minecraft mc, String what) {
        String key = what == null || what.equals("all") || what.equals("monsters") ? null : mobKey(what);
        if (what != null && !what.equals("all") && !what.equals("monsters") && key == null) return -1;
        int n = 0;
        for (Object o : new ArrayList<Object>(mc.world.entities)) {
            if (!(o instanceof MobEntity) || o instanceof PlayerEntity || ((Entity) o).removed) continue;
            if ("monsters".equals(what) && !(o instanceof Monster)) continue;
            if (key != null && !key.equals(Entities.getKey((Entity) o))) continue;
            ((Entity) o).remove();
            n++;
        }
        return n;
    }

    /** Animals (and squid) only cost ticks here: the pit is about monsters, and animals don't count to their cap. */
    private static void removeAnimals(Minecraft mc) {
        for (Object o : mc.world.entities) {
            if ((o instanceof net.minecraft.entity.mob.animal.AnimalEntity || o instanceof net.minecraft.entity.mob.water.WaterMobEntity) && !summoned.contains(o)) {
                ((Entity) o).remove();
            }
        }
    }

    // ---- where monsters can spawn ----------------------------------------------------------------------------------

    /** Blocks near you that monsters could spawn on right now: x, y, z, and 1 if an enderman fits (3 high). */
    private static List<int[]> spawnable;
    private static boolean spawnableDirty;
    private static int spawnableList;

    private static List<int[]> findSpawnable(Minecraft mc) {
        List<int[]> out = new ArrayList<>();
        World w = mc.world;
        int px = (int) Math.floor(mc.player.x), py = (int) Math.floor(mc.player.y), pz = (int) Math.floor(mc.player.z);
        int r = 24;
        for (int x = px - r; x <= px + r; x++) {
            for (int z = pz - r; z <= pz + r; z++) {
                for (int y = Math.max(1, py - 24); y <= Math.min(125, py + 12); y++) {
                    if (!w.isSolidBlock(x, y - 1, z) || w.isSolidBlock(x, y, z) || w.getMaterial(x, y, z).isLiquid() || w.isSolidBlock(x, y + 1, z)) continue;
                    if (w.getBlock(x, y, z) != 0 && w.getBlock(x, y, z) != 78 && w.getBlock(x, y, z) != 31) continue;
                    // MonsterEntity.canSpawnAtLightLevel: light 7 or less can spawn (lower is likelier)
                    if (w.getRawBrightness(x, y, z) > 7) continue;
                    boolean ender = !w.isSolidBlock(x, y + 2, z);
                    out.add(new int[]{x, y, z, ender ? 1 : 0});
                    if (out.size() > 6000) return out;
                }
            }
        }
        return out;
    }

    // ---- HUD and drawing --------------------------------------------------------------------------------------------

    /** The sandbox HUD's pieces: key, name in the editor, what it's for. The order and which are on is cfg.sbHud. */
    static final String[][] HUD_PARTS = {
            {"title", "Time of day", "\"Pit sandbox\" and the time as minutes:seconds into the day (11:20 is 13600)"},
            {"test", "Test timer", "During a test: its name, how long it's run and when the night ends"},
            {"kills", "Kills", "During a test: endermen you've killed, and per minute"},
            {"pearls", "Pearls", "During a test: pearls you've got, and per minute"},
            {"surface", "Surface endermen", "Endermen on the surface within the surface range (87) right now: the ones that can come for you"},
            {"spawned", "Surface spawns", "During a test: surface endermen that have spawned within 87, and per minute"},
            {"loaded", "Loaded mobs", "Every enderman and monster loaded, caves too"},
            {"light", "Light here", "The light level where you stand (monsters spawn at 7 or less)"},
            {"planner", "Pit planner", "The pit planner's lines, after J or /pit plan"}};
    static final String HUD_DEFAULT = "title,test,surface,kills,pearls,-spawned,-loaded,-light,planner";

    /** cfg.sbHud as keys in order, each with whether it's on. Unknown keys are dropped, missing ones added (off) at the end. */
    static List<String[]> hudLayout() {
        List<String[]> out = new ArrayList<>();
        java.util.Set<String> have = new java.util.HashSet<>();
        String cfg = EndPractice.cfg.sbHud == null || EndPractice.cfg.sbHud.isEmpty() ? HUD_DEFAULT : EndPractice.cfg.sbHud;
        for (String k : cfg.split(",")) {
            k = k.trim();
            boolean on = !k.startsWith("-");
            String key = on ? k : k.substring(1);
            for (String[] part : HUD_PARTS) {
                if (part[0].equals(key) && have.add(key)) out.add(new String[]{key, on ? "1" : "0"});
            }
        }
        for (String[] part : HUD_PARTS) if (have.add(part[0])) out.add(new String[]{part[0], "0"});
        return out;
    }

    static String hudString(List<String[]> layout) {
        StringBuilder sb = new StringBuilder();
        for (String[] l : layout) sb.append(sb.length() == 0 ? "" : ",").append(l[1].equals("1") ? "" : "-").append(l[0]);
        return sb.toString();
    }

    static List<String> hud(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (!tracking(mc)) return lines;
        // in your own worlds there's only a HUD during a test
        if (!active && test == null) return lines;
        long tod = lockedTime >= 0 ? lockedTime : Math.floorMod(mc.world.getTime(), 24000L);
        long ticks = test != null ? clock - test.start : 0;
        int range = EndPractice.cfg.endermanRange;
        int monsters = -1, endermen = 0, surface = 0;
        for (String[] l : hudLayout()) {
            if (!l[1].equals("1")) continue;
            switch (l[0]) {
                case "title":
                    if (active) lines.add("§dPit sandbox§r  " + dayClock(tod) + " §7(" + tod + (lockedTime >= 0 ? ", held" : "") + ")§r");
                    else lines.add("Time " + dayClock(tod));
                    break;
                case "test":
                    if (test != null) lines.add("§eTest \"" + test.name + "\"§r " + EndPractice.time(ticks) + (ticks >= test.length
                            ? " §6overtime: " + test.aggro + " still after you§r" : " §7(ends " + dayClock(NIGHT_START + test.length) + ")§r"));
                    break;
                case "kills":
                    if (test != null) lines.add("Killed §a" + test.killed + "§r §7(" + perMin(test.killed, ticks) + "/min)§r");
                    break;
                case "pearls":
                    if (test != null) lines.add("Pearls §d" + test.pearls + "§r §7(" + perMin(test.pearls, ticks) + "/min)§r");
                    break;
                case "spawned":
                    if (test != null) lines.add("Surface spawns " + test.endermen + " §7(" + perMin(test.endermen, ticks) + "/min)§r");
                    break;
                case "surface": case "loaded":
                    if (monsters < 0) {
                        monsters = 0;
                        for (Object o : mc.world.entities) {
                            if (o instanceof Entity && ((Entity) o).removed) continue;
                            if (o instanceof Monster) monsters++;
                            if (o instanceof EndermanEntity) {
                                endermen++;
                                if (onSurface(mc, (Entity) o, range)) surface++;
                            }
                        }
                    }
                    if (l[0].equals("surface")) lines.add("Surface endermen §d" + surface + "§r");
                    else lines.add("§7Loaded: " + endermen + " endermen, " + monsters + " monsters§r");
                    break;
                case "light": {
                    int x = (int) Math.floor(mc.player.x), y = (int) Math.floor(mc.player.y - mc.player.eyeHeight), z = (int) Math.floor(mc.player.z);
                    lines.add("§7Light here " + mc.world.getRawBrightness(x, y, z) + " (block " + mc.world.getLight(LightType.BLOCK, x, y, z) + ")§r");
                    break;
                }
                case "planner":
                    if (active && mc.world.dimension.id == 0) lines.addAll(PitPlanner.hud(mc, EndPractice.cfg.pitPlanner));
                    break;
            }
        }
        return lines;
    }

    /** Open sky above it (not in a cave), and within range blocks of you across (0: anywhere loaded). */
    public static boolean onSurface(Minecraft mc, Entity e, int range) {
        if (range > 0) {
            double dx = e.x - mc.player.x, dz = e.z - mc.player.z;
            if (dx * dx + dz * dz > (double) range * range) return false;
        }
        // on the ground layer: nothing solid above it but leaves (under a tree still counts, in a cave doesn't)
        World w = mc.world;
        int x = (int) Math.floor(e.x), z = (int) Math.floor(e.z);
        int y = w.getHeight(x, z);
        while (y > 1 && (w.getBlock(x, y - 1, z) == 18 || !w.getMaterial(x, y - 1, z).isSolid())) y--;
        return e.y >= y - 0.5;
    }

    private static double color(Entity e) {
        String k = Entities.getKey(e);
        if (k == null) return 0xFFFFFF;
        switch (k) {
            case "Enderman": return 0xC040FF;
            case "Creeper": return 0x40FF40;
            case "Zombie": return 0x40C0C0;
            case "Skeleton": return 0xE0E0E0;
            case "Spider": case "CaveSpider": return 0xFF4040;
            case "Slime": return 0x80FF80;
            default: return 0xFFB040;
        }
    }

    private static void glColor(double rgb, float a) {
        int c = (int) rgb;
        GL11.glColor4f(((c >> 16) & 255) / 255.0F, ((c >> 8) & 255) / 255.0F, (c & 255) / 255.0F, a);
    }

    public static void render(Minecraft mc, float tickDelta) {
        if (!overlays(mc)) return;
        Config cfg = EndPractice.cfg;
        PlayerEntity p = mc.player;
        double cx = p.prevX + (p.x - p.prevX) * tickDelta;
        double cy = p.prevY + (p.y - p.prevY) * tickDelta;
        double cz = p.prevZ + (p.z - p.prevZ) * tickDelta;
        long now = clock;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        prof[8]++;
        long r0 = System.nanoTime();
        if (cfg.sbSpawnable && spawnable != null) {
            // little crosses on the blocks monsters can spawn on: orange, purple where an enderman fits too. Built
            // into a display list once a second when the scan changes, so each frame is one call
            if (spawnableList == 0) spawnableList = GL11.glGenLists(1);
            if (spawnableDirty) {
                spawnableDirty = false;
                GL11.glNewList(spawnableList, GL11.GL_COMPILE);
                GL11.glBegin(GL11.GL_LINES);
                for (int[] s : spawnable) {
                    if (s[3] == 1) GL11.glColor4f(0.8F, 0.3F, 1.0F, 0.8F);
                    else GL11.glColor4f(1.0F, 0.6F, 0.1F, 0.8F);
                    float x = s[0], y = s[1] + 0.02F, z = s[2];
                    GL11.glVertex3f(x + 0.25F, y, z + 0.25F);
                    GL11.glVertex3f(x + 0.75F, y, z + 0.75F);
                    GL11.glVertex3f(x + 0.75F, y, z + 0.25F);
                    GL11.glVertex3f(x + 0.25F, y, z + 0.75F);
                }
                GL11.glEnd();
                GL11.glEndList();
            }
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glLineWidth(2.0F);
            GL11.glPushMatrix();
            GL11.glTranslated(-cx, -cy, -cz);
            GL11.glCallList(spawnableList);
            GL11.glPopMatrix();
        }
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        long r1 = System.nanoTime();
        prof[4] += r1 - r0;

        if (cfg.sbRings) {
            // no spawning within 24 blocks of you, random despawns past 32: two faint dashed rings at your feet
            double fy = p.prevY + (p.y - p.prevY) * tickDelta - p.eyeHeight + 0.05 - cy;
            ring(24, 0.3F, 1.0F, 0.3F, fy);
            ring(32, 1.0F, 0.9F, 0.2F, fy);
        }

        long r2 = System.nanoTime();
        prof[5] += r2 - r1;
        if (cfg.sbPaths) {
            GL11.glLineWidth(2.5F);
            for (Object o : mc.world.entities) {
                // hostiles only, within 64 blocks
                if (!(o instanceof PathFinderMobEntity) || !(o instanceof Monster)) continue;
                PathFinderMobEntity m = (PathFinderMobEntity) o;
                if (m.distanceTo(p) > 64) continue;
                double mx = m.prevX + (m.x - m.prevX) * tickDelta - cx, my = m.prevY + (m.y - m.prevY) * tickDelta - cy, mz = m.prevZ + (m.z - m.prevZ) * tickDelta - cz;
                Path path = ((PathMobAccessor) m).endpractice$getPath();
                if (path != null) {
                    PathNode[] nodes = ((PathAccessor) path).endpractice$getNodes();
                    int from = ((PathAccessor) path).endpractice$getCurrentIndex();
                    glColor(color(m), 0.85F);
                    GL11.glBegin(GL11.GL_LINE_STRIP);
                    GL11.glVertex3d(mx, my + 0.1, mz);
                    for (int i = Math.max(0, from); i < nodes.length; i++) {
                        GL11.glVertex3d(nodes[i].x + 0.5 - cx, nodes[i].y + 0.1 - cy, nodes[i].z + 0.5 - cz);
                    }
                    GL11.glEnd();
                    if (nodes.length > 0) {
                        PathNode end = nodes[nodes.length - 1];
                        GL11.glBegin(GL11.GL_LINES);
                        GL11.glVertex3d(end.x + 0.5 - cx, end.y - cy, end.z + 0.5 - cz);
                        GL11.glVertex3d(end.x + 0.5 - cx, end.y + 1.0 - cy, end.z + 0.5 - cz);
                        GL11.glEnd();
                    }
                }
                Entity target = ((PathMobAccessor) m).endpractice$getWalkTarget();
                if (target != null && !target.removed) {
                    // the mob is after this: a thin red line to it
                    GL11.glLineWidth(1.0F);
                    GL11.glColor4f(1.0F, 0.2F, 0.2F, 0.6F);
                    GL11.glBegin(GL11.GL_LINES);
                    GL11.glVertex3d(mx, my + m.height * 0.8, mz);
                    GL11.glVertex3d(target.x - cx, target.y - cy - (target == p ? p.eyeHeight - 1.0 : 0), target.z - cz);
                    GL11.glEnd();
                    GL11.glLineWidth(2.5F);
                }
            }
        }

        long r3 = System.nanoTime();
        prof[6] += r3 - r2;
        if (cfg.sbTeleports && !teleports.isEmpty()) {
            GL11.glLineWidth(3.0F);
            GL11.glBegin(GL11.GL_LINES);
            for (double[] t : teleports) {
                float a = 1.0F - (float) (now - t[6]) / TELEPORT_SHOWN;
                GL11.glColor4f(1.0F, 0.3F, 1.0F, 0.3F + 0.6F * a);
                GL11.glVertex3d(t[0] - cx, t[1] + 1 - cy, t[2] - cz);
                GL11.glVertex3d(t[3] - cx, t[4] + 1 - cy, t[5] - cz);
            }
            GL11.glEnd();
        }

        if (cfg.sbSpawns && !spawns.isEmpty()) {
            GL11.glLineWidth(4.0F);
            GL11.glBegin(GL11.GL_LINES);
            for (double[] s : spawns) {
                glColor(s[4], 0.9F - 0.6F * (float) (now - s[3]) / SPAWN_SHOWN);
                GL11.glVertex3d(s[0] - cx, s[1] - cy, s[2] - cz);
                GL11.glVertex3d(s[0] - cx, s[1] + 6 - cy, s[2] - cz);
            }
            GL11.glEnd();
        }
        PitDesigns.render(mc, cx, cy, cz);
        prof[7] += System.nanoTime() - r3;
        GL11.glPopAttrib();
    }

    private static void ring(double r, float red, float green, float blue, double y) {
        GL11.glLineWidth(1.0F);
        GL11.glColor4f(red, green, blue, 0.35F);
        GL11.glBegin(GL11.GL_LINES);
        int n = 48;
        for (int i = 0; i < n; i += 2) {
            double a = Math.PI * 2 * i / n, b = Math.PI * 2 * (i + 1) / n;
            GL11.glVertex3d(Math.cos(a) * r, y, Math.sin(a) * r);
            GL11.glVertex3d(Math.cos(b) * r, y, Math.sin(b) * r);
        }
        GL11.glEnd();
    }
}
