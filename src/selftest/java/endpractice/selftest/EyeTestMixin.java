package endpractice.selftest;

import endpractice.Commands;
import endpractice.EndPractice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** -Dendpractice.eyes=1: the eye throw trainer (two eyes and a guess) and the singleplayer commands, with screenshots cues. */
@Mixin(Minecraft.class)
public abstract class EyeTestMixin {
    private static int eyMode = Integer.getInteger("endpractice.eyes", 0);
    private static int eyTicks;
    private static boolean eyStarted;

    @Inject(method = "tick", at = @At("TAIL"))
    private void eyeTest(CallbackInfo ci) {
        if (eyMode <= 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        eyTicks++;
        if (!eyStarted) {
            if (eyTicks > 40 && mc.screen instanceof TitleScreen) {
                EndPractice.cfg.eyeAngles = true;
                EndPractice.queueEyes();
                eyStarted = true;
                eyTicks = 0;
            }
            return;
        }
        if (mc.player == null || mc.world == null) return;
        if (eyMode == 2) {
            bugSweep(mc);
            return;
        }
        if (eyMode == 3) {
            keimaAndClosest(mc);
            return;
        }
        if (eyTicks == 100) {
            System.out.println("[eyes] READY " + (int) mc.player.x + " " + (int) mc.player.z + " " + EndPractice.hud(mc));
            ItemStack eye = mc.player.inventory.items[0];
            System.out.println("[eyes] slot0 " + (eye == null ? "-" : eye.id + "x" + eye.size));
            mc.interactionManager.useItem(mc.player, mc.world, eye);
        }
        if (eyTicks == 160) {
            mc.player.sendChat("/tp ~40 ~ ~40");
            mc.interactionManager.useItem(mc.player, mc.world, mc.player.inventory.items[0]);
        }
        if (eyTicks == 220) {
            ItemStack eye = mc.player.inventory.items[0];
            System.out.println("[eyes] eyes refilled: " + (eye == null ? "-" : eye.id + "x" + eye.size) + " " + EndPractice.hud(mc));
            mc.player.sendChat("hello");
            mc.player.sendChat("/gamemode sp");
            BlockPos sh = mc.world.findNearestStructure("Stronghold", (int) mc.player.x, 64, (int) mc.player.z);
            mc.player.sendChat("/tp " + (sh.x + 6) + " " + (sh.y + 2) + " " + (sh.z + 6));
        }
        if (eyTicks == 260) {
            System.out.println("[eyes] SPECTATOR spectating=" + Commands.spectating(mc) + " noClip=" + mc.player.noClip + " flying=" + mc.player.abilities.flying
                    + " y=" + (int) mc.player.y + " block at head " + mc.world.getBlock((int) Math.floor(mc.player.x), (int) Math.floor(mc.player.y), (int) Math.floor(mc.player.z)));
        }
        if (eyTicks == 420) {
            System.out.println("[eyes] after 40 ticks in the ground y=" + (int) mc.player.y + " health " + mc.player.getHealth());
            mc.player.sendChat("/gamemode c");
            System.out.println("[eyes] CREATIVE creative=" + mc.interactionManager.creative + " noClip=" + mc.player.noClip);
            mc.player.sendChat("/gm s");
            System.out.println("[eyes] SURVIVAL creative=" + mc.interactionManager.creative + " canFly=" + mc.player.abilities.canFly);
            mc.player.sendChat("/time set night");
            mc.player.sendChat("/weather rain");
            mc.player.sendChat("/give 264 3");
            mc.player.sendChat("/nope");
            BlockPos s = mc.world.findNearestStructure("Stronghold", (int) mc.player.x, 64, (int) mc.player.z);
            mc.player.sendChat("/guess " + (s.x + 10) + " " + (s.z - 5));
        }
        if (eyTicks == 440) {
            // the portal room worked out from scratch (as for a guess before the stronghold loads) matches the generated one
            BlockPos gen = endpractice.EyeTrainer.portalRoom(mc.world, endpractice.Endgame.spotStronghold);
            endpractice.EyeTrainer.forgetStrongholds(mc.world);
            BlockPos fresh = endpractice.EyeTrainer.portalRoom(mc.world, endpractice.Endgame.spotStronghold);
            System.out.println("[eyes] PORTAL generated " + gen.x + " " + gen.y + " " + gen.z + " fresh " + fresh.x + " " + fresh.y + " " + fresh.z);
        }
        if (eyTicks == 460) System.out.println("[eyes] DONE " + EndPractice.hud(mc));
    }

    private static java.util.List<int[]> points;
    private static int bugs, flagged, agree;

    /** -Dendpractice.eyes=2: real eyes thrown from points all over, checked against which stronghold is really closest. */
    @SuppressWarnings("unchecked")
    private static void bugSweep(Minecraft mc) {
        if (eyTicks == 100) {
            net.minecraft.world.gen.structure.StructureFeature f = ((net.minecraft.world.gen.chunk.OverworldChunkGenerator) ((endpractice.mixin.ChunkCacheAccessor) mc.world.getChunkSource()).endpractice$getGenerator()).stronghold;
            java.util.List<BlockPos> sh = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$potentialPositions();
            System.out.println("[eyes] STRONGHOLDS " + sh.get(0).x + " " + sh.get(0).z + " | " + sh.get(1).x + " " + sh.get(1).z + " | " + sh.get(2).x + " " + sh.get(2).z);
            points = new java.util.ArrayList<>();
            for (int i = 0; i < 3; i++) {
                BlockPos a = sh.get(i), b = sh.get((i + 1) % 3);
                for (int k = 1; k < 8; k++) points.add(new int[]{a.x + (b.x - a.x) * k / 8, 70, a.z + (b.z - a.z) * k / 8});
                for (int d = 0; d < 8; d++) {
                    double ang = Math.PI * 2 * d / 8;
                    points.add(new int[]{(int) (a.x + Math.cos(ang) * 350), d % 2 == 0 ? 70 : 110, (int) (a.z + Math.sin(ang) * 350)});
                }
                points.add(new int[]{a.x / 2, 70, a.z / 2});
            }
            points.add(new int[]{0, 70, 0});
        }
        int i = eyTicks - 110;
        if (points != null && i >= 0 && i < points.size()) {
            int[] pt = points.get(i);
            mc.player.setPosition(pt[0] + 0.5, pt[1] + mc.player.eyeHeight, pt[2] + 0.5);
            BlockPos real = mc.world.findNearestStructure("Stronghold", (int) mc.player.x, (int) mc.player.y, (int) mc.player.z);
            net.minecraft.world.gen.structure.StructureFeature f = ((net.minecraft.world.gen.chunk.OverworldChunkGenerator) ((endpractice.mixin.ChunkCacheAccessor) mc.world.getChunkSource()).endpractice$getGenerator()).stronghold;
            java.util.List<BlockPos> sh = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$potentialPositions();
            BlockPos closest = null;
            double d = 1e18;
            for (BlockPos c : sh) {
                double e = Math.hypot(c.x - mc.player.x, c.z - mc.player.z);
                if (e < d) { d = e; closest = c; }
            }
            // which stronghold the eye went to, by identity: a spot itself, or the start whose first piece it's flying to
            BlockPos went = null;
            for (BlockPos c : sh) if (c.x == real.x && c.z == real.z && real.y == 64) went = c;
            for (java.util.Map.Entry<Long, net.minecraft.world.gen.structure.StructureStart> e : ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$getStructures().entrySet()) {
                if (e.getValue().getPieces().isEmpty()) continue;
                BlockPos c = ((net.minecraft.world.gen.structure.StructurePiece) e.getValue().getPieces().get(0)).getCenterPos();
                if (c.x == real.x && c.y == real.y && c.z == real.z) for (BlockPos q : sh) if (net.minecraft.util.math.ChunkPos.toLong(q.x >> 4, q.z >> 4) == e.getKey()) went = q;
            }
            boolean bug = went != closest;
            System.out.println("[eyes] ROOMDIST " + (went == null ? "?" : (int) Math.hypot(went.x - real.x, went.z - real.z)));
            System.out.println("[eyes] THROW " + pt[0] + " " + pt[1] + " " + pt[2] + " -> " + real.x + " " + real.y + " " + real.z + " closest " + closest.x + " " + closest.z + " BUG=" + bug);
            mc.interactionManager.useItem(mc.player, mc.world, mc.player.inventory.items[0]);
        }
        if (points != null && i == points.size() + 5) {
            System.out.println("[eyes] DONE");
            Runtime.getRuntime().halt(0);
        }
    }

    // -Dendpractice.eyes=3: the Keima method for real (throw, 4 sideways jumps, throw), then eyes from all over must go to
    // the closest portal room
    private static net.minecraft.entity.EnderEyeEntity kEye;
    private static int kStage, kThrowTick, kLandings, kMismatch, kChecked;
    private static boolean kAir;
    private static net.minecraft.client.entity.mob.player.Input kInput;
    private static double kL1;
    private static int kTries;

    private static net.minecraft.entity.EnderEyeEntity newestEye(Minecraft mc) {
        net.minecraft.entity.EnderEyeEntity e = null;
        for (Object o : mc.world.entities) if (o instanceof net.minecraft.entity.EnderEyeEntity && !((net.minecraft.entity.Entity) o).removed) e = (net.minecraft.entity.EnderEyeEntity) o;
        return e;
    }

    private static double field(Object o, String name) {
        try {
            java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getDouble(o);
        } catch (Throwable t) {
            return Double.NaN;
        }
    }

    private static void keimaAndClosest(Minecraft mc) {
        if (eyTicks == 90) {
            EndPractice.cfg.keima = true;
            EndPractice.cfg.keimaOverlay = true;
            mc.player.abilities.invulnerable = true;
            // flat ground to jump on: a 40x40 glass floor 3 up, with air above
            int x0 = (int) Math.floor(mc.player.x), z0 = (int) Math.floor(mc.player.z), y0 = 120;
            for (int dx = -25; dx <= 25; dx++) for (int dz = -25; dz <= 25; dz++) {
                mc.world.setBlock(x0 + dx, y0, z0 + dz, 20);
                for (int y = y0 + 1; y < y0 + 5; y++) mc.world.setBlock(x0 + dx, y, z0 + dz, 0);
            }
            mc.player.setPosition(x0 + 0.5, y0 + 1 + mc.player.eyeHeight + 0.01, z0 + 0.5);
        }
        if (eyTicks == 105) mc.player.sendChat("/keima calibrate");
        if (eyTicks >= 110 && kStage == 0 && mc.screen == null) {
            mc.interactionManager.useItem(mc.player, mc.world, mc.player.inventory.items[0]);
            kEye = newestEye(mc);
            kThrowTick = eyTicks;
            kStage = 1;
        }
        if (kStage == 1 && kEye != null) {
            int t = eyTicks - kThrowTick;
            double h = Math.hypot(kEye.x - mc.player.x, kEye.z - mc.player.z);
            if (t == 30 || t == 40 || t == 50 || t == 60 || t == 70 || t == 79) System.out.println("[eyes] KEIMA eye 1 at tick " + t + ": " + String.format("%.2f", h) + " blocks out, " + String.format("%.2f", kEye.y - mc.player.y) + " up");
            if (t == 60) kL1 = h;
            if (kEye.removed || t > 85) {
                // face the eye's line and strafe 4 jumps
                double tx = field(kEye, "targetX"), tz = field(kEye, "targetZ");
                mc.player.yaw = (float) Math.toDegrees(Math.atan2(-(tx - mc.player.x), tz - mc.player.z));
                kInput = mc.player.input;
                mc.player.input = new net.minecraft.client.entity.mob.player.Input() {
                    public void tick(net.minecraft.entity.mob.player.PlayerEntity p) { this.movementSideways = kTries % 2 == 0 ? 1.0F : -1.0F; this.jumping = true; }
                };
                kLandings = 0;
                kAir = false;
                kStage = 2;
            }
        } else if (kStage == 2) {
            if (!mc.player.onGround) kAir = true;
            else if (kAir) {
                kAir = false;
                if (++kLandings == 4) {
                    mc.player.input = kInput;
                    mc.interactionManager.useItem(mc.player, mc.world, mc.player.inventory.items[0]);
                    kStage = 3;
                    kThrowTick = eyTicks;
                }
            }
        } else if (kStage == 3) {
            if (mc.screen instanceof endpractice.KeimaScreen) {
                double[] t = endpractice.EyeTrainer.keimaTruth();
                System.out.println("[eyes] KEIMA screen open after " + (eyTicks - kThrowTick) + " ticks. truth: " + String.format("%.2f px, moved %.2f sideways, real %.0f", t[0], t[1], t[2])
                        + "; C/px = " + Math.round(endpractice.EyeTrainer.KEIMA_C / t[0]) + " (C " + endpractice.EyeTrainer.KEIMA_C + ", eye 1 was " + String.format("%.2f", kL1) + " out at tick 60) " + EndPractice.hud(mc));
                System.out.println("[eyes] SHOT1");
                kStage = 4;
                kThrowTick = eyTicks;
            }
        } else if (kStage == 4 && eyTicks - kThrowTick == 20) {
            double[] t = endpractice.EyeTrainer.keimaTruth();
            mc.openScreen(null);
            endpractice.EyeTrainer.keimaAnswer(mc, Math.round(t[0]));
            if (++kTries < 5) {
                kStage = 0;
                return;
            }
            kStage = 5;
            kThrowTick = eyTicks;
            EndPractice.cfg.keima = false;
        } else if (kStage == 5) {
            // eyes from all over: each must fly at the closest of the three portal rooms
            int i = eyTicks - kThrowTick - 5;
            if (i == 0) {
                net.minecraft.world.gen.structure.StructureFeature f = ((net.minecraft.world.gen.chunk.OverworldChunkGenerator) ((endpractice.mixin.ChunkCacheAccessor) mc.world.getChunkSource()).endpractice$getGenerator()).stronghold;
                java.util.List<BlockPos> sh = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$potentialPositions();
                points = new java.util.ArrayList<>();
                for (int k = 0; k < 3; k++) {
                    BlockPos a = sh.get(k), b = sh.get((k + 1) % 3);
                    for (int j = 1; j < 16; j++) points.add(new int[]{a.x + (b.x - a.x) * j / 16, 70, a.z + (b.z - a.z) * j / 16});
                    for (int d = 0; d < 8; d++) {
                        double ang = Math.PI * 2 * d / 8;
                        points.add(new int[]{(int) (a.x + Math.cos(ang) * 350), d % 2 == 0 ? 70 : 110, (int) (a.z + Math.sin(ang) * 350)});
                    }
                }
                points.add(new int[]{0, 70, 0});
            }
            if (points != null && i >= 1 && i % 2 == 1 && (i - 1) / 2 < points.size()) {
                int[] pt = points.get((i - 1) / 2);
                mc.player.setPosition(pt[0] + 0.5, pt[1] + mc.player.eyeHeight, pt[2] + 0.5);
                mc.interactionManager.useItem(mc.player, mc.world, mc.player.inventory.items[0]);
                net.minecraft.entity.EnderEyeEntity e = newestEye(mc);
                net.minecraft.world.gen.structure.StructureFeature f = ((net.minecraft.world.gen.chunk.OverworldChunkGenerator) ((endpractice.mixin.ChunkCacheAccessor) mc.world.getChunkSource()).endpractice$getGenerator()).stronghold;
                java.util.List<BlockPos> sh = ((endpractice.mixin.StructureFeatureAccessor) f).endpractice$potentialPositions();
                BlockPos best = null;
                double d = 1e18;
                for (BlockPos s : new java.util.ArrayList<BlockPos>(sh)) {
                    BlockPos room = endpractice.EyeTrainer.portalRoom(mc.world, s);
                    double h = Math.hypot(room.x - e.x, room.z - e.z);
                    if (h < d) { d = h; best = room; }
                }
                double tx = field(e, "targetX"), tz = field(e, "targetZ");
                double a1 = Math.atan2(tz - e.z, tx - e.x), a2 = Math.atan2(best.z - e.z, best.x - e.x);
                double diff = Math.abs(Math.toDegrees(Math.atan2(Math.sin(a1 - a2), Math.cos(a1 - a2))));
                kChecked++;
                if (diff > 0.5) {
                    kMismatch++;
                    System.out.println("[eyes] CLOSEST mismatch at " + pt[0] + " " + pt[2] + ": off by " + String.format("%.1f", diff) + " degrees");
                }
                e.remove();
            }
            if (points != null && (i - 1) / 2 == points.size() + 2) {
                System.out.println("[eyes] CLOSEST " + kChecked + " throws, " + kMismatch + " not at the closest portal room");
                System.out.println("[eyes] DONE");
                Runtime.getRuntime().halt(0);
            }
        }
    }
}
