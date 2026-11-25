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
        if (Integer.getInteger("endpractice.screens", 0) == 2) {
            kitTest(mc);
            return;
        }
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

    private static Object f(Object o, String n) throws Exception {
        java.lang.reflect.Field fl = o.getClass().getDeclaredField(n);
        fl.setAccessible(true);
        return fl.get(o);
    }

    private static void click(Object scr, int x, int y, int b) throws Exception {
        java.lang.reflect.Method m = net.minecraft.client.gui.screen.Screen.class.getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
        m.setAccessible(true);
        // dispatch on the subclass
        java.lang.reflect.Method mm = scr.getClass().getDeclaredMethod("mouseClicked", int.class, int.class, int.class);
        mm.setAccessible(true);
        mm.invoke(scr, x, y, b);
    }

    private static String desc(Object st) {
        if (st == null) return "null";
        net.minecraft.item.ItemStack s = (net.minecraft.item.ItemStack) st;
        return s.id + "x" + s.size;
    }

    /** -Dendpractice.screens=2: kit editor clicks and every tab, for screenshots. */
    private static void kitTest(Minecraft mc) {
        try {
            if (scTicks == 60) {
                mc.openScreen(new endpractice.KitEditorScreen(new TitleScreen()));
                System.out.println("[kits] open " + mc.screen.width + "x" + mc.screen.height);
            } else if (scTicks == 80) {
                Object k = mc.screen;
                int px = (Integer) f(k, "px"), y0 = (Integer) f(k, "y0"), x0 = (Integer) f(k, "x0");
                java.util.List<?> pal = (java.util.List<?>) f(k, "palette");
                int first = (Integer) pal.get(0), second = (Integer) pal.get(1);
                click(k, px + 2, y0 + 2, 0);
                System.out.println("[kits] click item once: cursor " + desc(f(k, "cursor")) + " (want " + first + "x1)");
                click(k, px + 2, y0 + 2, 0);
                click(k, px + 2, y0 + 2, 0);
                System.out.println("[kits] click item 3x: cursor " + desc(f(k, "cursor")) + " (want x3)");
                click(k, px + 20, y0 + 2, 0);
                System.out.println("[kits] other item: cursor " + desc(f(k, "cursor")) + " (want " + second + "x1)");
                click(k, px + 2, y0 + 2, 0);
                click(k, px + 2, y0 + 2, 0);
                Object[] kit = ((Object[][]) f(k, "kits"))[0];
                // hotbar slot 8 is at x0+22+8*18, y0+3*18+4
                int hx = x0 + 22 + 8 * 18 + 3, hy = y0 + 3 * 18 + 4 + 3;
                Object before = kit[8];
                click(k, hx, hy, 0);
                System.out.println("[kits] place on slot 8 (had " + desc(before) + "): slot " + desc(kit[8]) + ", cursor " + desc(f(k, "cursor")) + " (swap)");
                click(k, hx, hy, 0);
                System.out.println("[kits] click again: slot " + desc(kit[8]) + ", cursor " + desc(f(k, "cursor")));
                click(k, hx, hy, 0);
                Object held = f(k, "cursor");
                System.out.println("[kits] pick up slot 8: slot " + desc(kit[8]) + ", cursor " + desc(held));
                click(k, x0 + 22 + 2, y0 + 3 * 18 + 4 + 3, 0);
                System.out.println("[kits] put into slot 0: slot0 " + desc(kit[0]) + ", cursor " + desc(f(k, "cursor")));
                click(k, hx, hy, 0);
                System.out.println("[kits] pick up whatever's in 8 and drop outside");
                click(k, 2, mc.screen.height / 2, 0);
                System.out.println("[kits] cursor after outside click " + desc(f(k, "cursor")) + ", slot8 " + desc(kit[8]));
                click(k, px + 2, y0 + 2, 1);
                System.out.println("[kits] right click item: cursor " + desc(f(k, "cursor")) + " (full stack)");
                click(k, hx, hy, 1);
                System.out.println("[kits] right click slot 8 holding: slot " + desc(kit[8]) + " cursor " + desc(f(k, "cursor")));
                click(k, px + 2, y0 + 2, 0);
            } else if (scTicks >= 120 && scTicks < 120 + 7 * 30 && (scTicks - 120) % 30 == 0) {
                int t = (scTicks - 120) / 30;
                java.lang.reflect.Field tf = mc.screen.getClass().getDeclaredField("tab");
                tf.setAccessible(true);
                tf.set(mc.screen, t);
                mc.screen.init(mc, mc.screen.width, mc.screen.height);
                System.out.println("[kits] tab " + t);
            } else if (scTicks == 120 + 7 * 30 + 10) {
                mc.screen.init(mc, 320, 240);
                System.out.println("[kits] narrow 320x240");
            } else if (scTicks == 120 + 7 * 30 + 20) {
                System.out.println("[kits] done");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
