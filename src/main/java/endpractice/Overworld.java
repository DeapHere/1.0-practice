package endpractice;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

/**
 * Overworld practice: a fresh world where you spawn with 4 logs near trees, a cave with exposed iron and gravel, and
 * a lava pool on the surface. Splits for the stone pick, the iron, the bucket and flint and steel, the portal and the
 * nether, PBs for each, and how long you stood waiting on furnaces.
 */
public final class Overworld {
    private Overworld() {
    }

    enum Split {
        STONE_PICK("Stone pick"), IRON("Iron"), TOOLS("Bucket + flint & steel"), PORTAL("Portal lit"), ENTER("Portal entered");

        final String label;

        Split(String label) {
            this.label = label;
        }
    }

    private static boolean active;
    private static int attempts;
    private static boolean started;
    private static long startTick, clock, endTick = -1;
    private static int current;
    private static final long[] at = new long[Split.values().length];
    private static double sx, sy, sz;
    private static String result;
    /** Furnace stats: ticks smelting iron, ticks you stood waiting on it, ticks one furnace had 2+ more ore queued. */
    private static long smeltTicks, waitTicks, queueTicks;
    private static int furnacesUsed;
    private static final List<int[]> furnacesSeen = new ArrayList<>();
    /** For "waiting": where you were and what you had a second ago. */
    private static double lastX, lastZ;
    private static int lastInvHash, idleTicks;
    private static String spotInfo;
    /** Logs you've had over the run (the 4 you spawn with and every one picked up; crafting them doesn't take any off). */
    private static int logsGot, lastLogs;
    private static boolean screenWasOpen;
    private static long missingSaidAt = -1000;

    public static boolean isActive() {
        return active;
    }

    static void stop() {
        active = false;
    }

    // ---- finding a world ------------------------------------------------------------------------------------------

    /**
     * Lava lakes are placed while chunks populate, with the chunk's own random. That random can be worked out from the
     * seed alone, so the chunks with a lava lake that starts at the surface (y 63 or more) are known before anything
     * generates. Mineshafts in the chunk can shift it, so each one is still checked.
     */
    static List<int[]> surfaceLavaChunks(long seed, int radius) {
        List<int[]> out = new ArrayList<>();
        Random r = new Random(seed);
        long l = r.nextLong() / 2L * 2L + 1L;
        long m = r.nextLong() / 2L * 2L + 1L;
        for (int cx = -radius; cx <= radius; cx++) {
            for (int cz = -radius; cz <= radius; cz++) {
                r.setSeed(cx * l + cz * m ^ seed);
                if (r.nextInt(4) == 0) {
                    r.nextInt(16);
                    r.nextInt(128);
                    r.nextInt(16);
                    int n = r.nextInt(4) + 4;
                    for (int i = 0; i < n * 6; i++) r.nextDouble();
                }
                if (r.nextInt(8) == 0) {
                    int x = cx * 16 + r.nextInt(16) + 8;
                    int y = r.nextInt(r.nextInt(120) + 8);
                    int z = cz * 16 + r.nextInt(16) + 8;
                    if (y >= 63 && r.nextInt(10) == 0) out.add(new int[]{x, y, z});
                }
            }
        }
        out.sort((a, b) -> Long.compare((long) a[0] * a[0] + (long) a[2] * a[2], (long) b[0] * b[0] + (long) b[2] * b[2]));
        return out;
    }

    private static final Random seeds = new Random();
    private static List<int[]> candidates;

    /** A seed with at least one surface lava lake predicted within 400 blocks of 0,0. */
    private static long nextSeed() {
        while (true) {
            long s = seeds.nextLong();
            List<int[]> c = surfaceLavaChunks(s, 25);
            if (!c.isEmpty()) {
                candidates = c;
                return s;
            }
        }
    }

    private static boolean isGround(int id) {
        // not air, leaves, logs, plants, snow, fire, water or lava
        switch (id) {
            case 0: case 8: case 9: case 10: case 11: case 17: case 18: case 31: case 32: case 37: case 38: case 39: case 40:
            case 50: case 51: case 59: case 78: case 81: case 83: case 106:
                return false;
            default:
                return true;
        }
    }

