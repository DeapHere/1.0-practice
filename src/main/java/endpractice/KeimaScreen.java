package endpractice;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.input.Keyboard;

/** After the second eye: how many pixels did your crosshair move (the Keima method)? */
public final class KeimaScreen extends Screen {
    private static final int OK = 0, SKIP = 1;
    private String value = "";

    @Override
    public void init() {
        this.buttons.clear();
        int cx = this.width / 2, y = this.height / 2;
        ButtonWidget ok = new ButtonWidget(OK, cx - 100, y + 34, 98, 20, "Check");
        ok.active = parse() > 0;
        this.buttons.add(ok);
        this.buttons.add(new ButtonWidget(SKIP, cx + 2, y + 34, 98, 20, "Skip"));
    }

    private double parse() {
        try {
            return value.isEmpty() ? -1 : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void submit() {
        double v = parse();
        if (v <= 0) return;
        this.minecraft.openScreen(null);
        EyeTrainer.keimaAnswer(this.minecraft, v);
    }

    @Override
    protected void buttonClicked(ButtonWidget b) {
        if (!b.active) return;
        if (b.id == OK) submit();
        else if (b.id == SKIP) {
            this.minecraft.openScreen(null);
            EndPractice.chat(this.minecraft, "§7Keima skipped. /keima <pixels> answers it later.§r");
        }
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            this.minecraft.openScreen(null);
            EndPractice.chat(this.minecraft, "§7Keima skipped. /keima <pixels> answers it later.§r");
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            submit();
            return;
        }
        if (key == Keyboard.KEY_BACK) {
            if (!value.isEmpty()) value = value.substring(0, value.length() - 1);
        } else if ((Character.isDigit(chr) || chr == '.' && !value.contains(".")) && value.length() < 7) {
            value += chr;
        } else {
            return;
        }
        init();
    }

    @Override
    public void render(int mx, int my, float tickDelta) {
        this.renderBackground();
        int cx = this.width / 2, y = this.height / 2;
        this.drawCenteredString(this.textRenderer, "§bKeima§r: how many pixels did your crosshair move?", cx, y - 40, 0xFFFFFF);
        this.drawCenteredString(this.textRenderer, "§7Count on the eye (it's 16 pixels wide). Distance is about " + EyeTrainer.keimaConstant() + " / pixels.§r", cx, y - 28, 0xFFFFFF);
        this.drawCenteredString(this.textRenderer, "§7Under 100 is pixels, 100 or more is your distance guess in blocks.§r", cx, y - 18, 0xFFFFFF);
        this.fill(cx - 51, y - 1, cx + 51, y + 21, 0xFFA0A0A0);
        this.fill(cx - 50, y, cx + 50, y + 20, 0xFF000000);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        this.drawCenteredString(this.textRenderer, value + (blink ? "_" : " "), cx, y + 6, 0xFFFFFF);
        super.render(mx, my, tickDelta);
    }
}
