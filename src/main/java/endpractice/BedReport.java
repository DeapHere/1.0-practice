package endpractice;

import endpractice.mixin.MobEntityAccessor;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.monster.boss.EnderDragonEntity;
import net.minecraft.entity.mob.monster.boss.EnderDragonPart;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * How close each bed was to a perfect one.
 *
 * In 1.0 (and 1.7, the code is the same) an explosion of power 5 (a bed) damages every dragon part within 10 blocks
 * by distance from the explosion to the part's position, which is the bottom center of its hitbox. The head part is
 * a 3x3x3 box 5.5 blocks in front of the dragon's center and takes the full damage: (q*q + q) / 2 * 80 + 1 with
 * q = (1 - distance / 10) * exposure, so 81 at most. Every other part takes a quarter of that plus 1.
 *
 * The dragon's head position is recorded every tick, so after a bed goes off this can look at where the head was in
 * the ticks around it and say which tick would have been the best one.
 */
public final class BedReport {
    private static final int HISTORY = 64;
    /** Ticks either side of the bed that are looked at for the best moment. */
    private static final int WINDOW = 10;
    private static final double MAX_DIST = 10.0; // power 5, doubled by Explosion before it damages entities

    private static final double[][] head = new double[HISTORY][3];
    private static final float[] yaw = new float[HISTORY];
    private static final boolean[] restricted = new boolean[HISTORY];
    private static final double[] speed = new double[HISTORY];
    private static long recorded = 0; // number of ticks recorded

    // the beds waiting to be reported on, oldest first
    private static final java.util.ArrayList<Pending> pendings = new java.util.ArrayList<>();
    /** The last bed reported, for pairs. */
    private static Pending prev;
    private static String lastShort;

    private static final class Pending {
        double x, y, z;
        long index; // recorded - 1 when the bed went off: the head position it hit
        int healthBefore;
        int healthAfter;
        boolean invulnerable;
        int invulnerableFor;
        int prevDamage;
        String bestPart;
        int bestPartDamage;
        boolean done;
        boolean inGround;
        double speed, headUp;
    }

    private BedReport() {
    }

    static void clear() {
        recorded = 0;
        pendings.clear();
        prev = null;
        DoubleBed.clear();
        lastShort = null;
    }

    static String lastShort() {
        return lastShort;
    }

    /** After every dragon tick, from EnderDragonMixin. */
    public static void recordDragon(EnderDragonEntity dragon) {
        if (!EndPractice.endFight() || dragon.world != EndPractice.mc.world) return;
        int i = (int) (recorded % HISTORY);
        head[i][0] = dragon.head.x;
        head[i][1] = dragon.head.y;
        head[i][2] = dragon.head.z;
        yaw[i] = dragon.yaw;
        restricted[i] = dragon.restrictMovement;
        double vx = dragon.x - dragon.prevX, vy = dragon.y - dragon.prevY, vz = dragon.z - dragon.prevZ;
        speed[i] = Math.sqrt(vx * vx + vy * vy + vz * vz);
        recorded++;
    }

    /** Explosion.damageEntities, before anything is damaged. */
    public static void beforeExplosion(World world, double x, double y, double z, float power) {
        Minecraft mc = EndPractice.mc;
        if (power != 5.0F || !EndPractice.endFight() || !EndPractice.cfg.bedReport) return;
        if (world != mc.world || world.dimension.id != 1 || recorded == 0) return;
        EnderDragonEntity dragon = EndPractice.dragon(mc);
        if (dragon == null || dragon.getHealth() <= 0) return;
        if (dragon.head.distanceTo(x, y, z) > EndPractice.cfg.bedReportRange) return;

        Pending p = new Pending();
        p.x = x;
        p.y = y;
        p.z = z;
        p.index = recorded - 1;
        p.healthBefore = dragon.getHealth();
        MobEntityAccessor acc = (MobEntityAccessor) dragon;
        p.invulnerable = dragon.invulnerableTimer > dragon.invulnerableTicks / 2.0F;
        p.invulnerableFor = dragon.invulnerableTimer - dragon.invulnerableTicks / 2;
        p.prevDamage = acc.endpractice$getPrevDamageTaken();
        for (long t = Math.max(0, recorded - 5); t < recorded; t++) p.inGround |= restricted[(int) (t % HISTORY)];
        p.speed = speed[(int) ((recorded - 1) % HISTORY)];
        p.headUp = dragon.head.y - (mc.player.y - mc.player.eyeHeight);
        DoubleBed.onHit(mc, p.invulnerable);

        // what each part would take from this explosion, worked out the way Explosion does it
        Vec3d center = Vec3d.fromPool(x, y, z);
        p.bestPartDamage = 0;
        for (Entity e : dragon.getParts()) {
            EnderDragonPart part = (EnderDragonPart) e;
            double d = part.distanceTo(x, y, z) / MAX_DIST;
            if (d > 1.0) continue;
            double q = (1.0 - d) * world.getBlockDensity(center, part.shape);
            int dmg = (int) ((q * q + q) / 2.0 * 8.0 * MAX_DIST + 1.0);
            if (part != dragon.head) dmg = dmg / 4 + 1;
            if (dmg > p.bestPartDamage) {
                p.bestPartDamage = dmg;
                p.bestPart = part == dragon.head ? "head" : part.name;
            }
        }
        pendings.add(p);
    }

    /** Explosion.damageEntities, after. */
    public static void afterExplosion(World world, float power) {
        Pending p = pendings.isEmpty() ? null : pendings.get(pendings.size() - 1);
        if (p == null || p.done || power != 5.0F) return;
        EnderDragonEntity dragon = EndPractice.dragon(EndPractice.mc);
        p.healthAfter = dragon == null ? p.healthBefore : Math.max(0, dragon.getHealth());
        p.done = true;
        DoubleBed.dealt(p.healthBefore - p.healthAfter);
        EndPractice.onBed(p.healthBefore - p.healthAfter);
    }

