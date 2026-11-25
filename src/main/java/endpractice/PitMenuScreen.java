package endpractice;

import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.input.Keyboard;

/**
 * Saved pits (the pit menu key, in the pit sandbox): every saved design with Load and Delete, and a name box to save
 * the pit you're standing in. The same as /pit load, /pit delete and /pit save.
 */
public final class PitMenuScreen extends Screen {
    private static final int DONE = 0, SAVE = 1, PREV = 2, NEXT = 3, STATS = 4, WORLD = 5, HUD = 6, LOAD = 100, DELETE = 300;
    private List<String> names;
    private Map<String, long[]> tested;
    private int page, perPage, confirmDelete = -1;
    private String name = "";

    @Override
    public void init() {
        this.buttons.clear();
        names = PitDesigns.names();
        tested = PitSandbox.designs();
        if (PitDesigns.current != null && name.isEmpty()) name = PitDesigns.current;
        perPage = Math.max(1, (this.height - 30 - 82) / 22);
        int pages = Math.max(1, (names.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pages - 1));
        int cx = this.width / 2, y = 30;
        for (int i = page * perPage; i < Math.min(names.size(), (page + 1) * perPage); i++) {
            this.buttons.add(new ButtonWidget(LOAD + i, cx - 150, y, 220, 20, names.get(i)));
            this.buttons.add(new ButtonWidget(DELETE + i, cx + 74, y, 76, 20, i == confirmDelete ? "§cSure?" : "Delete"));
            y += 22;
        }
        int by = this.height - 50;
        if (pages > 1) {
            ButtonWidget prev = new ButtonWidget(PREV, cx - 150, by - 24, 40, 20, "<");
            prev.active = page > 0;
            ButtonWidget next = new ButtonWidget(NEXT, cx + 110, by - 24, 40, 20, ">");
            next.active = page < pages - 1;
            this.buttons.add(prev);
            this.buttons.add(next);
        }
        ButtonWidget save = new ButtonWidget(SAVE, cx + 74, by, 76, 20, "Save here");
        save.active = !name.trim().isEmpty();
        this.buttons.add(save);
        ButtonWidget world = new ButtonWidget(WORLD, cx - 106, by - 24, 212, 20, "Save the whole world as a new world");
        world.active = !name.trim().isEmpty();
        this.buttons.add(world);
        this.buttons.add(new ButtonWidget(STATS, cx - 150, this.height - 26, 98, 20, "Test stats..."));
        this.buttons.add(new ButtonWidget(HUD, cx - 49, this.height - 26, 98, 20, "HUD..."));
        this.buttons.add(new ButtonWidget(DONE, cx + 52, this.height - 26, 98, 20, "Done"));
    }

    @Override
    protected void buttonClicked(ButtonWidget b) {
        if (!b.active) return;
        if (b.id == DONE) {
            this.minecraft.openScreen(null);
        } else if (b.id == WORLD) {
            this.minecraft.openScreen(null);
            PitSandbox.saveWorld(this.minecraft, name.trim());
        } else if (b.id == HUD) {
            this.minecraft.openScreen(new HudEditorScreen(this));
        } else if (b.id == STATS) {
            this.minecraft.openScreen(new TestsScreen(this));
        } else if (b.id == PREV) {
            page--;
            confirmDelete = -1;
            init();
        } else if (b.id == NEXT) {
            page++;
            confirmDelete = -1;
            init();
        } else if (b.id == SAVE) {
            this.minecraft.openScreen(null);
            PitDesigns.save(this.minecraft, name.trim());
        } else if (b.id >= DELETE) {
            int i = b.id - DELETE;
            if (confirmDelete != i) {
                confirmDelete = i;
            } else {
                String n = names.get(i);
                EndPractice.chat(this.minecraft, PitDesigns.delete(n) ? "Deleted " + n + "." : "§cCouldn't delete " + n + ".");
                confirmDelete = -1;
            }
            init();
        } else if (b.id >= LOAD) {
            String n = names.get(b.id - LOAD);
            this.minecraft.openScreen(null);
            PitDesigns.load(this.minecraft, n);
        }
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            this.minecraft.openScreen(null);
            return;
        }
        if (key == Keyboard.KEY_RETURN) {
            if (!name.trim().isEmpty()) {
                this.minecraft.openScreen(null);
                PitDesigns.save(this.minecraft, name.trim());
            }
            return;
        }
        if (key == Keyboard.KEY_BACK) {
            if (!name.isEmpty()) name = name.substring(0, name.length() - 1);
        } else if ((Character.isLetterOrDigit(chr) || chr == ' ' || chr == '_' || chr == '-' || chr == '.') && name.length() < 32) {
            name += chr;
        } else {
            return;
        }
        init();
    }

    @Override
    public void render(int mx, int my, float tickDelta) {
        this.renderBackground();
        int cx = this.width / 2;
        this.drawCenteredString(this.textRenderer, "Saved pits", cx, 10, 0xFFFFFF);
        if (names.isEmpty()) {
            this.drawCenteredString(this.textRenderer, "§7No pits saved yet. Stand in your pit, type a name below and Save here.§r", cx, 40, 0xFFFFFF);
        }
        int by = this.height - 50;
        this.fill(cx - 151, by - 1, cx + 71, by + 21, 0xFFA0A0A0);
        this.fill(cx - 150, by, cx + 70, by + 20, 0xFF000000);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        this.drawString(this.textRenderer, name.isEmpty() ? "§7Name to save as...§r" : name + (blink ? "_" : ""), cx - 146, by + 6, 0xFFFFFF);
        super.render(mx, my, tickDelta);
        // tooltip: how a design did in its tests
        for (Object o : this.buttons) {
            ButtonWidget b = (ButtonWidget) o;
            if (b.id < LOAD || b.id >= DELETE || !b.mouseClicked(this.minecraft, mx, my)) continue;
            String n = names.get(b.id - LOAD);
            long[] t = tested.get(n);
            String tip = t == null ? "Load: pastes it where you stand. Not tested yet"
                    : "Load: pastes it where you stand. Tested x" + t[8] + ": " + String.format(java.util.Locale.ROOT, "%.1f", t[4] * 1200.0 / Math.max(1, t[0])) + " kills, "
                    + String.format(java.util.Locale.ROOT, "%.1f", t[5] * 1200.0 / Math.max(1, t[0])) + " pearls a minute";
            int w = this.textRenderer.getWidth(tip);
            this.fill(mx + 10, my - 12, mx + 14 + w, my, 0xF0100010);
            this.drawString(this.textRenderer, tip, mx + 12, my - 10, 0xFFFFFF);
        }
    }
}
