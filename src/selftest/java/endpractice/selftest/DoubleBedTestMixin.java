package endpractice.selftest;

import endpractice.EndPractice;
import endpractice.mixin.EnderDragonAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.monster.boss.EnderDragonEntity;
import net.minecraft.entity.mob.monster.boss.EnderDragonPart;
import net.minecraft.entity.EnderCrystalEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * -Dendpractice.doublebed=N: N charges at each height. The dragon is sent at a player standing still from 64 blocks
 * out and H up. Bed 1 goes off in a hole one block in front of the player at the tick the head is closest, then for
 * 40 ticks this works out what a second bed behind the player would do, at ground level or in a hole, 1 to 3 blocks
 * back, before and after the dragon's 10 ticks of invulnerability.
 */
@Mixin(Minecraft.class)
public abstract class DoubleBedTestMixin {
    private static final int TRIALS = Integer.getInteger("endpractice.doublebed", 0);
    private static final int[] HEIGHTS = {15, 15, 20, 25, 15, 25, 35};
    private static final int[] DISTS = {64, 96, 96, 96, 128, 128, 128};
    private static final double[] V0 = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
    private static int startD;
    private static int dbstep, dbwait, trial, tick, feetY, px, pz;
    private static double dirX, dirZ, prevD, startH;
    private static int dealt1;
    private static double[] hy = new double[20];
    private static int[] best = new int[12], bestK = new int[12], early = new int[12];
    private static double minHeadY;
    private static int tick1;
    private static final java.util.Random rand = new java.util.Random(1);
    private static final java.util.List<String> summary = new java.util.ArrayList<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void doubleBedTest(CallbackInfo ci) {
        if (TRIALS == 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        if (TRIALS == -2) {
            mode(mc);
            return;
        }
        if (TRIALS < 0) {
            feature(mc);
            return;
        }
        dbwait++;
        if (dbstep == 0) {
            if (dbwait < 40 || !(mc.screen instanceof TitleScreen)) return;
            EndPractice.queueStart();
            dbstep = 1;
            dbwait = 0;
            return;
        }
        if (mc.world == null || mc.player == null || mc.world.dimension.id != 1) return;
        EnderDragonEntity d = EndPractice.dragon(mc);
        if (d == null) return;
        for (Object o : mc.world.entities) if (o instanceof EnderCrystalEntity) ((Entity) o).remove();
        d.connectedCrystal = null;
        mc.player.abilities.invulnerable = true;
        if (dbstep == 1) {
            if (dbwait < 40) return;
            // a flat end stone spot away from the pillars
            px = 40;
            pz = 0;
            feetY = mc.world.getHeight(px, pz);
            log("standing at " + px + " " + feetY + " " + pz + " on block " + mc.world.getBlock(px, feetY - 1, pz));
            dbstep = 2;
            startTrial(mc, d);
            return;
        }
        // keep the player still
        mc.player.setPosition(px + 0.5, feetY + mc.player.eyeHeight, pz + 0.5);
        mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
        tick++;
        if (dbstep == 2) { // incoming: bed 1 at the spot within reach and the tick that does the most
            hy[tick % 20] = d.head.y - feetY;
            if (tick % 10 == 0 && false) {
                double hx = d.head.x - (px + 0.5), hz = d.head.z - (pz + 0.5);
                log(String.format("  H=%d D=%d t=%d head out %.1f side %.1f up %.1f speed %.2f vy %.2f tgtPlayer %s ground %s", (int) startH, startD, tick,
                        hx * dirX + hz * dirZ, hx * -dirZ + hz * dirX, d.head.y - feetY, Math.hypot(d.x - d.prevX, d.z - d.prevZ), d.y - d.prevY,
                        ((EnderDragonAccessor) d).endpractice$getTarget() == mc.player, d.restrictMovement));
            }
            double mx = d.x - d.prevX, my = d.y - d.prevY, mz = d.z - d.prevZ;
            int now = bestSpot(d.head.x, d.head.y, d.head.z), nxt = bestSpot(d.head.x + mx, d.head.y + my, d.head.z + mz);
            double nowD = headDist(now, d.head.x, d.head.y, d.head.z), nxtD = headDist(nxt, d.head.x + mx, d.head.y + my, d.head.z + mz);
            if (nowD < 8 && nxtD >= nowD) {
                double[] b = spot(now);
                if (b[1] < feetY) { mc.world.setBlock((int) Math.floor(b[0]), feetY - 1, (int) Math.floor(b[2]), 0); dug.add(new int[]{(int) Math.floor(b[0]), (int) Math.floor(b[2])}); }
                int before = d.getHealth();
                mc.world.explode(null, b[0], b[1], b[2], 5.0F, true);
                dealt1 = before - d.getHealth();
                log(String.format("H=%d bed1 at tick %d (%s %.0f,%.0f): head %.1f away, %.1f above feet, dealt %d, vy %.2f speed %.2f", (int) startH, tick,
                        b[1] < feetY ? "hole" : "ground", b[0] - px - 0.5, b[2] - pz - 0.5, nowD, d.head.y - feetY, dealt1, d.y - d.prevY, Math.hypot(d.x - d.prevX, d.z - d.prevZ)));
                dbstep = 3;
                tick1 = tick;
                tick = 0;
                minHeadY = 999;
                java.util.Arrays.fill(best, 0);
                java.util.Arrays.fill(early, 0);
                return;
            }
            if (tick > 200) {
                log("H=" + (int) startH + " no pass: dragon went off");
                summary.add((int) startH + " nopass");
                next(mc, d);
            }
            return;
        }
        if (dbstep == 3) { // after bed 1: the best bed 2 within reach, each tick
            minHeadY = Math.min(minHeadY, d.head.y - feetY);
            if (false) {
                double hx = d.head.x - (px + 0.5), hz = d.head.z - (pz + 0.5);
                log(String.format("  k=%d head along %.1f side %.1f up %.1f speed %.2f vy %.2f restricted %s", tick,
                        -(hx * dirX + hz * dirZ), hx * -dirZ + hz * dirX, d.head.y - feetY,
                        Math.hypot(d.x - d.prevX, d.z - d.prevZ), d.y - d.prevY, d.restrictMovement));
            }
            int sp = bestSpot(d.head.x, d.head.y, d.head.z);
            double[] b = spot(sp);
            int bx = (int) Math.floor(b[0]), bz = (int) Math.floor(b[2]);
            boolean hole = b[1] < feetY;
            int was = hole ? mc.world.getBlock(bx, feetY - 1, bz) : 0;
            if (hole) mc.world.setBlock(bx, feetY - 1, bz, 0);
            int dmg = damage(mc, d, b[0], b[1], b[2]);
            if (hole) mc.world.setBlock(bx, feetY - 1, bz, was);
            int c = tick >= 10 ? 0 : 1;
            if (c == 0 && dmg > best[0]) {
                best[0] = dmg;
                bestK[0] = tick;
                bestWhere = String.format("%s %d,%d", hole ? "hole" : "ground", bx - px, bz - pz);
            }
            if (c == 1) early[0] = Math.max(early[0], dmg);
            if (tick >= 40) {
                log(String.format("D=" + startD + " v=" + V0[trial / TRIALS] + " H=%d RESULT bed1 %d; head 10 ticks before bed1 %.1f up; bed2 (10+ ticks later) %d at tick %d, %s; best bed2 before 10 ticks %d (only %d over bed1 would count)",
                        (int) startH, dealt1, hy[(tick1 + 10) % 20], best[0], bestK[0], bestWhere, early[0], Math.max(0, early[0] - dealt1)));
                summary.add(startD + "/" + (int) startH + ":" + dealt1 + "+" + best[0] + "@" + bestK[0]);
                next(mc, d);
            }
        }
    }

    private static final java.util.List<int[]> dug = new java.util.ArrayList<>();
    private static String bestWhere = "";
    private static final int[][] SPOTS;
    static {
        java.util.List<int[]> l = new java.util.ArrayList<>();
        for (int dx = -4; dx <= 4; dx++)
            for (int dz = -4; dz <= 4; dz++)
                if (dx * dx + dz * dz <= 16 && (dx != 0 || dz != 0)) {
                    l.add(new int[]{dx, dz, 0});
                    l.add(new int[]{dx, dz, 1});
                }
        SPOTS = l.toArray(new int[0][]);
    }

    /** Where a bed in spot i explodes: ground level or in a hole one down. */
    private static double[] spot(int i) {
        int[] s = SPOTS[i];
        return new double[]{px + s[0] + 0.5, s[2] == 1 ? feetY - 0.5 : feetY + 0.5, pz + s[1] + 0.5};
    }

    private static double headDist(int i, double x, double y, double z) {
        double[] b = spot(i);
        return Math.sqrt((b[0] - x) * (b[0] - x) + (b[1] - y) * (b[1] - y) + (b[2] - z) * (b[2] - z));
    }

    private static int bestSpot(double x, double y, double z) {
        int best = 0;
        for (int i = 1; i < SPOTS.length; i++) if (headDist(i, x, y, z) < headDist(best, x, y, z)) best = i;
        return best;
    }

    private static int fstep, fwait, gapWanted, bed1Tick;

    /** -Dendpractice.doublebed=-1: the real thing, /charge from 50 up, a double 12 ticks apart, then one 5 apart. */
    private static void feature(Minecraft mc) {
        fwait++;
        if (fstep == 0) {
            if (fwait < 40 || !(mc.screen instanceof TitleScreen)) return;
            EndPractice.queueStart();
            fstep = 1;
            fwait = 0;
            return;
        }
        if (mc.world == null || mc.player == null || mc.world.dimension.id != 1) return;
        EnderDragonEntity d = EndPractice.dragon(mc);
        if (d == null) return;
        for (Object o : mc.world.entities) if (o instanceof EnderCrystalEntity) ((Entity) o).remove();
        d.connectedCrystal = null;
        mc.player.abilities.invulnerable = true;
        if (fstep == 1) {
            if (fwait < 40) return;
            px = 40;
            pz = 0;
            feetY = mc.world.getHeight(px, pz);
            gapWanted = 12;
            fstep = 2;
            startCharge(mc);
            return;
        }
        mc.player.setPositionAndAngles(px + 0.5, feetY + 0.01, pz + 0.5, -90.0F, 0.0F);
        mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
        tick++;
        if (fstep == 2) {
            double mx = d.x - d.prevX, my = d.y - d.prevY, mz = d.z - d.prevZ;
            int now = bestSpot(d.head.x, d.head.y, d.head.z), nxt = bestSpot(d.head.x + mx, d.head.y + my, d.head.z + mz);
            double nowD = headDist(now, d.head.x, d.head.y, d.head.z), nxtD = headDist(nxt, d.head.x + mx, d.head.y + my, d.head.z + mz);
            if (nowD < 8 && nxtD >= nowD) {
                bed(mc, now);
                log("bed 1 at tick " + tick + ", dragon " + d.getHealth());
                bed1Tick = tick;
                fstep = 3;
            } else if (tick > 300) {
                log("no pass, charging again");
                startCharge(mc);
            }
            return;
        }
        if (fstep == 3) {
            if (tick - bed1Tick <= 12) log("  hud after " + (tick - bed1Tick) + ": " + EndPractice.hud(mc));
            if (tick - bed1Tick == 3) System.out.println("SHOT1");
            if (tick - bed1Tick == gapWanted) {
                bed(mc, bestSpot(d.head.x, d.head.y, d.head.z));
                log("bed 2 " + gapWanted + " ticks later, dragon " + d.getHealth());
                fstep = 4;
                fwait = 0;
            }
            return;
        }
        if (fstep == 4 && fwait > 60) {
            log("hud: " + EndPractice.hud(mc));
            if (gapWanted == 12) {
                gapWanted = 5;
                d.setHealth(200);
                fstep = 2;
                startCharge(mc);
            } else {
                log("DONE");
                System.exit(0);
            }
        }
    }

    private static int mrep;
    private static Object w0;
    private static final int[] GAPS = {12, 5, -1, 11, 12};

    /** -Dendpractice.doublebed=-2: the double bed mode from its title screen key, four reps. */
    private static void mode(Minecraft mc) {
        fwait++;
        if (fstep == 0) {
            if (fwait < 40 || !(mc.screen instanceof TitleScreen)) return;
            EndPractice.queueDoubleBeds();
            fstep = 1;
            fwait = 0;
            return;
        }
        if (mc.world == null || mc.player == null || mc.world.dimension.id != 1 || !endpractice.DoubleBed.isActive()) return;
        EnderDragonEntity d = EndPractice.dragon(mc);
        if (d == null) return;
        if (fstep == 1) {
            px = (int) Math.floor(mc.player.x);
            pz = (int) Math.floor(mc.player.z);
            feetY = (int) Math.floor(mc.player.y - mc.player.eyeHeight + 0.05);
            w0 = mc.world;
            log("mode started, standing at " + px + " " + feetY + " " + pz + " facing " + (int) mc.player.yaw + ", flat: " + flat(mc) + ", hud " + EndPractice.hud(mc)
                    + ", hotbar " + mc.player.inventory.items[0].id + " " + mc.player.inventory.items[1].id + "x" + mc.player.inventory.items[1].size + " " + mc.player.inventory.items[2].id);
            net.minecraft.entity.ItemEntity it = new net.minecraft.entity.ItemEntity(mc.world, mc.player.x, mc.player.y - 1, mc.player.z, new net.minecraft.item.ItemStack(121, 3, 0));
            mc.world.addEntity(it);
            mc.player.inventory.items[20] = new net.minecraft.item.ItemStack(121, 5, 0); // stray end stone
            mc.player.inventory.items[1].size = 10; // used cobblestone
            log("dropped end stone at my feet, put 5 in slot 20, cobble down to 10");
            fstep = 2;
            tick = 0;
            return;
        }
        if (tick == 30 && mrep == 0) log("end stone item entities left: " + countItems(mc) + ", end stone in inventory: " + countInv(mc));
        if (Math.hypot(mc.player.x - px - 0.5, mc.player.z - pz - 0.5) > 3) {
            px = (int) Math.floor(mc.player.x);
            pz = (int) Math.floor(mc.player.z);
            feetY = (int) Math.floor(mc.player.y - mc.player.eyeHeight + 0.05);
            log("moved to " + px + " " + feetY + " " + pz + " facing " + (int) mc.player.yaw + ", flat: " + flat(mc));
            dug.clear();
        }
        mc.player.setPositionAndAngles(px + 0.5, feetY + 0.01, pz + 0.5, mc.player.yaw, 0.0F);
        mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
        tick++;
        if (tick % 20 == 0 && fstep == 2) log("  hud: " + EndPractice.hud(mc));
        if (fstep == 2) {
            if (((EnderDragonAccessor) d).endpractice$getTarget() != mc.player && d.head.distanceTo(px, feetY, pz) > 12) return;
            double mx = d.x - d.prevX, my = d.y - d.prevY, mz = d.z - d.prevZ;
            int now = bestSpot(d.head.x, d.head.y, d.head.z), nxt = bestSpot(d.head.x + mx, d.head.y + my, d.head.z + mz);
            double nowD = headDist(now, d.head.x, d.head.y, d.head.z), nxtD = headDist(nxt, d.head.x + mx, d.head.y + my, d.head.z + mz);
            if (nowD < 8 && nxtD >= nowD) {
                bed(mc, now);
                log("rep " + (mrep + 1) + " bed 1, dragon " + d.getHealth() + ", my health before " + mc.player.getHealth());
                bed1Tick = tick;
                fstep = 3;
            }
            return;
        }
        if (fstep == 3) {
            int g = GAPS[mrep];
            if (tick - bed1Tick == 3) System.out.println("SHOT1");
            if (g > 0 && tick - bed1Tick == g) {
                bed(mc, bestSpot(d.head.x, d.head.y, d.head.z));
                log("rep " + (mrep + 1) + " bed 2 " + g + " ticks later, dragon " + d.getHealth());
            }
            if (mrep == 0 && tick - bed1Tick == g + 3) {
                d.setHealth(5);
                d.invulnerableTimer = 0;
                bed(mc, bestSpot(d.head.x, d.head.y, d.head.z));
                log("killing bed: dragon " + d.getHealth() + " dead? removed " + d.removed + ", same world " + (mc.world == w0));
            }
            if (mrep == 0 && tick - bed1Tick == g + 10) log("after the kill: dragon " + d.getHealth() + ", ticksSinceDeath " + d.ticksSinceDeath + ", same world " + (mc.world == w0));
            if (g < 0 && tick - bed1Tick == 15) {
                log("pressing V after a missed second bed");
                try {
                    java.lang.reflect.Method m = endpractice.DoubleBed.class.getDeclaredMethod("skip", Minecraft.class);
                    m.setAccessible(true);
                    m.invoke(null, mc);
                } catch (Exception e) {
                    log("skip failed " + e);
                }
            }
            if (tick - bed1Tick > 70) {
                log("rep " + (mrep + 1) + " over, end stone in inventory " + countInv(mc) + ", cobble " + (mc.player.inventory.items[1] == null ? 0 : mc.player.inventory.items[1].size) + ", health " + mc.player.getHealth() + ", hud " + EndPractice.hud(mc));
                mrep++;
                if (mrep >= GAPS.length) {
                    log("DONE");
                    System.exit(0);
                }
                fstep = 2;
            }
        }
    }

    /** Heights of the ground in the 7x7 round the player, relative to their feet. */
    private static String flat(Minecraft mc) {
        StringBuilder sb = new StringBuilder();
        for (int dz = -3; dz <= 3; dz++) {
            for (int dx = -3; dx <= 3; dx++) sb.append(mc.world.getHeight(px + dx, pz + dz) - feetY);
            sb.append(' ');
        }
        return sb.toString();
    }

    private static int countItems(Minecraft mc) {
        int n = 0;
        for (Object o : mc.world.entities) if (o instanceof net.minecraft.entity.ItemEntity && ((net.minecraft.entity.ItemEntity) o).item.id == 121) n++;
        return n;
    }

    private static int countInv(Minecraft mc) {
        int n = 0;
        for (net.minecraft.item.ItemStack st : mc.player.inventory.items) if (st != null && st.id == 121) n += st.size;
        return n;
    }

    private static void startCharge(Minecraft mc) {
        for (int[] h : dug) mc.world.setBlock(h[0], feetY - 1, h[1], 121);
        dug.clear();
        mc.player.setPositionAndAngles(px + 0.5, feetY + 0.01, pz + 0.5, -90.0F, 0.0F);
        endpractice.Commands.onChat(mc, "/charge 50");
        tick = 0;
    }

    private static void bed(Minecraft mc, int spot) {
        double[] b = spot(spot);
        if (b[1] < feetY) {
            mc.world.setBlock((int) Math.floor(b[0]), feetY - 1, (int) Math.floor(b[2]), 0);
            dug.add(new int[]{(int) Math.floor(b[0]), (int) Math.floor(b[2])});
        }
        mc.world.explode(null, b[0], b[1], b[2], 5.0F, true);
    }

    private static void next(Minecraft mc, EnderDragonEntity d) {
        trial++;
        if (trial >= TRIALS * HEIGHTS.length) {
            log("SUMMARY (height bed1+best bed2): " + summary);
            log("DONE");
            dbstep = 9;
            System.exit(0);
            return;
        }
        startTrial(mc, d);
    }

    private static void startTrial(Minecraft mc, EnderDragonEntity d) {
        startH = HEIGHTS[trial / TRIALS];
        startD = DISTS[trial / TRIALS];
        double a = rand.nextDouble() * Math.PI * 2;
        dirX = Math.cos(a);
        dirZ = Math.sin(a);
        for (int[] h : dug) mc.world.setBlock(px + 0 * h[0] + (h[0] - px), feetY - 1, h[1], 121);
        dug.clear();
        double dx = px + 0.5 + dirX * startD, dz = pz + 0.5 + dirZ * startD;
        d.setHealth(200);
        d.invulnerableTimer = 0;
        float yaw = (float) (180.0 - Math.atan2(-dirX, -dirZ) * 180.0 / Math.PI);
        d.setPositionAndAngles(dx, feetY + startH, dz, yaw, 0);
        d.velocityX = -dirX * V0[trial / TRIALS];
        d.velocityZ = -dirZ * V0[trial / TRIALS];
        d.velocityY = 0;
        ((EnderDragonAccessor) d).endpractice$setTarget(mc.player);
        d.needsNewTarget = false;
        mc.player.setPosition(px + 0.5, feetY + mc.player.eyeHeight, pz + 0.5);
        tick = 0;
        dbstep = 2;
    }

    /** What a bed here would do now, the way Explosion and the dragon work it out (before invulnerability). */
    private static int damage(Minecraft mc, EnderDragonEntity d, double x, double y, double z) {
        Vec3d c = Vec3d.fromPool(x, y, z);
        int bestDmg = 0;
        for (Entity e : d.getParts()) {
            EnderDragonPart part = (EnderDragonPart) e;
            double dd = part.distanceTo(x, y, z) / 10.0;
            if (dd > 1.0) continue;
            double q = (1.0 - dd) * mc.world.getBlockDensity(c, part.shape);
            int dmg = (int) ((q * q + q) / 2.0 * 80.0 + 1.0);
            if (part != d.head) dmg = dmg / 4 + 1;
            bestDmg = Math.max(bestDmg, dmg);
        }
        return bestDmg;
    }

    private static void log(String s) {
        System.out.println("[doublebed] " + s);
    }
}