    /** Every client tick in the End: reports a bed once the ticks after it have been recorded. */
    static void tick(Minecraft mc) {
        while (!pendings.isEmpty()) {
            Pending p = pendings.get(0);
            if (!p.done) return;
            boolean dragonDead = p.healthAfter <= 0;
            if (recorded - 1 < p.index + WINDOW && !dragonDead) return;
            pendings.remove(0);
            report(mc, p, dragonDead);
        }
    }

    private static void report(Minecraft mc, Pending p, boolean dragonDead) {
        int dealt = p.healthBefore - p.healthAfter;
        int gap = prev == null ? Integer.MAX_VALUE : (int) (p.index - prev.index);
        int prevDealt = prev == null ? 0 : prev.healthBefore - prev.healthAfter;
        boolean inPair = gap <= DoubleBed.PAIR_TICKS && prevDealt > 0 && (dealt > 0 || p.invulnerable);
        int at = (int) (p.index % HISTORY);
        double[] h = head[at];
        double dist = dist(h, p);

        // offset of the bed from the head's damage point, in the dragon's own directions
        float r = yaw[at] * (float) Math.PI / 180.0F;
        double fx = MathHelper.sin(r), fz = -MathHelper.cos(r); // the way the dragon flies
        double vx = h[0] - p.x, vy = h[1] - p.y, vz = h[2] - p.z;  // bed to head
        double ahead = vx * fx + vz * fz;      // > 0: the head was in front of the bed, so the bed was behind it
        double right = vx * -fz + vz * fx;     // > 0: the head was to the dragon's right of the bed

        StringBuilder sb = new StringBuilder("\u00a7eBed\u00a7r ");
        if (dealt <= 0 && p.bestPartDamage <= 0) {
            sb.append("missed, head ").append(f1(dist)).append(" away");
        } else {
            sb.append(dragonDead ? p.bestPartDamage : dealt).append("/81");
            if (p.bestPart != null && !"head".equals(p.bestPart)) sb.append(", only hit the ").append(p.bestPart);
            if (dist < 0.05) {
                sb.append(", right on the head");
            } else {
                sb.append(", ").append(f1(dist)).append(" from head:");
                String dirs = dir(ahead, "behind", "in front") + dir(vy, "below", "above") + dir(right, "left", "right");
                sb.append(dirs.isEmpty() ? " close" : dirs.substring(0, dirs.length() - 1));
            }
        }
        EndPractice.chat(mc, sb.toString());

        if (p.invulnerable && !inPair) {
            EndPractice.chat(mc, "Dragon was invulnerable for " + f2(p.invulnerableFor / 20.0) + " s more: only damage over "
                    + p.prevDamage + " counted");
        }

        // the best tick around the bed
        long first = Math.max(p.index - WINDOW, recorded - HISTORY);
        long last = Math.min(p.index + WINDOW, recorded - 1);
        long best = p.index;
        double bestDist = dist;
        for (long t = Math.max(first, 0); t <= last; t++) {
            double d = dist(head[(int) (t % HISTORY)], p);
            if (d < bestDist - 1e-9) {
                bestDist = d;
                best = t;
            }
        }
        int off = (int) (best - p.index);
        int then = headDamage(bestDist);
        // off > 0: the best tick came after the bed, so the click was early
        int n = Math.abs(off);
        String when = n + (n == 1 ? " tick " : " ticks ") + (off > 0 ? "early" : "late");
        int gain = Math.max(0, then - Math.max(headDamage(dist), dealt));
        String s;
        if (off == 0) {
            s = "\u00a7aPerfect tick\u00a7r";
        } else {
            s = (n == 1 ? "\u00a7aGood\u00a7r, " : "\u00a7e") + when + (n == 1 ? "" : "\u00a7r") + " (" + f2(n / 20.0) + " s)";
            s += gain > 0 ? ": the perfect tick does up to " + then + " (+" + gain + ")" : "";
            if (off > 0 && dealt > 0) s += ", estimate";
        }
        EndPractice.chat(mc, s);
        if (inPair) {
            DoubleBed.pair(mc, gap, prevDealt, Math.max(0, dealt), dragonDead ? Math.max(0, dealt) : p.bestPartDamage, p.invulnerable, p.prevDamage);
        } else if (dealt > 0 && !p.invulnerable && !dragonDead) {
            EndPractice.chat(mc, DoubleBed.chargeLine(p.inGround, p.speed, p.headUp));
        }
        if (dealt > 0 || p.invulnerable) prev = p;
        lastShort = "Last bed " + (dragonDead ? p.bestPartDamage : Math.max(0, dealt)) + (off == 0 ? ", perfect tick" : ", " + when);
    }

    /** Most a bed does to the head at this distance from its damage point, with nothing in the way. */
    static int headDamage(double dist) {
        double d = dist / MAX_DIST;
        if (d > 1.0) return 0;
        double q = 1.0 - d;
        return (int) ((q * q + q) / 2.0 * 8.0 * MAX_DIST + 1.0);
    }

    private static double dist(double[] h, Pending p) {
        double dx = h[0] - p.x, dy = h[1] - p.y, dz = h[2] - p.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** " 1.3 behind,": how far the bed was from the head's damage point along one direction. */
    private static String dir(double headMinusBed, String bedWhenPositive, String bedWhenNegative) {
        double a = Math.abs(headMinusBed);
        if (a < 0.25) return "";
        return " " + f1(a) + " " + (headMinusBed > 0 ? bedWhenPositive : bedWhenNegative) + ",";
    }

    private static String f1(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    private static String f2(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }
}
