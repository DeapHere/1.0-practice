package endpractice;

import endpractice.compat.Versions;
import endpractice.mixin.ChunkCacheAccessor;
import endpractice.mixin.StructureFeatureAccessor;
import endpractice.mixin.StructurePieceInvoker;
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
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SurvivalInteractionManager;
import net.minecraft.entity.mob.monster.BlazeEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.gen.chunk.NetherChunkGenerator;
import net.minecraft.world.gen.structure.FortressPieces;
import net.minecraft.world.gen.structure.StructureBox;
import net.minecraft.world.gen.structure.StructurePiece;
import net.minecraft.world.gen.structure.StructureStart;

/**
 * Blaze practice: a new world, straight into the nether at the doorway of a fortress's blaze spawner room, either an
 * open one (out in the open nether air) or an enclosed one (buried in netherrack), with the blaze kit. Timed from
 * arriving until the set number of blaze rods is in your inventory. Rods drop like vanilla (0 or 1 a blaze).
 * <p>
 * Fortress positions come from the seed the same way the game places them (one chance in three per 16x16 chunk
 * region), so only the fortresses' own chunks are generated, and their pieces say where the spawner rooms are.
 */
public final class BlazePractice {
    public static final String FOLDER = "BlazePractice";
    /** Share of the ground around a spawner room that is air or lava: under ENCLOSED is enclosed, over OPEN is open. */
    private static final double ENCLOSED = 0.25, OPEN = 0.5;

    private static boolean active;
    static boolean preparing;
    private static boolean enclosedMode;
    private static long startTime, endTime = -1;
    private static String result;
    private static int attempts, kills, rodsDropped;
    private static String roomText;
    private static final Map<String, Long> pbs = new HashMap<>();
    private static boolean pbLoaded;

    private BlazePractice() {
    }

    public static boolean isActive() {
        return active;
    }

    private static String type(boolean enclosed) {
        return enclosed ? "enclosed" : "open";
    }

    /** A blaze killed by you (it drops rods: 0 or 1). */
    public static void onBlazeKilled(int rods) {
        if (!active || endTime >= 0) return;
        kills++;
        rodsDropped += rods;
    }

    static void start(Minecraft mc, boolean enclosed) {
        loadPb();
        attempts++;
        active = false;
        enclosedMode = enclosed;
        EndPractice.log("starting blaze attempt " + attempts + " (" + type(enclosed) + ")");
        mc.setWorld(null);
        mc.interactionManager = new SurvivalInteractionManager(mc);
        Room room;
        preparing = true;
        try {
            room = makeWorld(mc, enclosed);
        } finally {
            preparing = false;
        }
        mc.openScreen(null);
        PlayerEntity p = mc.player;
        EndPractice.giveKit(p, EndPractice.cfg.blazeKit);
        p.setHealth(20);
        p.getHungerManager().add(20, 20.0F);
        p.velocityX = p.velocityY = p.velocityZ = 0.0;
        p.fallDistance = 0.0F;
        active = true;
        startTime = mc.world.getTime();
        endTime = -1;
        result = null;
        kills = rodsDropped = 0;
        String keys = Config.keyName(enclosed ? EndPractice.cfg.keyBlazeEnclosed : EndPractice.cfg.keyBlazeOpen) + " restarts.";
        if (room == null) {
            roomText = "no spawner";
            EndPractice.chat(mc, "§dBlaze practice§r: no blaze spawner found near this nether's fortresses. " + keys);
            return;
        }
        boolean matches = enclosed ? room.open < ENCLOSED : room.open > OPEN;
        roomText = room.open < ENCLOSED ? "enclosed" : room.open > OPEN ? "open" : "half open";
        EndPractice.chat(mc, "§dBlaze practice§r attempt " + attempts + ": get " + EndPractice.cfg.blazeRods + " rods"
                + (EndPractice.cfg.blazeTowerCrafts ? " and the tower crafts (pressure plate, fence gate, 3 ladders, 3 nether brick fences, boat). " : ". ")
                + (matches ? "" : "§eNo " + type(enclosed) + " spawner near this seed's fortresses, so this is the closest:§r ")
                + "This is " + (roomText.startsWith("half") ? "a " : "an ") + roomText + " spawner (" + Math.round(room.open * 100) + "% of the space around the room is open). " + keys);
    }

    private static final class Room {
        int[] spawner;
        double[] stand;
        double open;
    }

