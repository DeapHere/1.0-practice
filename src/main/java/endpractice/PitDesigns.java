package endpractice;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

/**
 * Saved pit designs: the blocks in a box, relative to where you stood when saving, in config/endpractice-pits/.
 * /pit pos1 and /pit pos2 pick the box (or it's 9x9 round you, from 44 below to 6 above), /pit save, /pit load
 * pastes it where you stand, /pit list and /pit delete.
 */
public final class PitDesigns {
    private PitDesigns() {
    }

    /** The box corners, or null. */
    static int[] pos1, pos2;
    /** The design last loaded or saved here: /test with no name uses it. */
    static String current;

    private static File dir() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-pits");
    }

    private static String clean(String name) {
        return name.trim().replaceAll("[^A-Za-z0-9 _.-]", "").replace(' ', '_');
    }

    private static int[] feet(PlayerEntity p) {
        return new int[]{(int) Math.floor(p.x), (int) Math.floor(p.y - p.eyeHeight + 0.01), (int) Math.floor(p.z)};
    }

    static void command(Minecraft mc, String[] a) {
        PlayerEntity p = mc.player;
        String sub = a.length >= 2 ? a[1].toLowerCase(Locale.ROOT) : "";
        String name = a.length >= 3 ? String.join(" ", Arrays.copyOfRange(a, 2, a.length)) : null;
        switch (sub) {
            case "pos1": case "1":
                pos1 = feet(p);
                pos1[1]--; // the block you stand on
                EndPractice.chat(mc, "Corner 1 at " + pos1[0] + " " + pos1[1] + " " + pos1[2] + ".");
                return;
            case "pos2": case "2":
                pos2 = feet(p);
                pos2[1]--;
                EndPractice.chat(mc, "Corner 2 at " + pos2[0] + " " + pos2[1] + " " + pos2[2] + ".");
                return;
            case "clear":
                pos1 = pos2 = null;
                EndPractice.chat(mc, "Box cleared: /pit save takes 9x9 round you, 44 down to 6 up.");
                return;
            case "save":
                if (name == null) {
                    EndPractice.chat(mc, "§c/pit save <name>§r, standing where you'd stand in the pit");
                    return;
                }
                save(mc, name);
                return;
            case "load": case "paste":
                if (name == null) {
                    EndPractice.chat(mc, "§c/pit load <name>§r pastes it where you stand. /pit list shows them.");
                    return;
                }
                load(mc, name);
                return;
            case "list":
                list(mc);
                return;
            case "delete": case "remove":
                if (name == null) {
                    EndPractice.chat(mc, "§c/pit delete <name>");
                    return;
                }
                File f = new File(dir(), clean(name) + ".txt");
                EndPractice.chat(mc, f.delete() ? "Deleted " + clean(name) + "." : "§cNo design called " + clean(name) + ".");
                return;
            case "plan":
                PitPlanner.plan(mc, (int) Math.floor(p.x), (int) Math.floor(p.z), false);
                EndPractice.chat(mc, "Planning the pit from here...");
                return;
            default:
                EndPractice.chat(mc, "§d/pit§r save <name>, load <name>, list, delete <name>, pos1, pos2, clear (the box to save), plan (pit planner from here)");
        }
    }

    /** The box to save: pos1/pos2 if both are set, else 9x9 round you from 44 below your feet to 6 above. */
    private static int[] box(PlayerEntity p) {
        if (pos1 != null && pos2 != null) {
            return new int[]{Math.min(pos1[0], pos2[0]), Math.min(pos1[1], pos2[1]), Math.min(pos1[2], pos2[2]),
                    Math.max(pos1[0], pos2[0]), Math.max(pos1[1], pos2[1]), Math.max(pos1[2], pos2[2])};
        }
        int[] f = feet(p);
        return new int[]{f[0] - 4, Math.max(0, f[1] - 44), f[2] - 4, f[0] + 4, Math.min(127, f[1] + 6), f[2] + 4};
    }

    static void save(Minecraft mc, String name) {
        World w = mc.world;
        int[] b = box(mc.player), f = feet(mc.player);
        EndPractice.log("pit save at feet " + f[0] + " " + f[1] + " " + f[2]);
        int sx = b[3] - b[0] + 1, sy = b[4] - b[1] + 1, sz = b[5] - b[2] + 1;
        if ((long) sx * sy * sz > 200000) {
            EndPractice.chat(mc, "§cThat box is " + sx + "x" + sy + "x" + sz + ", too big (200,000 blocks at most).");
            return;
        }
        StringBuilder sb = new StringBuilder();
        // header: size, the box's corner relative to where you stood, and where you looked
        sb.append("oldschoolprac-pit 1\n");
        sb.append(sx).append(' ').append(sy).append(' ').append(sz).append(' ')
                .append(b[0] - f[0]).append(' ').append(b[1] - f[1]).append(' ').append(b[2] - f[2]).append(' ')
                .append(Math.round(mc.player.yaw)).append('\n');
        // blocks x, then z, then y (bottom up), as id[:meta], runs of the same block as n*id[:meta]
        String last = null;
        int run = 0;
        int solid = 0;
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    int id = w.getBlock(b[0] + x, b[1] + y, b[2] + z);
                    int meta = id == 0 ? 0 : w.getBlockMetadata(b[0] + x, b[1] + y, b[2] + z);
                    if (id != 0) solid++;
                    String s = meta == 0 ? String.valueOf(id) : id + ":" + meta;
                    if (s.equals(last)) {
                        run++;
                    } else {
                        if (last != null) sb.append(run > 1 ? run + "*" + last : last).append(',');
                        last = s;
                        run = 1;
                    }
                }
            }
        }
        if (last != null) sb.append(run > 1 ? run + "*" + last : last);
        sb.append('\n');
        try {
            File d = dir();
            d.mkdirs();
            File out = new File(d, clean(name) + ".txt");
            boolean existed = out.exists();
            Files.write(out.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
            current = clean(name);
            EndPractice.chat(mc, (existed ? "Replaced" : "Saved") + " pit design §e" + clean(name) + "§r: " + sx + "x" + sy + "x" + sz + ", " + solid
                    + " blocks, relative to where you stand. /pit load " + clean(name) + " pastes it.");
        } catch (Exception e) {
            EndPractice.chat(mc, "§cCouldn't save the design: " + e.getMessage());
        }
    }

    static void load(Minecraft mc, String name) {
        File in = new File(dir(), clean(name) + ".txt");
        if (!in.exists()) {
            EndPractice.chat(mc, "§cNo design called " + clean(name) + ".§r /pit list shows them.");
            return;
        }
        try {
            List<String> lines = Files.readAllLines(in.toPath(), StandardCharsets.UTF_8);
            String[] h = lines.get(1).trim().split(" ");
            int sx = Integer.parseInt(h[0]), sy = Integer.parseInt(h[1]), sz = Integer.parseInt(h[2]);
            int ox = Integer.parseInt(h[3]), oy = Integer.parseInt(h[4]), oz = Integer.parseInt(h[5]);
            float yaw = h.length > 6 ? Float.parseFloat(h[6]) : mc.player.yaw;
            int[] ids = new int[sx * sy * sz], metas = new int[ids.length];
            int i = 0;
            for (String tok : lines.get(2).trim().split(",")) {
                if (tok.isEmpty()) continue;
                int n = 1;
                int star = tok.indexOf('*');
                if (star >= 0) {
                    n = Integer.parseInt(tok.substring(0, star));
                    tok = tok.substring(star + 1);
                }
                int colon = tok.indexOf(':');
                int id = Integer.parseInt(colon >= 0 ? tok.substring(0, colon) : tok);
                int meta = colon >= 0 ? Integer.parseInt(tok.substring(colon + 1)) : 0;
                for (int k = 0; k < n && i < ids.length; k++, i++) {
                    ids[i] = id;
                    metas[i] = meta;
                }
            }
            World w = mc.world;
            int[] f = feet(mc.player);
            int bx = f[0] + ox, by = f[1] + oy, bz = f[2] + oz;
            EndPractice.log("pit load " + current + " at feet " + f[0] + " " + f[1] + " " + f[2]);
            // solid blocks and air first, then the ones that hang on others (torches, ladders, water, plants)
            for (int pass = 0; pass < 2; pass++) {
                int j = 0;
                for (int y = 0; y < sy; y++) {
                    for (int z = 0; z < sz; z++) {
                        for (int x = 0; x < sx; x++, j++) {
                            int id = ids[j];
                            Block block = id > 0 && id < Block.BY_ID.length ? Block.BY_ID[id] : null;
                            if (id != 0 && block == null) continue;
                            boolean first = id == 0 || block.isCube();
                            if (first != (pass == 0)) continue;
                            int y2 = by + y;
                            if (y2 < 1 || y2 > 127) continue;
                            w.setBlockWithMetadata(bx + x, y2, bz + z, id, metas[j]);
                        }
                    }
                }
            }
            current = clean(name);
            // stand where the design was saved from, looking the same way
            mc.player.setPositionAndAngles(f[0] + 0.5, f[1] + 0.01, f[2] + 0.5, yaw, mc.player.pitch); // it adds the eye height itself
            mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0.0;
            mc.player.fallDistance = 0.0F;
            EndPractice.chat(mc, "Loaded pit design §e" + current + "§r here (" + sx + "x" + sy + "x" + sz + "). /test with no name tests it as " + current + ".");
        } catch (Exception e) {
            EndPractice.chat(mc, "§cCouldn't load " + clean(name) + ": " + e);
        }
    }

    /** Saved design names, sorted. */
    static List<String> names() {
        File[] fs = dir().listFiles((d, n) -> n.endsWith(".txt"));
        List<String> names = new ArrayList<>();
        if (fs == null) return names;
        Arrays.sort(fs);
        for (File f : fs) names.add(f.getName().substring(0, f.getName().length() - 4));
        return names;
    }

    static boolean delete(String name) {
        return new File(dir(), clean(name) + ".txt").delete();
    }

    private static void list(Minecraft mc) {
        File[] fs = dir().listFiles((d, n) -> n.endsWith(".txt"));
        if (fs == null || fs.length == 0) {
            EndPractice.chat(mc, "No pit designs saved yet. /pit save <name>, standing in the pit.");
            return;
        }
        Arrays.sort(fs);
        List<String> names = new ArrayList<>();
        for (File f : fs) names.add(f.getName().substring(0, f.getName().length() - 4));
        EndPractice.chat(mc, "§dPit designs:§r " + String.join(", ", names) + ". /pit load <name>");
    }

    /** The box /pit save would take, as a white outline, while a corner is set. */
    static void render(Minecraft mc, double cx, double cy, double cz) {
        if (pos1 == null && pos2 == null) return;
        int[] b = box(mc.player);
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.8F);
        double x0 = b[0] - cx, y0 = b[1] - cy, z0 = b[2] - cz, x1 = b[3] + 1 - cx, y1 = b[4] + 1 - cy, z1 = b[5] + 1 - cz;
        GL11.glBegin(GL11.GL_LINES);
        double[][] e = {{x0, y0, z0, x1, y0, z0}, {x0, y1, z0, x1, y1, z0}, {x0, y0, z1, x1, y0, z1}, {x0, y1, z1, x1, y1, z1},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0}, {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1}, {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1}};
        for (double[] l : e) {
            GL11.glVertex3d(l[0], l[1], l[2]);
            GL11.glVertex3d(l[3], l[4], l[5]);
        }
        GL11.glEnd();
    }
}
