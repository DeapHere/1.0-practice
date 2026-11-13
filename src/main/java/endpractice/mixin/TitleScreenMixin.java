package endpractice.mixin;

import endpractice.EndPractice;
import endpractice.PracticeScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An ender pearl button left of Singleplayer opens the practice menu; the practice keys work here too. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    private static final int PRACTICE_BUTTON = 7314;

    private ButtonWidget endpractice$button;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void endpractice$startKey(char chr, int key, CallbackInfo ci) {
        EndPractice.init();
        if (!EndPractice.cfg.modeKeys) return;
        if (key != Keyboard.KEY_NONE && key == EndPractice.cfg.keyStart) {
            EndPractice.queueStart();
            ci.cancel();
        } else if (key != Keyboard.KEY_NONE && key == EndPractice.cfg.keyCrystals) {
            EndPractice.queueCrystals(false);
            ci.cancel();
        } else if (key != Keyboard.KEY_NONE && key == EndPractice.cfg.keyRandomCrystals) {
            EndPractice.queueCrystals(true);
            ci.cancel();
        } else if (key != Keyboard.KEY_NONE && key == EndPractice.cfg.keyBlazeOpen) {
            EndPractice.queueBlaze(false);
            ci.cancel();
        } else if (key != Keyboard.KEY_NONE && key == EndPractice.cfg.keyBlazeEnclosed) {
            EndPractice.queueBlaze(true);
            ci.cancel();
        } else if (key != Keyboard.KEY_NONE && key == EndPractice.cfg.keyEyeTrainer) {
            EndPractice.queueEyes();
            ci.cancel();
        } else if (EndPractice.queueEndgameKey(key)) {
            ci.cancel();
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void endpractice$addButton(CallbackInfo ci) {
        this.endpractice$button = new ButtonWidget(PRACTICE_BUTTON, this.width / 2 - 124, this.height / 4 + 48, 20, 20, "");
        this.buttons.add(this.endpractice$button);
    }

    @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
    private void endpractice$click(ButtonWidget button, CallbackInfo ci) {
        if (button.id == PRACTICE_BUTTON) {
            ci.cancel();
            this.minecraft.openScreen(new PracticeScreen(this));
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void endpractice$renderButton(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
        ButtonWidget b = this.endpractice$button;
        if (b == null || !this.buttons.contains(b)) return;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.minecraft.textureManager.load("/gui/items.png"));
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        int sprite = Item.ENDER_PEARL.getSprite(0);
        this.drawTexture(b.x + 2, b.y + 2, sprite % 16 * 16, sprite / 16 * 16, 16, 16);
        GL11.glDisable(GL11.GL_BLEND);
        if (mouseX >= b.x && mouseY >= b.y && mouseX < b.x + 20 && mouseY < b.y + 20) {
            String text = "OldschoolPrac";
            int w = this.textRenderer.getWidth(text);
            int x = Math.max(4, b.x - w - 7), y = b.y + 6;
            this.fillGradient(x - 3, y - 3, x + w + 3, y + 8 + 3, 0xC0000000, 0xC0000000);
            this.drawString(this.textRenderer, text, x, y, 0xFFFFFF);
        }
    }
}
