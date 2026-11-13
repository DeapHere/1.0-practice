package endpractice.selftest;

import endpractice.EndPractice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.monster.BlazeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** -Dendpractice.blaze=1 (open) or 2 (enclosed): a blaze attempt, then blazes killed at your feet until the rods are in. */
@Mixin(Minecraft.class)
public abstract class BlazeTestMixin {
    private static int bzMode = Integer.getInteger("endpractice.blaze", 0);
    private static int bzTicks;
    private static boolean bzStarted;

    @Inject(method = "tick", at = @At("TAIL"))
    private void blazeTest(CallbackInfo ci) {
        if (bzMode <= 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        bzTicks++;
        if (!bzStarted) {
            if (bzTicks > 40 && mc.screen instanceof TitleScreen) {
                if (bzMode == 3) EndPractice.cfg.blazeTowerCrafts = true;
                EndPractice.queueBlaze(bzMode == 2);
                bzStarted = true;
                bzTicks = 0;
            }
            return;
        }
        if (mc.player == null) return;
        if (mc.player.getHealth() > 0) mc.player.setHealth(20);
        if (bzTicks == 80) System.out.println("[blaze] READY dim " + mc.world.dimension.id + " at " + (int) mc.player.x + " " + (int) mc.player.y + " " + (int) mc.player.z + " " + EndPractice.hud(mc));
        if (bzTicks >= 200 && bzTicks <= 400 && bzTicks % 10 == 0) {
            BlazeEntity b = new BlazeEntity(mc.world);
            b.setPositionAndAngles(mc.player.x, mc.player.y - 1.5, mc.player.z, 0, 0);
            mc.world.addEntity(b);
            b.takeDamage(DamageSource.player(mc.player), 100);
        }
        if (bzTicks == 420) System.out.println("[blaze] BEFORE CRAFTS " + EndPractice.hud(mc));
        if (bzTicks == 425 && bzMode == 3) {
            int[][] give = {{72, 1}, {107, 1}, {65, 3}, {113, 3}, {333, 1}};
            for (int i = 0; i < give.length; i++) mc.player.inventory.items[27 + i] = new net.minecraft.item.ItemStack(give[i][0], give[i][1], 0);
        }
        if (bzTicks == 460) System.out.println("[blaze] DONE " + EndPractice.hud(mc));
    }
}
