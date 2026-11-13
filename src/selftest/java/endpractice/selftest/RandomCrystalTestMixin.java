package endpractice.selftest;

import endpractice.CrystalTrainer;
import endpractice.EndPractice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.EnderCrystalEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * -Dendpractice.randomcrystals=N: random crystal mode for N shots, one a second, in a fixed pattern of hits and
 * misses (high, low into the tower, to the side), so the streak and the miss lines can be checked in the log.
 */
@Mixin(Minecraft.class)
public abstract class RandomCrystalTestMixin {
    private static int rcLeft = Integer.getInteger("endpractice.randomcrystals", 0);
    private static int rcWait;
    private static boolean rcStarted;
    private static int rcShot;
    private static EnderCrystalEntity rcLastTarget;
    private static EnderCrystalEntity rcPendingHit;
    private static String rcPendingInfo;
    private static int rcAimed, rcAimedHits;

    private static final String[] PATTERN = {"hit", "hit", "high", "hit", "low", "hit", "side"};

    @Inject(method = "tick", at = @At("TAIL"))
    private void randomCrystalTest(CallbackInfo ci) {
        if (rcLeft <= 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        if (++rcWait < 40) return;
        rcWait = 0;
        if (!rcStarted) {
            if (!(mc.screen instanceof TitleScreen)) return;
            EndPractice.queueCrystals(true);
            rcStarted = true;
            return;
        }
        if (!CrystalTrainer.isRandomMode() || mc.player == null) return;
        System.out.println("[rctest] hud: " + String.join(" | ", EndPractice.hud(mc)).replaceAll("§.", ""));
        EnderCrystalEntity c = CrystalTrainer.target();
        if (c == null || c.removed) return;
        if (c != rcLastTarget) {
            rcLastTarget = c;
            System.out.println("[rctest] target at " + (int) c.x + " " + (int) c.y + " " + (int) c.z + ", player at "
                    + (int) mc.player.x + " " + (int) mc.player.y + " " + (int) mc.player.z
                    + " standing on " + mc.world.getBlock((int) Math.floor(mc.player.x), (int) Math.floor(mc.player.shape.minY) - 1, (int) Math.floor(mc.player.z)));
        }
        if (rcLeft-- == 1) {
            System.out.println("[rctest] RANDOM CRYSTAL TEST DONE");
            return;
        }
        if (rcPendingHit != null) {
            // the last aimed shot: did it take the crystal?
            rcAimed++;
            if (rcPendingHit.removed) rcAimedHits++;
            else System.out.println("[rctest] aimed shot MISSED " + rcPendingInfo);
            rcPendingHit = null;
            System.out.println("[rctest] aimed shots " + rcAimedHits + "/" + rcAimed);
            if (c.removed) return;
        }
        String kind = PATTERN[rcShot++ % PATTERN.length];
        System.out.println("[rctest] shot " + rcShot + ": " + kind + ", streak " + CrystalTrainer.streak());
        // a real fully drawn bow shot from where the player stands
        float yaw = CrystalTrainer.aimYaw, pitch = CrystalTrainer.aimPitch;
        if (kind.equals("high")) pitch -= 4;
        if (kind.equals("low")) pitch += 4;
        if (kind.equals("side")) yaw += 6;
        mc.player.yaw = yaw;
        mc.player.pitch = pitch;
        ArrowEntity a = new ArrowEntity(mc.world, mc.player, 2.0F);
        if (kind.equals("hit")) {
            rcPendingHit = c;
            rcPendingInfo = String.format(java.util.Locale.ROOT, "crystal %.1f %.1f %.1f from %.1f %.1f %.1f (feet %.2f) pitch %.2f",
                    c.x, c.y, c.z, mc.player.x, mc.player.y, mc.player.z, mc.player.shape.minY, pitch);
        }
        mc.world.addEntity(a);
    }
}
