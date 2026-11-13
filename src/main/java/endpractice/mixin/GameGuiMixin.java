package endpractice.mixin;

import endpractice.EndPractice;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameGui.class)
public abstract class GameGuiMixin {
    @Shadow private Minecraft minecraft;

    @Inject(method = "render", at = @At("TAIL"))
    private void endpractice$hud(float tickDelta, boolean screenOpen, int mouseX, int mouseY, CallbackInfo ci) {
        Minecraft mc = this.minecraft;
        if (EndPractice.cfg == null || !EndPractice.cfg.hud || mc.options.hideGui || mc.options.debugEnabled) return;
        List<String> lines = EndPractice.hud(mc);
        int y = 2;
        for (String s : lines) {
            mc.textRenderer.drawWithShadow(s, 2, y, 0xFFFFFF);
            y += 10;
        }
        List<String> right = EndPractice.hudRight(mc);
        if (right.isEmpty()) return;
        int width = new net.minecraft.client.render.Window(mc.options, mc.width, mc.height).getWidth();
        y = 2;
        for (String s : right) {
            mc.textRenderer.drawWithShadow(s, width - 2 - mc.textRenderer.getWidth(s), y, 0xFFFFFF);
            y += 10;
        }
    }
}
