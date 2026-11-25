package endpractice.selftest;

import endpractice.EndPractice;
import endpractice.Overworld;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** -Dendpractice.overworld=N: overworld practice, N worlds in a row, splits and furnace stats on the first. */
@Mixin(Minecraft.class)
public abstract class OverworldTestMixin {
    private static int owMode = Integer.getInteger("endpractice.overworld", 0);
    private static int owTicks, owWorlds;
    private static boolean owStarted;

    @Inject(method = "tick", at = @At("TAIL"))
    private void overworldTest(CallbackInfo ci) {
        if (owMode <= 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        owTicks++;
        if (!owStarted) {
            if (owTicks > 40 && mc.screen instanceof TitleScreen) {
                EndPractice.queueOverworld();
                owStarted = true;
                owTicks = 0;
                owWorlds = 1;
            }
            return;
        }
        if (mc.player == null || mc.world == null || !Overworld.isActive()) return;
        int fx = (int) Math.floor(mc.player.x), fy = (int) Math.floor(mc.player.y - mc.player.eyeHeight + 0.01), fz = (int) Math.floor(mc.player.z);
        if (owTicks == 60) {
            System.out.println("[overworld] READY world " + owWorlds + " at " + fx + " " + fy + " " + fz + " slot0 " + (mc.player.inventory.items[0] == null ? "-" : mc.player.inventory.items[0].id + "x" + mc.player.inventory.items[0].size) + " " + EndPractice.hud(mc));
            System.out.println("[overworld] SHOT" + owWorlds);
        }
        if (owWorlds < owMode) {
            if (owTicks == 100) {
                owWorlds++;
                owTicks = 0;
                EndPractice.queueOverworld();
            }
            return;
        }
        if (owTicks == 100) {
            mc.player.inventory.addItem(new ItemStack(274, 1, 0));
            // a furnace next to you with 6 iron ore and 4 planks
            mc.world.setBlock(fx + 2, fy, fz, 61);
            FurnaceBlockEntity f = (FurnaceBlockEntity) mc.world.getBlockEntity(fx + 2, fy, fz);
            f.setItem(0, new ItemStack(15, 6, 0));
            f.setItem(1, new ItemStack(5, 4, 0));
        }
        if (owTicks == 200) {
            mc.world.setBlock(fx + 2, fy, fz, 0);
            mc.world.setBlock(fx, fy, fz + 2, 61);
            FurnaceBlockEntity f = (FurnaceBlockEntity) mc.world.getBlockEntity(fx, fy, fz + 2);
            f.setItem(0, new ItemStack(15, 3, 0));
            f.setItem(1, new ItemStack(5, 2, 0));
            mc.world.setBlock(fx, fy, fz - 2, 61);
            FurnaceBlockEntity g = (FurnaceBlockEntity) mc.world.getBlockEntity(fx, fy, fz - 2);
            g.setItem(0, new ItemStack(15, 3, 0));
            g.setItem(1, new ItemStack(5, 2, 0));
        }
        if (owTicks == 420) {
            System.out.println("[overworld] HUD " + EndPractice.hud(mc));
            mc.player.inventory.addItem(new ItemStack(265, 1, 0));
        }
        if (owTicks == 440) {
            mc.player.inventory.addItem(new ItemStack(325, 1, 0));
            mc.player.inventory.addItem(new ItemStack(259, 1, 0));
        }
        if (owTicks == 460) mc.world.setBlock(fx - 2, fy, fz, 90);
        if (owTicks == 480) {
            System.out.println("[overworld] HUD before portal " + EndPractice.hud(mc));
            mc.world.setBlock(fx, fy, fz, 90);
        }
        if (owTicks == 490) {
            // in the portal without the leaves and logs: no finish; then pick them up
            System.out.println("[overworld] in portal, missing stuff: " + EndPractice.hud(mc));
            mc.player.inventory.addItem(new ItemStack(18, 64, 0));
            mc.player.inventory.addItem(new ItemStack(18, 64, 1));
            mc.player.inventory.addItem(new ItemStack(17, 8, 2));
        }
        if (owTicks == 510) {
            System.out.println("[overworld] HUD " + EndPractice.hud(mc));
            System.out.println("[overworld] DONE");
            Runtime.getRuntime().halt(0);
        }
    }
}
