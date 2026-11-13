package endpractice.selftest;

import endpractice.CrystalTrainer;
import endpractice.EndPractice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.EnderCrystalEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** -Dendpractice.crystals=N: N crystal trainer attempts, shooting an arrow at the nearest crystal every second. */
@Mixin(Minecraft.class)
public abstract class CrystalTestMixin {
    private static int ctLeft = Integer.getInteger("endpractice.crystals", 0);
    private static int ctWait;
    private static boolean ctStarted;
    private static int ctShots;

    @Inject(method = "tick", at = @At("TAIL"))
    private void crystalTest(CallbackInfo ci) {
        if (ctLeft <= 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        if (++ctWait < 20) return;
        ctWait = 0;
        if (!ctStarted) {
            if (!(mc.screen instanceof TitleScreen) && !CrystalTrainer.isActive()) return;
            EndPractice.queueCrystals();
            ctStarted = true;
            ctShots = 0;
            return;
        }
        if (!CrystalTrainer.isActive() || mc.player == null) return;
        System.out.println("[ctest] hud: " + String.join(" | ", EndPractice.hud(mc)).replaceAll("§.", ""));
        if (EndPractice.crystals(mc) == 0 || ctShots > 60) {
            System.out.println("[ctest] attempt done, player at " + (int) mc.player.x + " " + (int) mc.player.y + " " + (int) mc.player.z
                    + " dragon=" + (EndPractice.dragon(mc) != null));
            ctLeft--;
            ctStarted = false;
            if (ctLeft == 0) System.out.println("[ctest] CRYSTAL TEST DONE");
            return;
        }
        // nearest crystal; every third shot misses on purpose
        Entity best = null;
        double bd = 1e9;
        for (Object o : mc.world.entities) {
            if (o instanceof EnderCrystalEntity && !((Entity) o).removed) {
                double d = mc.player.distanceTo((Entity) o);
                if (d < bd) { bd = d; best = (Entity) o; }
            }
        }
        if (best == null) return;
        // shot by the player from 3 blocks away; every third one goes straight up and misses
        ArrowEntity a = new ArrowEntity(mc.world, mc.player, 1.0F);
        a.setPosition(best.x + 3, best.y + 0.8, best.z);
        if (++ctShots % 3 == 0) a.shoot(0, 1, 0, 1.0F, 0.0F);
        else a.shoot(-1, 0, 0, 1.5F, 0.0F);
        mc.world.addEntity(a);
    }
}