    private static Room makeWorld(Minecraft mc, boolean enclosed) {
        Random random = new Random();
        Room best = null;
        for (int tries = 0; tries < 4; tries++) {
            mc.getWorldStorageSource().delete(FOLDER);
            EndPractice.resetBiomeArrayPool();
            EndPractice.skipOverworldPrep = true;
            try {
                mc.startGame(FOLDER, "Blaze Practice", Versions.newWorldSettings(random.nextLong(), true));
            } finally {
                EndPractice.skipOverworldPrep = false;
            }
            List<int[]> forts = fortresses(mc.world.getSeed());
            if (forts.isEmpty()) {
                mc.setWorld(null);
                continue;
            }
            // into the nether by vanilla's own path, arriving at the nearest fortress (overworld x 8 = nether x 1)
            int[] f = forts.get(0);
            PlayerEntity p = mc.player;
            p.setPositionAndAngles((f[0] * 16 + 8) * 8.0, 80, (f[1] * 16 + 8) * 8.0, 0, 0);
            mc.changeDimension(-1);
            World w = mc.world;
            ChunkSource gen = ((ChunkCacheAccessor) w.getChunkSource()).endpractice$getGenerator();
            if (!(gen instanceof NetherChunkGenerator)) return null;
            NetherChunkGenerator nether = (NetherChunkGenerator) gen;
            List<Room> rooms = new ArrayList<>();
            for (int i = 0; i < forts.size() && i < 6; i++) {
                int[] c = forts.get(i);
                w.getBlock(c[0] * 16 + 8, 64, c[1] * 16 + 8); // lays out the fortress
                rooms.addAll(spawnerRooms(w, nether, c[0], c[1]));
                for (Room r : rooms) {
                    if (enclosed ? r.open < ENCLOSED : r.open > OPEN) {
                        place(mc, r);
                        return r;
                    }
                }
            }
            for (Room r : rooms) {
                if (best == null || (enclosed ? r.open < best.open : r.open > best.open)) best = r;
            }
            if (best != null && tries == 3) break;
            if (best != null && (enclosed ? best.open < 0.4 : best.open > 0.35)) break;
            mc.setWorld(null);
            mc.interactionManager = new SurvivalInteractionManager(mc);
            best = null;
        }
        if (best != null) place(mc, best);
        return best;
    }

    private static void place(Minecraft mc, Room r) {
        PlayerEntity p = mc.player;
        double dx = r.spawner[0] + 0.5 - r.stand[0], dz = r.spawner[2] + 0.5 - r.stand[2];
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        double dy = r.spawner[1] + 0.5 - (r.stand[1] + 1.62);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.setPositionAndAngles(r.stand[0], r.stand[1] + p.eyeHeight + 0.1, r.stand[2], yaw, pitch);
        // a clean start: no blazes from before you got there
        for (Object o : new ArrayList<Object>(mc.world.entities)) {
            if (o instanceof BlazeEntity && ((BlazeEntity) o).distanceTo(p) < 48) ((BlazeEntity) o).remove();
        }
        EndPractice.log(String.format(Locale.ROOT, "blaze spawner %d %d %d, %.0f%% open", r.spawner[0], r.spawner[1], r.spawner[2], r.open * 100));
    }

