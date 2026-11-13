package endpractice.selftest;

import endpractice.EndPractice;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** -Dendpractice.stress=N: resets N times at random moments, some after dying or killing the dragon. */
@Mixin(Minecraft.class)
public abstract class StressMixin {
    private static int left = Integer.getInteger("endpractice.stress", 0);
    private static int wait;
    private static int target = 40;
    private static int n;
    private static final Random R = new Random(1);
    private static long lastBeat;
    private static int respawnWait = -1;

    @Inject(method = "tick", at = @At("TAIL"))
    private void stress(CallbackInfo ci) {
        if (left <= 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        long now = System.currentTimeMillis();
        if (now - lastBeat > 2000) {
            lastBeat = now;
            System.out.println("[stress] beat screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName())
                    + " world=" + (mc.world == null ? "none" : mc.world.dimension.id) + " active=" + EndPractice.isActive());
        }
        if (respawnWait >= 0) {
            // dead and respawned in the overworld: let it generate chunks for a while before resetting
            if (respawnWait == 0 && mc.player != null && mc.player.getHealth() <= 0) {
                System.out.println("[stress] respawning in the overworld");
                mc.respawnPlayer(false, 0, false);
                mc.openScreen(null);
            }
            if (++respawnWait < 200) return;
            respawnWait = -1;
            wait = target;
        }
        if (++wait < target) return;
        wait = 0;
        if (mc.screen instanceof TitleScreen || EndPractice.isActive()) {
            n++;
            int kind = R.nextInt(5);
            if (kind == 4 && mc.player != null && EndPractice.isActive() && mc.world.dimension.id == 1) {
                System.out.println("[stress] dying, then respawning");
                mc.player.takeDamage(DamageSource.OUT_OF_WORLD, 1000);
                respawnWait = 0;
                n--;
                return;
            }
            if (kind == 1 && mc.player != null) {
                System.out.println("[stress] killing the player first");
                mc.player.takeDamage(DamageSource.OUT_OF_WORLD, 1000);
            } else if (kind == 2 && mc.world != null && EndPractice.dragon(mc) != null) {
                System.out.println("[stress] killing the dragon first");
                EndPractice.dragon(mc).setHealth(0);
            }
            Runtime rt = Runtime.getRuntime(); System.gc();
            System.out.println("[stress] reset " + n + " (" + left + " left) heapMB=" + (rt.totalMemory() - rt.freeMemory()) / 1048576);
            long t = System.currentTimeMillis();
            EndPractice.queueStart();
            left--;
            target = R.nextInt(3) == 0 ? 1 + R.nextInt(5) : 5 + R.nextInt(120);
            if (left == 0) System.out.println("[stress] STRESS DONE");
        }
    }
}
