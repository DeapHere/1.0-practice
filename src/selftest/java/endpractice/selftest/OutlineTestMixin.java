package endpractice.selftest;

import endpractice.EndPractice;
import endpractice.Endgame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.mob.monster.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** -Dendpractice.outline=1: a full endgame attempt with endermen in front, one buried, for a screenshot of the outlines. */
@Mixin(Minecraft.class)
public abstract class OutlineTestMixin {
    private static boolean olOn = Integer.getInteger("endpractice.outline", 0) > 0;
    private static int olTicks;
    private static boolean olStarted;

    @Inject(method = "tick", at = @At("TAIL"))
    private void outlineTest(CallbackInfo ci) {
        if (!olOn) return;
        Minecraft mc = (Minecraft) (Object) this;
        olTicks++;
        if (!olStarted) {
            if (olTicks > 40 && mc.screen instanceof TitleScreen) {
                EndPractice.queueEndgame(Endgame.Mode.FULL);
                olStarted = true;
                olTicks = 0;
            }
            return;
        }
        if (olTicks == 60 && mc.player != null) {
            mc.player.yaw = 0;
            mc.player.pitch = 10;
            double px = mc.player.x, py = mc.player.shape.minY, pz = mc.player.z;
            double[][] at = {{-3, 0, 8}, {3, 0, 12}, {0, -6, 10}};
            for (double[] a : at) {
                EndermanEntity e = new EndermanEntity(mc.world);
                e.setPositionAndAngles(px + a[0], py + a[1], pz + a[2], 180, 0);
                mc.world.addEntity(e);
                if (a[0] > 0) e.setWalkTarget(mc.player);
            }
            System.out.println("[outline] spawned");
        }
        if (olTicks >= 60 && olTicks <= 80 && mc.player != null) {
            for (Object o : mc.world.entities) if (o instanceof EndermanEntity && ((EndermanEntity) o).z - mc.player.z > 11) ((EndermanEntity) o).setWalkTarget(mc.player);
            mc.player.yaw = 0;
            mc.player.pitch = 10;
        }
        if (olTicks >= 300 && olTicks <= 340 && mc.player != null && endpractice.PitPlanner.pit() != null) {
            int[] pit = endpractice.PitPlanner.pit();
            mc.player.yaw = (float) Math.toDegrees(Math.atan2(-(pit[0] - mc.player.x), pit[2] - mc.player.z));
            mc.player.pitch = 25;
            if (olTicks == 340) System.out.println("[outline] PLAN");
        }
        // flush overlay: die among zombies 180 blocks out, respawn at the spawn, they should all despawn
        if (olTicks == 400 && mc.player != null) {
            EndPractice.cfg.flushOverlay = true;
            net.minecraft.util.math.Vec3i sp = mc.world.getSpawnPoint();
            int x = sp.x + 180, z = sp.z;
            mc.world.getBlock(x, 64, z);
            int y = mc.world.getHeight(x, z);
            mc.player.setPosition(x + 0.5, y + 1.62, z + 0.5);
            for (int i = 0; i < 8; i++) {
                net.minecraft.entity.mob.monster.ZombieEntity zb = new net.minecraft.entity.mob.monster.ZombieEntity(mc.world);
                int zx = x + (i % 4) * 3 - 5, zz = z + (i / 4) * 4 - 2;
                zb.setPositionAndAngles(zx + 0.5, mc.world.getHeight(zx, zz), zz + 0.5, 0, 0);
                mc.world.addEntity(zb);
            }
            System.out.println("[outline] zombies placed");
        }
        if (olTicks == 420 && mc.player != null) {
            System.out.println("[outline] before death " + mc.world.getEntityCount(net.minecraft.entity.mob.monster.Monster.class));
            mc.player.setHealth(0);
        }
        if (olTicks == 470 && mc.player != null) {
            mc.player.respawn();
            mc.openScreen(null);
        }
        if (olTicks == 540) System.out.println("[outline] FLUSH " + EndPractice.hudRight(mc));
        if (olTicks == 63) { int n = 0; for (Object o : mc.world.entities) if (o instanceof EndermanEntity && ((EndermanEntity) o).angry) n++; System.out.println("[outline] SHOOT angry=" + n); }
    }
}