    /** Fortress start chunks within 2 regions (512 blocks) of 0,0, nearest first, the way FortressStructure picks them. */
    static List<int[]> fortresses(long seed) {
        List<int[]> out = new ArrayList<>();
        Random r = new Random();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                r.setSeed((long) (i ^ j << 4) ^ seed);
                r.nextInt();
                if (r.nextInt(3) != 0) continue;
                out.add(new int[]{(i << 4) + 4 + r.nextInt(8), (j << 4) + 4 + r.nextInt(8)});
            }
        }
        out.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
        return out;
    }

    /** The blaze spawner rooms of the fortress starting at that chunk, generated, with how open each one is. */
    private static List<Room> spawnerRooms(World w, NetherChunkGenerator nether, int cx, int cz) {
        List<Room> rooms = new ArrayList<>();
        StructureStart start = ((StructureFeatureAccessor) nether.fortress).endpractice$getStructures().get(chunkKey(cx, cz));
        if (start == null) return rooms;
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof FortressPieces.BlazeSpawner)) continue;
            StructurePieceInvoker inv = (StructurePieceInvoker) piece;
            int sx = inv.endpractice$transformX(3, 5), sy = inv.endpractice$transformY(5), sz = inv.endpractice$transformZ(3, 5);
            StructureBox b = piece.getBounds();
            // generate (and populate) the chunks the room and its surroundings are in
            for (int x = (b.minX - 20) >> 4; x <= (b.maxX + 20) >> 4; x++) {
                for (int z = (b.minZ - 20) >> 4; z <= (b.maxZ + 20) >> 4; z++) w.getBlock(x * 16 + 8, 64, z * 16 + 8);
            }
            if (w.getBlock(sx, sy, sz) != Block.MOB_SPAWNER.id) continue;
            Room r = new Room();
            r.spawner = new int[]{sx, sy, sz};
            // the doorway, where the room joins the corridor or bridge before it, on the floor
            r.stand = new double[]{inv.endpractice$transformX(3, 0) + 0.5, inv.endpractice$transformY(2), inv.endpractice$transformZ(3, 0) + 0.5};
            r.open = openness(w, nether, b);
            rooms.add(r);
        }
        return rooms;
    }

    private static long chunkKey(int x, int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    /**
     * Share of the space around a room (a 2 block thick shell, 1 to 3 blocks out from its sides and top, leaving out
     * the rest of the fortress) that is air or lava rather than netherrack and the like.
     */
    private static double openness(World w, NetherChunkGenerator nether, StructureBox b) {
        int open = 0, all = 0;
        for (int x = b.minX - 3; x <= b.maxX + 3; x++) {
            for (int z = b.minZ - 3; z <= b.maxZ + 3; z++) {
                for (int y = b.minY; y <= b.maxY + 3; y++) {
                    boolean shell = x < b.minX - 1 || x > b.maxX + 1 || z < b.minZ - 1 || z > b.maxZ + 1 || y > b.maxY + 1;
                    if (!shell || nether.fortress.isInside(x, y, z)) continue;
                    all++;
                    int id = w.getBlock(x, y, z);
                    if (id == 0 || w.getMaterial(x, y, z) == Material.LAVA) open++;
                }
            }
        }
        return all == 0 ? 0 : (double) open / all;
    }

    static void stop() {
        active = false;
    }

    static void tick(Minecraft mc) {
        if (!active || mc.player == null || endTime >= 0) return;
        if (mc.world.dimension.id != -1 || mc.player.getHealth() <= 0) {
            finish(mc, mc.player.getHealth() <= 0 ? "You died" : "Left the nether");
            return;
        }
        if (rods(mc.player) >= EndPractice.cfg.blazeRods && missing(mc.player).isEmpty()) finish(mc, null);
    }

    /** With the tower crafts setting: what a 1.0 tower build needs from here (id, count, name). */
    private static final Object[][] TOWER_CRAFTS = {
            {new int[]{Block.WOODEN_PRESSURE_PLATE.id, Block.STONE_PRESSURE_PLATE.id}, 1, "plate"},
            {new int[]{Block.FENCE_GATE.id}, 1, "gate"},
            {new int[]{Block.LADDER.id}, 3, "ladders"},
            {new int[]{Block.NETHER_BRICK_FENCE.id}, 3, "fences"},
            {new int[]{Item.BOAT.id}, 1, "boat"}};

    /** The tower crafts still missing, as HUD text ("ladders 1/3"), empty when none are or the setting is off. */
    private static List<String> missing(PlayerEntity p) {
        List<String> out = new ArrayList<>();
        if (!EndPractice.cfg.blazeTowerCrafts) return out;
        for (Object[] c : TOWER_CRAFTS) {
            int have = count(p, (int[]) c[0]), need = (Integer) c[1];
            if (have < need) out.add(need == 1 ? (String) c[2] : c[2] + " " + have + "/" + need);
        }
        return out;
    }

    private static int count(PlayerEntity p, int[] ids) {
        int n = 0;
        for (ItemStack s : p.inventory.items) {
            if (s == null) continue;
            for (int id : ids) if (s.id == id) n += s.size;
        }
        return n;
    }

    /** The PB key: the spawner type, and "+tower" with the tower crafts setting. */
    private static String pbKey() {
        return type(enclosedMode) + (EndPractice.cfg.blazeTowerCrafts ? "+tower" : "");
    }

    private static int rods(PlayerEntity p) {
        int n = 0;
        for (ItemStack s : p.inventory.items) if (s != null && s.id == Item.BLAZE_ROD.id) n += s.size;
        return n;
    }

    private static void finish(Minecraft mc, String failure) {
        endTime = mc.world.getTime();
        long ticks = endTime - startTime;
        String drop = kills == 0 ? "" : ", " + kills + " blazes, " + Math.round(100.0F * rodsDropped / kills) + "% dropped a rod";
        if (failure != null) {
            result = failure;
            EndPractice.chat(mc, failure + " after " + EndPractice.time(ticks) + drop + ".");
            return;
        }
        String t = pbKey();
        if (Commands.cheated) {
            result = "Done " + EndPractice.time(ticks);
            EndPractice.chat(mc, "Done in " + EndPractice.time(ticks) + drop + " §7(not saved: commands were used)§r");
            return;
        }
        Long pb = pbs.get(t);
        String line = "§d" + EndPractice.cfg.blazeRods + " rods" + (EndPractice.cfg.blazeTowerCrafts ? " and tower crafts" : "") + "§r in §e" + EndPractice.time(ticks) + "§r" + drop;
        if (pb == null || ticks < pb) {
            line += pb == null ? " §6first time§r" : " §6New PB!§r was " + EndPractice.time(pb);
            pbs.put(t, ticks);
        } else {
            line += " (PB " + EndPractice.time(pb) + ")";
        }
        result = "Done " + EndPractice.time(ticks);
        EndPractice.chat(mc, line);
        save(ticks, t);
    }

    static List<String> hud(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (!active || mc.world == null || mc.player == null) return lines;
        long ticks = (endTime >= 0 ? endTime : mc.world.getTime()) - startTime;
        Long pb = pbs.get(pbKey());
        lines.add("§dBlaze§r (" + type(enclosedMode) + (EndPractice.cfg.blazeTowerCrafts ? ", tower" : "") + ")  " + EndPractice.time(ticks) + (pb != null ? "  §7PB " + EndPractice.time(pb) + "§r" : ""));
        lines.add("Rods " + rods(mc.player) + "/" + EndPractice.cfg.blazeRods + "  Blazes " + kills
                + (kills > 0 ? "  §7" + Math.round(100.0F * rodsDropped / kills) + "% drop§r" : ""));
        List<String> need = missing(mc.player);
        if (!need.isEmpty() && endTime < 0) lines.add("§7Need " + String.join(", ", need) + "§r");
        if (result != null) lines.add(result);
        return lines;
    }

    /** The Stats screen lines. */
    static List<String> pbLines() {
        loadPb();
        List<String> out = new ArrayList<>();
        Long open = pbs.get("open"), enclosed = pbs.get("enclosed");
        out.add("§dBlaze§r (" + EndPractice.cfg.blazeRods + " rods)   open " + (open != null ? "§e" + EndPractice.time(open) + "§r" : "§7none§r")
                + "   enclosed " + (enclosed != null ? "§e" + EndPractice.time(enclosed) + "§r" : "§7none§r"));
        Long openT = pbs.get("open+tower"), enclosedT = pbs.get("enclosed+tower");
        if (openT != null || enclosedT != null || EndPractice.cfg.blazeTowerCrafts) {
            out.add("with tower crafts   open " + (openT != null ? "§e" + EndPractice.time(openT) + "§r" : "§7none§r")
                    + "   enclosed " + (enclosedT != null ? "§e" + EndPractice.time(enclosedT) + "§r" : "§7none§r"));
        }
        return out;
    }

    // ---- PBs, config/endpractice-blaze.txt: time,ticks,type,rods,kills,dropped -------------------------------------

    private static File file() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-blaze.txt");
    }

    private static void save(long ticks, String type) {
        try {
            File f = file();
            f.getParentFile().mkdirs();
            String line = String.format(Locale.ROOT, "%d,%d,%s,%d,%d,%d%n", System.currentTimeMillis(), ticks, type, EndPractice.cfg.blazeRods, kills, rodsDropped);
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            EndPractice.log("couldn't save the blaze time: " + e);
        }
    }

    /** The best time per spawner type, for the current number of rods. */
    private static void loadPb() {
        pbs.clear();
        try {
            File f = file();
            if (!f.isFile()) return;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String[] p = line.split(",");
                if (p.length < 4) continue;
                try {
                    if (Integer.parseInt(p[3].trim()) != EndPractice.cfg.blazeRods) continue;
                    long t = Long.parseLong(p[1].trim());
                    String type = p[2].trim();
                    Long old = pbs.get(type);
                    if (old == null || t < old) pbs.put(type, t);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception e) {
            EndPractice.log("couldn't read the blaze times: " + e);
        }
    }
}