    private static final int R = 44, TOP = 127, BOTTOM = 8;

    /** Checks the predicted lava lakes in this world; the spot to spawn, or null. */
    private static Vec3i find(World w) {
        if (candidates == null) return null;
        int checked = 0;
        for (int[] c : candidates) {
            if (checked >= 3) break;
            Biome b = w.getBiomeSource().getBiome(c[0], c[2]);
            if (b == Biome.OCEAN || b == Biome.FROZEN_OCEAN || b == Biome.RIVER || b == Biome.FROZEN_RIVER || b == Biome.DESERT
                    || b == Biome.SWAMPLAND || b == Biome.EXTREME_HILLS || b == Biome.ICE_PLAINS || b == Biome.ICE_MOUNTAINS || b == Biome.MUSHROOM_ISLAND
                    || b == Biome.MUSHROOM_ISLAND_SHORE) continue;
            checked++;
            Vec3i v = check(w, c[0], c[2]);
            if (v != null) return v;
        }
        return null;
    }

    private static Vec3i check(World w, int lx, int lz) {
        // 1. the lava, with open sky over it
        int[] lava = null;
        for (int x = lx - 12; x <= lx + 12 && lava == null; x++) {
            for (int z = lz - 12; z <= lz + 12 && lava == null; z++) {
                for (int y = TOP; y > 55; y--) {
                    int id = w.getBlock(x, y, z);
                    if (id == 0) continue;
                    if (id == 10 || id == 11) lava = new int[]{x, y, z};
                    break;
                }
            }
        }
        if (lava == null) return null;
        // 2. the ground and the caves round it
        int size = 2 * R + 1;
        int[] ground = new int[size * size];
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int y = TOP;
                while (y > 1 && !isGround(w.getBlock(lava[0] - R + x, y, lava[2] - R + z))) y--;
                ground[x * size + z] = y;
            }
        }
        int height = TOP - BOTTOM + 1;
        boolean[] seen = new boolean[size * size * height];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        List<int[]> entrances = new ArrayList<>(), iron = new ArrayList<>(), gravel = new ArrayList<>();
        int deepest = 0;
        // entrances: underground air next to open air
        for (int x = 1; x < size - 1; x++) {
            for (int z = 1; z < size - 1; z++) {
                int g = ground[x * size + z];
                for (int y = Math.max(BOTTOM, g - 3); y < g; y++) {
                    if (w.getBlock(lava[0] - R + x, y, lava[2] - R + z) != 0) continue;
                    boolean open = ground[(x + 1) * size + z] < y || ground[(x - 1) * size + z] < y || ground[x * size + z + 1] < y || ground[x * size + z - 1] < y;
                    if (!open) continue;
                    int k = (x * size + z) * height + (y - BOTTOM);
                    if (!seen[k]) {
                        seen[k] = true;
                        queue.add(new int[]{x, y, z});
                        entrances.add(new int[]{lava[0] - R + x, y, lava[2] - R + z});
                    }
                }
            }
        }
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        int visited = 0;
        while (!queue.isEmpty() && visited < 300000) {
            int[] c = queue.poll();
            visited++;
            int g = ground[c[0] * size + c[2]];
            deepest = Math.max(deepest, g - c[1]);
            for (int[] d : dirs) {
                int x = c[0] + d[0], y = c[1] + d[1], z = c[2] + d[2];
                if (x < 0 || z < 0 || x >= size || z >= size || y < BOTTOM || y > TOP) continue;
                int id = w.getBlock(lava[0] - R + x, y, lava[2] - R + z);
                if (id == 15) iron.add(new int[]{lava[0] - R + x, y, lava[2] - R + z});
                if (id == 13) gravel.add(new int[]{lava[0] - R + x, y, lava[2] - R + z});
                if (id != 0 || y >= ground[x * size + z]) continue;
                int k = (x * size + z) * height + (y - BOTTOM);
                if (seen[k]) continue;
                seen[k] = true;
                queue.add(new int[]{x, y, z});
            }
        }
        if (deepest < 10 || distinct(iron) < 6 || distinct(gravel) < 3) return null;
        // 3. a spawn spot on grass by some trees, close to the lava and a cave way in
        Vec3i best = null;
        double bestScore = Double.MAX_VALUE;
        for (int x = 4; x < size - 4; x += 2) {
            for (int z = 4; z < size - 4; z += 2) {
                int wx = lava[0] - R + x, wz = lava[2] - R + z, g = ground[x * size + z];
                if (w.getBlock(wx, g, wz) != 2 || w.getBlock(wx, g + 1, wz) != 0 || w.getBlock(wx, g + 2, wz) != 0) continue;
                double dl = Math.hypot(wx - lava[0], wz - lava[2]);
                if (dl < 6 || dl > 32) continue;
                double de = Double.MAX_VALUE;
                for (int[] e : entrances) de = Math.min(de, Math.hypot(e[0] - wx, e[2] - wz));
                if (de > 36) continue;
                int logs = 0;
                for (int ax = -12; ax <= 12 && logs < 12; ax++) {
                    for (int az = -12; az <= 12 && logs < 12; az++) {
                        for (int ay = 0; ay <= 6; ay++) if (w.getBlock(wx + ax, g + 1 + ay, wz + az) == 17) logs++;
                    }
                }
                if (logs < 8) continue;
                double score = dl + de;
                if (score < bestScore) {
                    bestScore = score;
                    best = new Vec3i(wx, g + 1, wz);
                }
            }
        }
        if (best != null) {
            spotInfo = "lava at " + lava[0] + " " + lava[1] + " " + lava[2] + ", " + distinct(iron) + " iron and " + distinct(gravel) + " gravel showing in caves "
                    + deepest + " deep, " + entrances.size() + " cave openings";
            EndPractice.log("overworld spot " + best.x + " " + best.y + " " + best.z + ": " + spotInfo);
        }
        return best;
    }

    private static int distinct(List<int[]> l) {
        java.util.Set<Long> s = new java.util.HashSet<>();
        for (int[] p : l) s.add(((long) p[0] << 40) ^ ((long) p[1] << 20) ^ (p[2] & 0xFFFFF));
        return s.size();
    }

    // ---- the attempt ------------------------------------------------------------------------------------------------

    static void start(Minecraft mc) {
        attempts++;
        active = false;
        result = null;
        candidates = null;
        Vec3i spot = Endgame.makeCustomWorld(mc, "Overworld Practice", Overworld::nextSeed, Overworld::find, 25);
        PlayerEntity p = mc.player;
        EndPractice.giveKit(p, EndPractice.cfg.overworldKit);
        p.setHealth(20);
        p.getHungerManager().add(20, 20.0F);
        mc.world.setTime(0);
        started = false;
        current = 0;
        for (int i = 0; i < at.length; i++) at[i] = -1;
        clock = 0;
        endTick = -1;
        smeltTicks = waitTicks = queueTicks = 0;
        furnacesUsed = 0;
        furnacesSeen.clear();
        idleTicks = 0;
        logsGot = lastLogs = count(p, 17);
        screenWasOpen = false;
        missingSaidAt = -1000;
        sx = lastX = p.x;
        sy = p.y;
        sz = lastZ = p.z;
        lastInvHash = invHash(p);
        Commands.cheated = false;
        active = true;
        loadBest();
        EndPractice.chat(mc, "§dOverworld practice§r attempt " + attempts + ": trees, a cave with iron and gravel, and a lava pool on the surface are all close. "
                + "The timer starts when you move. " + Config.keyName(EndPractice.cfg.keyOverworld) + " for a new world.");
        if (spot == null) EndPractice.chat(mc, "§cCouldn't find a world with all of that this time, so this one may be missing something.");
    }

    private static int invHash(PlayerEntity p) {
        int h = 1;
        for (ItemStack s : p.inventory.items) h = h * 31 + (s == null ? 0 : s.id * 1000 + s.size);
        return h;
    }

    private static int count(PlayerEntity p, int id) {
        int n = 0;
        for (ItemStack s : p.inventory.items) if (s != null && s.id == id) n += s.size;
        return n;
    }

    /** Iron you've smelted so far, counting what you've already made with it. */
    private static int ironGot(PlayerEntity p) {
        return count(p, 265) + 3 * (count(p, 325) + count(p, 326) + count(p, 327)) + count(p, 259) + 3 * count(p, 257) + count(p, 256) + 2 * count(p, 267)
                + 3 * count(p, 258) + 2 * count(p, 292);
    }

    private static boolean portalNear(Minecraft mc) {
        PlayerEntity p = mc.player;
        int px = (int) Math.floor(p.x), py = (int) Math.floor(p.y), pz = (int) Math.floor(p.z);
        for (int x = px - 12; x <= px + 12; x++)
            for (int z = pz - 12; z <= pz + 12; z++)
                for (int y = Math.max(1, py - 12); y <= Math.min(126, py + 12); y++)
                    if (mc.world.getBlock(x, y, z) == 90) return true;
        return false;
    }

    static void tick(Minecraft mc) {
        if (!active || mc.player == null || mc.world == null) return;
        PlayerEntity p = mc.player;
        if (!started) {
            if (Math.abs(p.x - sx) + Math.abs(p.z - sz) > 0.05 || Math.abs(p.y - sy) > 0.6 || invHash(p) != lastInvHash) {
                started = true;
                startTick = clock;
            } else {
                lastInvHash = invHash(p);
                return;
            }
        }
        clock++;
        long now = clock - startTick;
        if (endTick >= 0) return;
        // logs: count what comes in with no screen open (picked up), so moving them round a crafting grid doesn't count twice
        int logsNow = count(p, 17);
        boolean screenOpen = mc.screen != null;
        if (!screenOpen && !screenWasOpen && logsNow > lastLogs) logsGot += logsNow - lastLogs;
        lastLogs = logsNow;
        screenWasOpen = screenOpen;
        // every split is checked on its own, so one you skip (less iron, a portal without flint and steel) doesn't
        // hold up the rest; stepping into the portal ends the run whatever is left
        boolean inPortal = inPortal(mc);
        Split[] sp = Split.values();
        for (int i = 0; i < sp.length; i++) {
            if (at[i] >= 0) continue;
            boolean done = false;
            switch (sp[i]) {
                case STONE_PICK: done = count(p, 274) > 0; break;
                case IRON: done = ironGot(p) >= EndPractice.cfg.overworldIron; break;
                case TOOLS: done = (count(p, 325) + count(p, 326) + count(p, 327)) > 0 && count(p, 259) > 0; break;
                case PORTAL: done = inPortal || mc.world.dimension.id == -1 || (clock % 10 == 0 && portalNear(mc)); break;
                case ENTER:
                    if (inPortal || mc.world.dimension.id == -1) {
                        String missing = missing(p);
                        if (missing == null) done = true;
                        else if (clock - missingSaidAt > 60) {
                            missingSaidAt = clock;
                            EndPractice.chat(mc, "§cNot done yet:§r still need " + missing + ".");
                        }
                    }
                    break;
            }
            if (done) split(mc, sp[i], i, now);
        }
        if (endTick >= 0) return;
        // furnaces cooking iron near you
        int cooking = 0, queued = 0;
        boolean nearFurnace = false;
        if (mc.world.dimension.id == 0) {
            for (BlockEntity be : mc.world.blockEntities) {
                if (!(be instanceof FurnaceBlockEntity)) continue;
                double dx = be.x + 0.5 - p.x, dz = be.z + 0.5 - p.z;
                if (dx * dx + dz * dz > 48 * 48) continue;
                FurnaceBlockEntity f = (FurnaceBlockEntity) be;
                ItemStack in = f.getItem(0);
                if (in != null && in.id == 15 && f.fuelTime > 0) {
                    cooking++;
                    queued = Math.max(queued, in.size - 1);
                    if (dx * dx + dz * dz < 36) nearFurnace = true;
                }
            }
        }
        // idle: hardly moved and nothing new in your inventory for a second (taking ingots out doesn't count as doing something)
        int h = invHash(p);
        boolean moved = Math.abs(p.x - lastX) + Math.abs(p.z - lastZ) > 0.15;
        boolean changed = h != lastInvHash;
        lastX = p.x;
        lastZ = p.z;
        lastInvHash = h;
        boolean atFurnaceScreen = mc.screen instanceof net.minecraft.client.gui.screen.inventory.menu.FurnaceScreen;
        if (moved || (changed && !atFurnaceScreen) || (mc.screen != null && !atFurnaceScreen)) idleTicks = 0;
        else idleTicks++;
        // furnaces used: the most cooking at once (one picked up and put down again elsewhere is still one)
        furnacesUsed = Math.max(furnacesUsed, cooking);
        if (cooking > 0) {
            smeltTicks++;
            if (idleTicks >= 20 && (nearFurnace || atFurnaceScreen)) waitTicks++;
            if (cooking == 1 && queued >= 2) queueTicks++;
        }
    }

    /** What's still needed before the portal finishes the run (leaves held, logs had), or null. */
    private static String missing(PlayerEntity p) {
        int leaves = count(p, 18), needLeaves = EndPractice.cfg.overworldLeaves, needLogs = EndPractice.cfg.overworldLogs;
        List<String> m = new ArrayList<>();
        if (leaves < needLeaves) m.add((needLeaves - leaves) + " more leaves (" + leaves + "/" + needLeaves + ")");
        if (logsGot < needLogs) m.add((needLogs - logsGot) + " more logs (" + logsGot + "/" + needLogs + ")");
        return m.isEmpty() ? null : String.join(" and ", m);
    }

    /** Your feet or head in a portal block. */
    private static boolean inPortal(Minecraft mc) {
        PlayerEntity p = mc.player;
        int x = (int) Math.floor(p.x), z = (int) Math.floor(p.z), feet = (int) Math.floor(p.y - p.eyeHeight + 0.01);
        return mc.world.getBlock(x, feet, z) == 90 || mc.world.getBlock(x, feet + 1, z) == 90;
    }

    private static void split(Minecraft mc, Split s, int i, long now) {
        at[i] = now;
        Long pb = best.get(s.name());
        long prev = 0;
        for (long a : at) if (a >= 0 && a < now) prev = Math.max(prev, a);
        long seg = now - prev;
        String vs = pb == null ? "" : " (" + (now <= pb ? "§a-" : "§c+") + EndPractice.time(Math.abs(now - pb)) + "§r)";
        EndPractice.chat(mc, "§e" + s.label + "§r " + EndPractice.time(now) + vs + "  §7segment " + EndPractice.time(seg) + "§r");
        if (s == Split.IRON) EndPractice.chat(mc, furnaceLine());
        current = i + 1;
        if (s == Split.ENTER) finish(mc, now);
    }

    private static String furnaceLine() {
        String line = "§7Furnaces: " + furnacesUsed + " used, smelting " + EndPractice.time(smeltTicks) + ", you stood waiting " + EndPractice.time(waitTicks);
        if (queueTicks >= 100) line += ". One furnace had 2+ ore queued for " + EndPractice.time(queueTicks) + ": a second one would have saved about " + EndPractice.time(queueTicks / 2);
        return line + "§r";
    }

    private static void finish(Minecraft mc, long now) {
        endTick = now;
        result = "Portal entered in " + EndPractice.time(now);
        if (at[Split.IRON.ordinal()] < 0 && smeltTicks > 0) EndPractice.chat(mc, furnaceLine());
        Long pb = best.get("RUN");
        boolean saved = !Commands.cheated;
        if (saved) {
            if (pb == null || now < pb) {
                EndPractice.chat(mc, pb == null ? "§6First overworld finished§r" : "§6New PB!§r was " + EndPractice.time(pb));
                best.put("RUN", now);
            }
            for (int i = 0; i < at.length; i++) {
                String k = Split.values()[i].name();
                Long b = best.get(k);
                if (at[i] >= 0 && (b == null || at[i] < b)) best.put(k, at[i]);
            }
            save(now);
        } else {
            EndPractice.chat(mc, "§7Not saved: commands were used.§r");
        }
        EndPractice.chat(mc, "§dOverworld§r " + EndPractice.time(now) + ". " + Config.keyName(EndPractice.cfg.keyOverworld) + " for a new world.");
    }

    static List<String> hud(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (!active || mc.world == null) return lines;
        long now = started ? (endTick >= 0 ? endTick : clock - startTick) : 0;
        Long run = best.get("RUN");
        lines.add("§dOverworld§r  " + EndPractice.time(now) + (run != null ? "  §7PB " + EndPractice.time(run) + "§r" : "") + (started ? "" : "  §7(move to start)§r"));
        Split[] sp = Split.values();
        for (int i = 0; i < sp.length; i++) {
            Long pb = best.get(sp[i].name());
            if (at[i] >= 0) lines.add("  " + sp[i].label + " §e" + EndPractice.time(at[i]) + "§r" + (pb != null ? "  §7PB " + EndPractice.time(pb) + "§r" : ""));
            else if (endTick >= 0) lines.add("  " + sp[i].label + " §7skipped§r");
            else lines.add("> " + sp[i].label + (pb != null ? "  §7PB " + EndPractice.time(pb) + "§r" : ""));
        }
        if (endTick < 0 && mc.player != null && (EndPractice.cfg.overworldLeaves > 0 || EndPractice.cfg.overworldLogs > 0)) {
            int leaves = count(mc.player, 18);
            String lc = leaves >= EndPractice.cfg.overworldLeaves ? "§a" : "§7", gc = logsGot >= EndPractice.cfg.overworldLogs ? "§a" : "§7";
            lines.add(lc + "Leaves " + leaves + "/" + EndPractice.cfg.overworldLeaves + "§r  " + gc + "Logs " + logsGot + "/" + EndPractice.cfg.overworldLogs + "§r");
        }
        if (smeltTicks > 0 && endTick < 0) lines.add("§7Smelting " + EndPractice.time(smeltTicks) + ", waited " + EndPractice.time(waitTicks) + "§r");
        if (result != null) lines.add(result);
        return lines;
    }

    // ---- PBs: config/endpractice-overworld.txt: time,run ticks,split ticks..,smelt,wait,furnaces -------------------

    private static final Map<String, Long> best = new HashMap<>();

    private static File file() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-overworld.txt");
    }

    private static void save(long run) {
        try {
            File f = file();
            f.getParentFile().mkdirs();
            StringBuilder sb = new StringBuilder();
            sb.append(System.currentTimeMillis()).append(',').append(run);
            for (long a : at) sb.append(',').append(a);
            sb.append(',').append(smeltTicks).append(',').append(waitTicks).append(',').append(furnacesUsed).append('\n');
            Files.write(f.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            EndPractice.log("couldn't save the overworld run: " + e);
        }
    }

    private static void loadBest() {
        best.clear();
        try {
            File f = file();
            if (!f.exists()) return;
            Split[] sp = Split.values();
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String[] s = line.split(",");
                if (s.length < 2 + sp.length) continue;
                long run = Long.parseLong(s[1]);
                best.merge("RUN", run, Math::min);
                for (int i = 0; i < sp.length; i++) {
                    long a = Long.parseLong(s[2 + i]);
                    if (a >= 0) best.merge(sp[i].name(), a, Math::min);
                }
            }
        } catch (Exception e) {
            EndPractice.log("couldn't read the overworld runs: " + e);
        }
    }

    static List<String> pbLines() {
        loadBest();
        List<String> out = new ArrayList<>();
        Long run = best.get("RUN");
        out.add("§dOverworld§r" + (run == null ? "   §7none§r" : "   PB §e" + EndPractice.time(run) + "§r"));
        for (Split s : Split.values()) {
            Long b = best.get(s.name());
            if (b != null) out.add("  " + s.label + " §e" + EndPractice.time(b) + "§r");
        }
        return out;
    }
}
