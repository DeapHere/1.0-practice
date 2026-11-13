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
}
