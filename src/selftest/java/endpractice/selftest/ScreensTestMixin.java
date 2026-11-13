package endpractice.selftest;

import endpractice.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** -Dendpractice.screens=1: opens Options from the title screen, then OldschoolPrac's settings, for screenshots. */
@Mixin(Minecraft.class)
public abstract class ScreensTestMixin {
    private static boolean scOn = Integer.getInteger("endpractice.screens", 0) > 0;
    private static int scTicks;

    @Inject(method = "tick", at = @At("TAIL"))
    private void screensTest(CallbackInfo ci) {
        if (!scOn) return;
        Minecraft mc = (Minecraft) (Object) this;
        scTicks++;
        if (scTicks == 60) {
            mc.openScreen(new TitleScreen());
            System.out.println("[screens] options");
        } else if (scTicks == 400) {
            mc.openScreen(new endpractice.PracticeScreen(mc.screen));
            System.out.println("[screens] settings");
        } else if (scTicks == 500) {
            mc.openScreen(new endpractice.SettingsScreen(mc.screen, true));
            System.out.println("[screens] lab");
        } else if (scTicks == 600) {
            mc.openScreen(endpractice.StatsScreen.pbs(mc.screen));
            System.out.println("[screens] pbs");
        } else if (scTicks == 700) {
            mc.openScreen(new endpractice.StatsScreen(mc.screen));
            System.out.println("[screens] stats");
        } else if (scTicks == 760) {
            mc.openScreen(new endpractice.SettingsScreen(mc.screen));
            System.out.println("[screens] hotkeys");
        } else if (scTicks == 820) {
            mc.openScreen(new endpractice.KitEditorScreen(mc.screen));
            System.out.println("[screens] kits");
        } else if (scTicks == 900) {
            System.out.println("[screens] done");
        }
    }
}
