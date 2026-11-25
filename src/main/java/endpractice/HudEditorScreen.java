package endpractice;

import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.input.Keyboard;

/**
 * Pit sandbox HUD editor (the X menu, or Settings): every piece of the sandbox HUD with on/off and up/down. Changes
 * show straight away in the HUD behind; Done keeps them, Reset puts the default back.
 */
public final class HudEditorScreen extends Screen {
    private static final int DONE = 0, RESET = 1, TOGGLE = 100, UP = 200, DOWN = 300;
    private final Screen parent;
    private final String before;
    private List<String[]> layout;

    /** The settings screen's own copy of the config, if opened from there, so its Done doesn't undo this. */
    private final Config also;

    public HudEditorScreen(Screen parent) {
        this(parent, null);
    }

    public HudEditorScreen(Screen parent, Config also) {
        this.parent = parent;
        this.also = also;
        this.before = EndPractice.cfg.sbHud;
        layout = PitSandbox.hudLayout();
    }

    @Override
    public void init() {
        this.buttons.clear();
        int n = layout.size();
        int row = Math.min(22, Math.max(20, (this.height - 60) / n));
        int y = Math.max(22, (this.height - 28 - n * row) / 2 + 4);
        int left = this.width / 2 - 60;
        for (int i = 0; i < n; i++) {
            String[] l = layout.get(i);
            this.buttons.add(new ButtonWidget(TOGGLE + i, left, y, 150, 20, name(l[0]) + ": " + (l[1].equals("1") ? "§aON" : "§7OFF")));
            ButtonWidget up = new ButtonWidget(UP + i, left + 154, y, 20, 20, "^");
            up.active = i > 0;
            ButtonWidget down = new ButtonWidget(DOWN + i, left + 178, y, 20, 20, "v");
            down.active = i < n - 1;
            this.buttons.add(up);
            this.buttons.add(down);
            y += row;
        }
        this.buttons.add(new ButtonWidget(RESET, this.width / 2 - 102, this.height - 24, 100, 20, "Reset"));
        this.buttons.add(new ButtonWidget(DONE, this.width / 2 + 2, this.height - 24, 100, 20, "Done"));
    }

    private static String name(String key) {
        for (String[] p : PitSandbox.HUD_PARTS) if (p[0].equals(key)) return p[1];
        return key;
    }

    private static String help(String key) {
        for (String[] p : PitSandbox.HUD_PARTS) if (p[0].equals(key)) return p[2];
        return "";
    }

    private void apply() {
        EndPractice.cfg.sbHud = PitSandbox.hudString(layout);
        init();
    }

    @Override
    protected void buttonClicked(ButtonWidget b) {
        if (!b.active) return;
        if (b.id == DONE) {
            if (also != null) also.sbHud = EndPractice.cfg.sbHud;
            EndPractice.cfg.save();
            this.minecraft.openScreen(parent);
        } else if (b.id == RESET) {
            EndPractice.cfg.sbHud = PitSandbox.HUD_DEFAULT;
            layout = PitSandbox.hudLayout();
            init();
        } else if (b.id >= DOWN) {
            int i = b.id - DOWN;
            layout.add(i + 1, layout.remove(i));
            apply();
        } else if (b.id >= UP) {
            int i = b.id - UP;
            layout.add(i - 1, layout.remove(i));
            apply();
        } else if (b.id >= TOGGLE) {
            String[] l = layout.get(b.id - TOGGLE);
            l[1] = l[1].equals("1") ? "0" : "1";
            apply();
        }
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            // Escape: leave without keeping the changes
            EndPractice.cfg.sbHud = before;
            this.minecraft.openScreen(parent);
        }
    }

    @Override
    public void render(int mx, int my, float tickDelta) {
        // no dark background, so the HUD in the top left shows each change
        this.drawCenteredString(this.textRenderer, "Sandbox HUD", this.width / 2 + 40, 6, 0xFFFFFF);
        String help = null;
        for (Object o : this.buttons) {
            ButtonWidget b = (ButtonWidget) o;
            if (b.id >= TOGGLE && b.id < UP && b.mouseClicked(this.minecraft, mx, my)) help = help(layout.get(b.id - TOGGLE)[0]);
        }
        if (help != null) this.drawCenteredString(this.textRenderer, "§7" + help + "§r", this.width / 2, this.height - 36, 0xFFFFFF);
        super.render(mx, my, tickDelta);
    }
}
