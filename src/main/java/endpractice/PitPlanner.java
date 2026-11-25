package endpractice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import org.lwjgl.opengl.GL11;

/**
 * Pit planner setting: for endgame practice, picks the pit at the set distance from the tower at the world spawn (where
 * you respawn after each death in the pit), and draws the line between them on the ground. Every direction (every 10
 * degrees) is scored:
 * <ul>
 * <li>the run: water and lava to swim through, trees in the way, and climbs and drops along the line</li>
 * <li>view: how much of the spawnable ground around the route and the pit you can actually see, so hills that hide the
 * mobs cost and high ground with a wide view pays</li>
 * <li>spawnable ground: open surface that monsters can spawn on around the tower and the pit, instead of water, trees
 * or nothing (more of it means more of each spawn burst lands on the surface near you, not in caves)</li>
 * </ul>
 * The pit spot must be dry, flat, and solid (no cave, water or lava) 44 blocks down over 3x3.
 * The scan generates the terrain it looks at, a little every tick so the game doesn't freeze.
 */
public final class PitPlanner {
    private static final int DIRECTIONS = 36;
    private static final int TOWER_OFFSET = 16;
    /** How deep the pit must be solid, and the spots tried around the one on the line (center first). */
    private static final int PIT_DEPTH = 44;
    private static final int[][] PIT_TRIES = {{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4}, {4, 4}, {-4, -4}, {4, -4}, {-4, 4}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}};
    /** How far around you visibility is checked (the enderman draw distance), and how often along the route. */
    private static final int VIEW_RANGE = 48, VIEW_STEP = 40;
    /** Time the scan may take each tick. */
    private static final long BUDGET_NANOS = 4_000_000L;
    /** Ticks to wait after the world loads before scanning. */
    private static final int START_DELAY = 200;
    /** More than this between two ticks means the game is behind: no new chunk that tick. */
    private static final long BEHIND_NANOS = 70_000_000L;
    /** Directions per tower that get their terrain generated and scored, picked from the biome map first. */
    private static final int KEEP = 16;

    private static final class Plan {
        int tx, ty, tz, px, py, pz;
        double score, view;
        final List<int[]> line = new ArrayList<>();
    }

    /** A direction whose view is still being checked. */
    private static final class Job {
        Plan p;
        final List<int[]> points = new ArrayList<>();
        int index, weight;
        double view;
    }

    private static Job job;
    private static World world;
    private static int distance;
    private static final List<int[]> towers = new ArrayList<>();
    /** Next task: an index into order. */
    private static int next;
    private static int tasks;
    private static boolean planning;
    private static Plan best;
    private static double[] towerSpawnable;
    /** Chunks the next direction still needs generated (x << 32 | z), or null when not worked out yet. */
    private static List<Long> chunks;
    private static int waitTicks, ticks, slowTicks, chunksMade;
    private static long lastTick;
    /** The directions worth a closer look (tower index, direction), best by biome first. */
    private static final List<int[]> order = new ArrayList<>();
    private static long worst, worstTick;

    private PitPlanner() {
    }

    /** A new plan from a tower spot near x, z (searchTower) or exactly there (the re-plan key). */
    static void plan(Minecraft mc, int x, int z, boolean searchTower) {
        world = mc.world;
        distance = EndPractice.cfg.pitDistance;
        towers.clear();
        best = null;
        next = 0;
        chunks = null;
        job = null;
        ticks = 0;
        worst = 0;
        worstTick = 0;
        slowTicks = 0;
        waitTicks = searchTower ? 0 : START_DELAY;
        if (searchTower) {
            for (int dx = -TOWER_OFFSET; dx <= TOWER_OFFSET; dx += TOWER_OFFSET) {
                for (int dz = -TOWER_OFFSET; dz <= TOWER_OFFSET; dz += TOWER_OFFSET) {
                    int[] t = flatSpot(x + dx, z + dz, 2);
                    if (t != null) towers.add(t);
                }
            }
        }
        if (towers.isEmpty()) towers.add(new int[]{x, top(x, z), z});
        towerSpawnable = new double[towers.size()];
        for (int i = 0; i < towers.size(); i++) towerSpawnable[i] = -1;
        // the biome map costs nothing to read (no terrain is generated), so it picks which directions are worth
        // generating: no ocean, river or swamp on the way or at the pit, few forests (trees) or hills
        order.clear();
        BiomeSource biomes = world.getBiomeSource();
        for (int ti = 0; ti < towers.size(); ti++) {
            int[] t = towers.get(ti);
            List<double[]> scored = new ArrayList<>();
            for (int di = 0; di < DIRECTIONS; di++) {
                double a = Math.PI * 2 * di / DIRECTIONS, ux = Math.cos(a), uz = Math.sin(a), sc = 0;
                for (int s = 0; s <= distance + 16; s += 16) {
                    Biome b = biomes.getBiome((int) (t[0] + ux * s), (int) (t[2] + uz * s));
                    double w = s >= distance - 16 ? 3 : 1; // the pit end counts more
                    sc -= w * biomeCost(b);
                }
                scored.add(new double[]{sc, di});
            }
            scored.sort((p1, p2) -> Double.compare(p2[0], p1[0]));
            for (int k = 0; k < Math.min(KEEP, scored.size()); k++) order.add(new int[]{ti, (int) scored.get(k)[1]});
        }
        tasks = order.size();
        chunksMade = 0;
        lastTick = 0;
        planning = true;
        EndPractice.log("pit planner: " + towers.size() + " tower spots, " + distance + " blocks");
    }

    /** The planned pit (x, y, z), or null. */
    public static int[] pit() {
        Plan p = best;
        return p == null || planning ? null : new int[]{p.px, p.py, p.pz};
    }

    static void clear() {
        planning = false;
        best = null;
        job = null;
        world = null;
    }

    /** Some of the scan, every tick. */
    static void tick(Minecraft mc) {
        if (!planning || mc.world != world) {
            if (mc.world != world) clear();
            return;
        }
        if (waitTicks > 0) {
            // the world has just loaded: let the game settle before generating more
            waitTicks--;
            return;
        }
        long start = System.nanoTime();
        boolean behind = lastTick != 0 && start - lastTick > BEHIND_NANOS;
        lastTick = start;
        boolean generated = false;
        while (next < tasks && System.nanoTime() - start < BUDGET_NANOS) {
            int ti = order.get(next)[0], di = order.get(next)[1];
            if (chunks == null) chunks = chunksFor(ti, di);
            if (!chunks.isEmpty()) {
                // generate the terrain a direction needs, at most one new chunk a tick, and none while the game is
                // already behind (a slow frame or tick just before), so it never adds to a stutter
                long c = chunks.get(chunks.size() - 1);
                int cx = (int) (c >> 32), cz = (int) c;
                if (!world.isChunkLoaded(cx << 4, 64, cz << 4)) {
                    if (generated || behind) break;
                    world.getBlock(cx << 4, 64, cz << 4);
                    generated = true;
                    chunksMade++;
                }
                chunks.remove(chunks.size() - 1);
                continue;
            }
            long t0 = System.nanoTime();
            if (job != null) {
                if (viewStep()) {
                    chunks = null;
                    next++;
                }
            } else {
                evaluate(ti, di);
                if (job == null) {
                    // no pit spot that way
                    chunks = null;
                    next++;
                }
            }
            worst = Math.max(worst, System.nanoTime() - t0);
        }
        ticks++;
        worstTick = Math.max(worstTick, System.nanoTime() - start);
        if (System.nanoTime() - start > 25_000_000L) slowTicks++;
        if (next >= tasks) {
            planning = false;
            EndPractice.log(String.format(Locale.ROOT, "pit planner: %d ticks, %d chunks, slowest step %.1f ms, slowest tick %.1f ms, %d over 25 ms", ticks, chunksMade, worst / 1e6, worstTick / 1e6, slowTicks));
            if (best != null) {
                // where an eye thrown at the pit would go, against the way back to the tower (for checking the spawn)
                net.minecraft.util.math.BlockPos sh = world.findNearestStructure("Stronghold", best.px, 64, best.pz);
                if (sh != null) {
                    double ex = sh.x - best.px, ez = sh.z - best.pz, bx = best.tx - best.px, bz = best.tz - best.pz;
                    double ang = Math.toDegrees(Math.acos((ex * bx + ez * bz) / Math.sqrt((ex * ex + ez * ez) * (bx * bx + bz * bz))));
                    EndPractice.log(String.format(Locale.ROOT, "pit planner: stronghold %d blocks from the tower, %d from the pit, eye from the pit %.0f degrees off the way back",
                            (int) Math.hypot(sh.x - best.tx, sh.z - best.tz), (int) Math.hypot(ex, ez), ang));
                }
                EndPractice.log(String.format(Locale.ROOT, "pit planner: tower %d %d %d, pit %d %d %d, score %.1f, view %.2f",
                        best.tx, best.ty, best.tz, best.px, best.py, best.pz, best.score, best.view));
                EndPractice.chat(mc, "§dPit planner§r: tower at §e" + best.tx + " " + best.ty + " " + best.tz + "§r, pit at §e"
                        + best.px + " " + best.py + " " + best.pz + "§r (" + compass(best.px - best.tx, best.pz - best.tz) + ", " + distance + " blocks, view " + Math.round(best.view * 100) + "%)");
            } else {
                EndPractice.chat(mc, "§dPit planner§r: no dry, flat pit spot solid " + PIT_DEPTH + " blocks down, " + distance + " blocks out in any direction.");
            }
        }
    }

    private static double biomeCost(Biome b) {
        if (b == Biome.OCEAN || b == Biome.FROZEN_OCEAN || b == Biome.RIVER || b == Biome.FROZEN_RIVER || b == Biome.SWAMPLAND) return 3;
        if (b == Biome.EXTREME_HILLS || b == Biome.ICE_MOUNTAINS || b == Biome.MUSHROOM_ISLAND || b == Biome.MUSHROOM_ISLAND_SHORE) return 2;
        if (b == Biome.FOREST || b == Biome.TAIGA) return 1;
        return 0;
    }

    /** The not yet loaded chunks evaluate(ti, di) reads: the route, the view around it, and the ground around the pit and tower. */
    private static List<Long> chunksFor(int ti, int di) {
        int[] t = towers.get(ti);
        double a = Math.PI * 2 * di / DIRECTIONS;
        double ux = Math.cos(a), uz = Math.sin(a);
        LinkedHashSet<Long> set = new LinkedHashSet<>();
        for (int s = 0; s <= distance; s += 8) area(set, (int) Math.floor(t[0] + ux * s), (int) Math.floor(t[2] + uz * s), 2);
        for (int s = VIEW_STEP / 2; s < distance; s += VIEW_STEP) area(set, (int) Math.floor(t[0] + ux * s), (int) Math.floor(t[2] + uz * s), VIEW_RANGE);
        int px = (int) Math.floor(t[0] + ux * distance), pz = (int) Math.floor(t[2] + uz * distance);
        area(set, px, pz, VIEW_RANGE + 9);
        area(set, t[0], t[2], 33);
        List<Long> out = new ArrayList<>();
        for (long c : set) {
            if (!world.isChunkLoaded((int) (c >> 32) << 4, 64, (int) c << 4)) out.add(c);
        }
        Collections.reverse(out); // taken from the end, so the route comes first
        return out;
    }

    private static void area(Set<Long> set, int x, int z, int r) {
        for (int cx = (x - r) >> 4; cx <= (x + r) >> 4; cx++) {
            for (int cz = (z - r) >> 4; cz <= (z + r) >> 4; cz++) set.add(((long) cx << 32) | (cz & 0xFFFFFFFFL));
        }
    }

    private static void evaluate(int ti, int di) {
        int[] t = towers.get(ti);
        double a = Math.PI * 2 * di / DIRECTIONS;
        double ux = Math.cos(a), uz = Math.sin(a);
        int px = (int) Math.floor(t[0] + ux * distance), pz = (int) Math.floor(t[2] + uz * distance);
        int[] pit = null;
        // the spot on the line, or the nearest one around it that is flat, dry and solid all the way down
        for (int k = 0; k < PIT_TRIES.length && pit == null; k++) {
            int[] c = flatSpot(px + PIT_TRIES[k][0], pz + PIT_TRIES[k][1], 1);
            if (c != null && solidBelow(c[0], c[1], c[2])) pit = c;
        }
        if (pit == null) return;
        // the run there
        Plan p = new Plan();
        double penalty = 0;
        int lastY = t[1];
        for (int s = 0; s <= distance; s += 2) {
            int x = (int) Math.floor(t[0] + ux * s), z = (int) Math.floor(t[2] + uz * s);
            int y = top(x, z);
            int id = world.getBlock(x, y - 1, z);
            Material m = world.getMaterial(x, y - 1, z);
            if (m == Material.LAVA) penalty += 200;
            else if (m.isLiquid()) penalty += 20;
            if (id == Block.LEAVES.id || id == Block.LOG.id) penalty += 8;
            int dy = Math.abs(y - lastY);
            penalty += dy * 0.5 + Math.max(0, dy - 1) * 4;
            lastY = y;
            if (s % 4 == 0) p.line.add(new int[]{x, y, z});
        }
        if (towerSpawnable[ti] < 0) towerSpawnable[ti] = spawnable(t[0], t[2]);
        double spawn = towerSpawnable[ti] + spawnable(pit[0], pit[2]);
        p.score = spawn * 100 - penalty;
        p.tx = t[0];
        p.ty = t[1];
        p.tz = t[2];
        p.px = pit[0];
        p.py = pit[1];
        p.pz = pit[2];
        // what you can see on the way: the share of the ground around a few points on the route (and the pit, counted
        // double) that is open, spawnable and in your line of sight, so a hill that hides the mobs costs and a rise with
        // a wide view pays. One point at a time (viewStep), since the line of sight checks are the slow part.
        job = new Job();
        job.p = p;
        for (int s = VIEW_STEP / 2; s < distance; s += VIEW_STEP) {
            int x = (int) Math.floor(t[0] + ux * s), z = (int) Math.floor(t[2] + uz * s);
            job.points.add(new int[]{x, top(x, z), z, 1});
        }
        job.points.add(new int[]{pit[0], pit[1], pit[2], 2});
    }

    /** One view point of the direction being scored; true when that direction is done. */
    private static boolean viewStep() {
        int[] q = job.points.get(job.index++);
        job.view += visible(q[0], q[1], q[2]) * q[3];
        job.weight += q[3];
        if (job.index < job.points.size()) return false;
        Plan p = job.p;
        p.view = job.view / job.weight;
        p.score += p.view * 120;
        if (best == null || p.score > best.score) best = p;
        job = null;
        return true;
    }

    /**
     * Share of the ground within VIEW_RANGE of a spot (standing at x, y, z) that monsters can spawn on and that you can see
     * from eye height: a line from your eyes to an enderman's chest over that block hits no terrain.
     */
    private static double visible(int cx, int cy, int cz) {
        int seen = 0, all = 0;
        double ex = cx + 0.5, ey = cy + 1.62, ez = cz + 0.5;
        for (int dx = -VIEW_RANGE; dx <= VIEW_RANGE; dx += 8) {
            for (int dz = -VIEW_RANGE; dz <= VIEW_RANGE; dz += 8) {
                int d2 = dx * dx + dz * dz;
                if (d2 > VIEW_RANGE * VIEW_RANGE || d2 < 64) continue;
                int x = cx + dx, z = cz + dz;
                int y = top(x, z);
                all++;
                int id = world.getBlock(x, y - 1, z);
                if (id == Block.LEAVES.id || !world.isSolidBlock(x, y - 1, z) || world.getMaterial(x, y - 1, z).isLiquid()) continue;
                if (world.rayTrace(Vec3d.fromPool(ex, ey, ez), Vec3d.fromPool(x + 0.5, y + 1.5, z + 0.5), false, true) == null) seen++;
            }
        }
        return all == 0 ? 0 : (double) seen / all;
    }

    /** Whether every block of the 3x3 under x, y, z is solid down to PIT_DEPTH below the surface (no cave, water or lava). */
    private static boolean solidBelow(int x, int y, int z) {
        if (y - PIT_DEPTH < 1) return false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int yy = y - 1; yy >= y - PIT_DEPTH; yy--) {
                    if (!world.isSolidBlock(x + dx, yy, z + dz) || world.getMaterial(x + dx, yy, z + dz).isLiquid()) return false;
                }
            }
        }
        return true;
    }

    /** Share of the surface within 32 blocks that monsters can spawn on (solid ground under open sky). */
    private static double spawnable(int cx, int cz) {
        int ok = 0, all = 0;
        for (int dx = -32; dx <= 32; dx += 4) {
            for (int dz = -32; dz <= 32; dz += 4) {
                if (dx * dx + dz * dz > 32 * 32) continue;
                int x = cx + dx, z = cz + dz;
                int y = top(x, z);
                all++;
                int id = world.getBlock(x, y - 1, z);
                if (id != Block.LEAVES.id && world.isSolidBlock(x, y - 1, z) && !world.getMaterial(x, y - 1, z).isLiquid()) ok++;
            }
        }
        return all == 0 ? 0 : (double) ok / all;
    }

    /** A dry spot within 1 block of level over (2r+1)^2 around x, z (generating it): x, y, z or null. */
    private static int[] flatSpot(int x, int z, int r) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int y = top(x + dx, z + dz);
                Material m = world.getMaterial(x + dx, y - 1, z + dz);
                int id = world.getBlock(x + dx, y - 1, z + dz);
                if (m.isLiquid() || id == Block.LEAVES.id || id == Block.LOG.id) return null;
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return max - min > 1 ? null : new int[]{x, top(x, z), z};
    }

    /** The y above the top block at x, z (generating the chunk if needed). */
    private static int top(int x, int z) {
        world.getBlock(x, 64, z);
        return world.getHeight(x, z);
    }

    private static String compass(int dx, int dz) {
        String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        double deg = Math.toDegrees(Math.atan2(-dx, dz)); // 0 is south (+z), 90 west, like Minecraft's yaw
        int i = (int) Math.floor(((deg % 360 + 360) % 360 + 22.5) / 45) % 8;
        return names[i];
    }

    /** Share of the area within 128 blocks of the tower that is over 128 blocks from the pit (despawns there). */
    static int flushPercent(int d) {
        int in = 0, out = 0;
        for (int x = -128; x <= 128; x += 2) {
            for (int z = -128; z <= 128; z += 2) {
                if (x * x + z * z > 128 * 128) continue;
                in++;
                double dx = x - d;
                if (dx * dx + z * z > 128 * 128) out++;
            }
        }
        return Math.round(100.0F * out / in);
    }

    static List<String> hud(Minecraft mc, boolean show) {
        List<String> lines = new ArrayList<>();
        if (!show || mc.world == null || mc.world != world) return lines;
        if (planning) {
            lines.add("§7Planning pit " + (100 * next / Math.max(1, tasks)) + "%§r");
        } else if (best != null) {
            int dx = best.px - (int) Math.floor(mc.player.x), dz = best.pz - (int) Math.floor(mc.player.z);
            lines.add("Pit §e" + best.px + " " + best.py + " " + best.pz + "§r  " + compass(dx, dz) + " " + (int) Math.sqrt(dx * dx + dz * dz)
                    + "m  §7flush " + flushPercent(distance) + "%§r");
        }
        return lines;
    }

    /** The line on the ground and markers over the tower spot and the pit, from WorldRenderer.renderEntities. */
    public static void render(Minecraft mc, float tickDelta) {
        Plan p = best;
        if (endpractice.compat.RankedCompat.busy()) return;
        if (p == null || mc.world != world || mc.player == null || !(Endgame.showPitPlan() || PitSandbox.tracking(mc) && EndPractice.cfg.pitPlanner)) return;
        double cx = mc.player.prevX + (mc.player.x - mc.player.prevX) * tickDelta;
        double cy = mc.player.prevY + (mc.player.y - mc.player.prevY) * tickDelta;
        double cz = mc.player.prevZ + (mc.player.z - mc.player.prevZ) * tickDelta;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(3.0F);
        GL11.glColor4f(0.2F, 0.9F, 1.0F, 0.8F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int[] q : p.line) GL11.glVertex3d(q[0] + 0.5 - cx, q[1] + 0.15 - cy, q[2] + 0.5 - cz);
        GL11.glVertex3d(p.px + 0.5 - cx, p.py + 0.15 - cy, p.pz + 0.5 - cz);
        GL11.glEnd();
        GL11.glLineWidth(5.0F);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glColor4f(1.0F, 0.85F, 0.2F, 0.9F); // tower spot
        GL11.glVertex3d(p.tx + 0.5 - cx, p.ty - cy, p.tz + 0.5 - cz);
        GL11.glVertex3d(p.tx + 0.5 - cx, p.ty + 40 - cy, p.tz + 0.5 - cz);
        GL11.glColor4f(1.0F, 0.2F, 0.2F, 0.9F); // pit
        GL11.glVertex3d(p.px + 0.5 - cx, p.py - cy, p.pz + 0.5 - cz);
        GL11.glVertex3d(p.px + 0.5 - cx, p.py + 40 - cy, p.pz + 0.5 - cz);
        GL11.glEnd();
        GL11.glPopAttrib();
    }
}
