package endpractice;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

/** A page of numbers: the PBs (Stats in the practice menu) or the enderman tracker's (Endgame Lab). */
public final class StatsScreen extends Screen {
    private final Screen parent;
    private final String title;
    private final Supplier<List<String>> source;
    private List<String> lines;

    /** The enderman tracker's numbers. */
    public StatsScreen(Screen parent) {
        this(parent, "Enderman Stats", Endgame::statsLines);
    }

    public StatsScreen(Screen parent, String title, Supplier<List<String>> source) {
        this.parent = parent;
        this.title = title;
        this.source = source;
    }

    /** Every PB: the End fight, each endgame mode with its splits, and the crystal trainers. */
    public static StatsScreen pbs(Screen parent) {
        return new StatsScreen(parent, "Personal Bests", () -> {
            List<String> out = new ArrayList<>();
            long end = EndPractice.endPb();
            out.add("§dEnd fight§r   " + (end >= 0 ? "§e" + EndPractice.time(end) + "§r" : "§7none§r"));
            long[] st = EndPractice.endFightStats();
            if (st[0] > 0) {
                out.add("average §e" + EndPractice.time(st[1]) + "§r over " + st[0] + "   last 10 §e" + EndPractice.time(st[2])
                        + "§r   completed " + st[0] + "/" + st[3] + " (" + Math.round(100.0 * st[0] / Math.max(1, st[3])) + "%)");
            }
            out.add("");
            out.addAll(Endgame.pbLines());
            long crystals = CrystalTrainer.bestTime();
            out.add("§dCrystal aim trainer§r   " + (crystals >= 0 ? "§e" + EndPractice.time(crystals) + "§r" : "§7none§r"));
            out.add("§dRandom crystals§r   best streak §e" + CrystalTrainer.bestStreak() + "§r");
            out.add("");
            out.addAll(BlazePractice.pbLines());
            out.add("");
            out.addAll(EyeTrainer.pbLines());
            return out;
        });
    }

    @Override
    public void init() {
        this.buttons.clear();
        this.lines = source.get();
        this.buttons.add(new ButtonWidget(0, this.width / 2 - 100, this.height - 28, "Done"));
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (button.id == 0) this.minecraft.openScreen(parent);
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, title, this.width / 2, 8, 0xFFFFFF);
        int y = 26;
        for (String l : lines) {
            if (y > this.height - 40) break;
            this.drawCenteredString(this.textRenderer, l, this.width / 2, y, 0xE0E0E0);
            y += 10;
        }
        super.render(mouseX, mouseY, tickDelta);
    }
}
