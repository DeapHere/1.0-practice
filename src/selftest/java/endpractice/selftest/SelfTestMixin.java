package endpractice.selftest;

import endpractice.EndPractice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.mob.monster.boss.EnderDragonEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class SelfTestMixin {
    private static int step;
    private static int wait;
    private static int attempt;

    @Inject(method = "tick", at = @At("TAIL"))
    private void selftest(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        if (Integer.getInteger("endpractice.stress", 0) > 0 || Integer.getInteger("endpractice.endgame", 0) > 0 || Integer.getInteger("endpractice.screens", 0) > 0 || Integer.getInteger("endpractice.crystals", 0) > 0 || Integer.getInteger("endpractice.randomcrystals", 0) > 0 || Integer.getInteger("endpractice.outline", 0) > 0 || Integer.getInteger("endpractice.blaze", 0) > 0 || Integer.getInteger("endpractice.eyes", 0) > 0) return;
        if (++wait < 40) return;
        if (step == 0 && mc.screen instanceof TitleScreen) {
            log("title screen: starting an attempt");
            EndPractice.queueStart();
            step = 1;
            wait = 0;
            return;
        }
        if (!EndPractice.isActive() || mc.world == null || mc.world.dimension.id != 1) return;
        EnderDragonEntity d = EndPractice.dragon(mc);
        if (d == null) {
            if (wait % 100 == 0) log("no dragon loaded yet");
            return;
        }
        float r = d.yaw * (float) Math.PI / 180.0F;
        double fx = MathHelper.sin(r), fz = -MathHelper.cos(r);
        switch (step) {
            case 1:
                log("in the End at " + (int) mc.player.x + " " + (int) mc.player.y + " " + (int) mc.player.z
                        + ", dragon " + d.getHealth() + ", crystals " + EndPractice.crystals(mc)
                        + ", slot0 " + (mc.player.inventory.items[0] == null ? "empty" : mc.player.inventory.items[0].id));
                // keep the player out of the way
                mc.player.setPosition(d.x, d.y + 40, d.z);
                mc.player.abilities.invulnerable = true;
                mc.player.abilities.flying = true;
                break;
            case 2:
                log("bed exactly on the head part, health " + d.getHealth());
                mc.world.explode(null, d.head.x, d.head.y, d.head.z, 5.0F, true);
                break;
            case 3:
                log("bed 3 behind and 1 below the head part, health " + d.getHealth());
                mc.world.explode(null, d.head.x - fx * 3, d.head.y - 1, d.head.z - fz * 3, 5.0F, true);
                break;
            case 4:
                log("bed at the tail end, 9 behind the head, health " + d.getHealth());
                mc.world.explode(null, d.head.x - fx * 9, d.head.y + 1, d.head.z - fz * 9, 5.0F, true);
                break;
            case 5:
                log("bed 4 in front of the head, health " + d.getHealth());
                mc.world.explode(null, d.head.x + fx * 4, d.head.y, d.head.z + fz * 4, 5.0F, true);
                break;
            case 6:
                log("killing bed, health " + d.getHealth());
                d.setHealth(5);
                mc.world.explode(null, d.head.x, d.head.y, d.head.z, 5.0F, true);
                break;
            case 7:
                log("restarting from in game");
                EndPractice.queueStart();
                break;
            case 8:
                log("second attempt: dragon " + d.getHealth() + ", crystals " + EndPractice.crystals(mc) + "; hud " + EndPractice.hud(mc));
                log("SELFTEST DONE");
                break;
            default:
                return;
        }
        step++;
        wait = 0;
    }

    private static void log(String s) {
        System.out.println("[selftest] " + s);
    }
}
