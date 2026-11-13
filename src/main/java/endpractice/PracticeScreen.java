package endpractice;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.input.Keyboard;

/** The practice menu from the ender pearl button on the title screen: every mode, the settings, and the Endgame Lab (testing tools and stats). */
public final class PracticeScreen extends Screen {
    private static final int END = 0, FULL = 1, TOWER = 2, STRONGHOLD = 3, SETTINGS = 4, BACK = 5, CRYSTALS = 6, RANDOM_CRYSTALS = 7, STATS = 8, LAB = 9, BLAZE_OPEN = 10, BLAZE_ENCLOSED = 11, KITS = 12, EYES = 13;

    private final Screen parent;

    public PracticeScreen(Screen parent) {
        this.parent = parent;
        EndPractice.init();
    }

    /** Top of the menu, and where each section's heading goes. */
    private int top;
    private static final String[] HEADINGS = {"Endgame", "End Fight", "Trainers", "Blaze Fight"};
    private static final int SECTION = 34;

    @Override
    public void init() {
        this.buttons.clear();
        int x = this.width / 2 - 100;
        top = Math.max(4, this.height / 2 - 100);
        int y = top + 24;
        this.buttons.add(new ButtonWidget(FULL, x, y, 64, 20, "Full run"));
        this.buttons.add(new ButtonWidget(TOWER, x + 68, y, 64, 20, "Tower build"));
        this.buttons.add(new ButtonWidget(STRONGHOLD, x + 136, y, 64, 20, "Stronghold"));
        y += SECTION;
        this.buttons.add(new ButtonWidget(END, x, y, 200, 20, "Dragon fight"));
        y += SECTION;
        this.buttons.add(new ButtonWidget(CRYSTALS, x, y, 64, 20, "Crystals"));
        this.buttons.add(new ButtonWidget(RANDOM_CRYSTALS, x + 68, y, 64, 20, "Random"));
        this.buttons.add(new ButtonWidget(EYES, x + 136, y, 64, 20, "Eye throw"));
        y += SECTION;
        this.buttons.add(new ButtonWidget(BLAZE_OPEN, x, y, 98, 20, "Open spawner"));
        this.buttons.add(new ButtonWidget(BLAZE_ENCLOSED, x + 102, y, 98, 20, "Enclosed spawner"));
        y += 30;
        this.buttons.add(new ButtonWidget(SETTINGS, x, y, 47, 20, "Settings"));
        this.buttons.add(new ButtonWidget(KITS, x + 51, y, 47, 20, "Kits"));
        this.buttons.add(new ButtonWidget(STATS, x + 102, y, 47, 20, "Stats"));
        this.buttons.add(new ButtonWidget(LAB, x + 153, y, 47, 20, "Lab"));
        this.buttons.add(new ButtonWidget(BACK, x, y + 24, "Back"));
    }

    /** The section's keys, in grey after its heading, when the mode keys are on. */
    private static String keys(int... ks) {
        if (!EndPractice.cfg.modeKeys) return "";
        StringBuilder sb = new StringBuilder();
        for (int k : ks) {
            sb.append(sb.length() == 0 ? "  \u00a77" : " / ").append(k == Keyboard.KEY_NONE ? "-" : Keyboard.getKeyName(k));
        }
        return sb.append("\u00a7r").toString();
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        switch (button.id) {
            case END: EndPractice.queueStart(); break;
            case FULL: EndPractice.queueEndgame(Endgame.Mode.FULL); break;
            case TOWER: EndPractice.queueEndgame(Endgame.Mode.TOWER); break;
            case STRONGHOLD: EndPractice.queueEndgame(Endgame.Mode.STRONGHOLD); break;
            case CRYSTALS: EndPractice.queueCrystals(false); break;
            case BLAZE_OPEN: EndPractice.queueBlaze(false); break;
            case BLAZE_ENCLOSED: EndPractice.queueBlaze(true); break;
            case RANDOM_CRYSTALS: EndPractice.queueCrystals(true); break;
            case EYES: EndPractice.queueEyes(); break;
            case SETTINGS: this.minecraft.openScreen(new SettingsScreen(this)); break;
            case STATS: this.minecraft.openScreen(StatsScreen.pbs(this)); break;
            case KITS: this.minecraft.openScreen(new KitEditorScreen(this)); break;
            case LAB: this.minecraft.openScreen(new SettingsScreen(this, true)); break;
            case BACK: this.minecraft.openScreen(parent); break;
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        Config c = EndPractice.cfg;
        this.drawCenteredString(this.textRenderer, "OldschoolPrac", this.width / 2, top, 0xFFFFFF);
        String[] k = {keys(c.keyEndgame, c.keyTower, c.keyStronghold), keys(c.keyStart), keys(c.keyCrystals, c.keyRandomCrystals, c.keyEyeTrainer),
                keys(c.keyBlazeOpen, c.keyBlazeEnclosed)};
        for (int i = 0; i < HEADINGS.length; i++) {
            this.drawString(this.textRenderer, HEADINGS[i] + k[i], this.width / 2 - 100, top + 14 + i * SECTION, 0xD8A0FF);
        }
        super.render(mouseX, mouseY, tickDelta);
    }
}
