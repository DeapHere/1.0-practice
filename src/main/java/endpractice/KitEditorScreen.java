package endpractice;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

/**
 * Kit editor (Kits in the practice menu): 1.0 has no creative inventory to build a kit from, so this edits the kits
 * directly. Pick an item from the list on the right (type to search, scroll for more), click inventory slots to put it
 * there. Right click a slot to empty it; click a slot with nothing picked to select it and change its count.
 */
public final class KitEditorScreen extends Screen {
    private static final int DONE = 0, CANCEL = 1, RESET = 2, MINUS = 3, PLUS = 4, MAX = 5, CLEAR = 6, TAB = 10;
    private static final String[] TABS = {"End fight", "Tower", "Stronghold", "Blaze", "Eye throw"};
    private static final int SLOTS = 40, S = 18, COLS = 11, ROWS = 7;
    /** Blocks that aren't items you can hold (liquids, fire, portals, door and bed halves and the like). */
    private static final int[] SKIP = {0, 8, 9, 10, 11, 26, 34, 36, 51, 55, 59, 62, 63, 64, 68, 71, 74, 75, 83, 90, 92, 93, 94,
            104, 105, 115, 117, 118, 119, 124};
    private static final ItemRenderer items = new ItemRenderer();

    private final Screen parent;
    private final Config c = Config.load();
    private final Config defaults = new Config();
    private final ItemStack[][] kits = new ItemStack[TABS.length][];
    private int tab;
    private ItemStack cursor;
    private int selected = -1;
    private String search = "";
    private int scroll;
    private final List<Integer> palette = new ArrayList<>();
    private int x0, y0, px;

    public KitEditorScreen(Screen parent) {
        this.parent = parent;
        for (int i = 0; i < TABS.length; i++) kits[i] = parse(get(c, i));
        filter();
    }

    private static String get(Config c, int i) {
        switch (i) {
            case 0: return c.kit;
            case 1: return c.towerKit;
            case 2: return c.strongholdKit;
            case 3: return c.blazeKit;
            default: return c.eyeKit;
        }
    }

    private void set(int i, String v) {
        switch (i) {
            case 0: c.kit = v; break;
            case 1: c.towerKit = v; break;
            case 2: c.strongholdKit = v; break;
            case 3: c.blazeKit = v; break;
            default: c.eyeKit = v; break;
        }
    }

    static ItemStack[] parse(String kit) {
        ItemStack[] out = new ItemStack[SLOTS];
        for (String entry : kit.split(",")) {
            String[] f = entry.trim().split(":");
            if (f.length < 3) continue;
            try {
                int slot = Integer.parseInt(f[0].trim()), id = Integer.parseInt(f[1].trim()), n = Integer.parseInt(f[2].trim());
                int dmg = f.length > 3 ? Integer.parseInt(f[3].trim()) : 0;
                if (slot >= 0 && slot < SLOTS && id > 0 && id < Item.BY_ID.length && Item.BY_ID[id] != null) out[slot] = new ItemStack(id, n, dmg);
            } catch (RuntimeException ignored) {
            }
        }
        return out;
    }

    static String write(ItemStack[] kit) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < kit.length; i++) {
            ItemStack s = kit[i];
            if (s == null || s.size <= 0) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(i).append(':').append(s.id).append(':').append(s.size);
            if (s.getMetadata() != 0) sb.append(':').append(s.getMetadata());
        }
        return sb.toString();
    }

    private void filter() {
        palette.clear();
        String q = search.toLowerCase();
        outer:
        for (int id = 1; id < Item.BY_ID.length; id++) {
            Item it = Item.BY_ID[id];
            if (it == null) continue;
            for (int s : SKIP) if (s == id) continue outer;
            if (!q.isEmpty() && !name(new ItemStack(id, 1, 0)).toLowerCase().contains(q)) continue;
            palette.add(id);
        }
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int maxScroll() {
        return Math.max(0, (palette.size() + COLS - 1) / COLS - ROWS);
    }

    private static String name(ItemStack s) {
        try {
            String n = s.getItem().getName(s);
            return n == null || n.isEmpty() ? "#" + s.id : n;
        } catch (RuntimeException e) {
            return "#" + s.id;
        }
    }

    @Override
    public void init() {
        this.buttons.clear();
        int cx = this.width / 2;
        x0 = cx - 206;
        y0 = 46;
        px = x0 + 196;
        for (int i = 0; i < TABS.length; i++) {
            ButtonWidget b = new ButtonWidget(TAB + i, cx - 154 + i * 62, 16, 60, 20, TABS[i]);
            b.active = i != tab;
            this.buttons.add(b);
        }
        int by = y0 + 4 * S + 12;
        this.buttons.add(new ButtonWidget(MINUS, x0 + 22, by, 30, 20, "-1"));
        this.buttons.add(new ButtonWidget(PLUS, x0 + 56, by, 30, 20, "+1"));
        this.buttons.add(new ButtonWidget(MAX, x0 + 90, by, 40, 20, "Stack"));
        this.buttons.add(new ButtonWidget(CLEAR, x0 + 134, by, 50, 20, "Empty"));
        this.buttons.add(new ButtonWidget(RESET, cx - 154, this.height - 26, 100, 20, "Reset this kit"));
        this.buttons.add(new ButtonWidget(DONE, cx - 50, this.height - 26, 100, 20, "Done"));
        this.buttons.add(new ButtonWidget(CANCEL, cx + 54, this.height - 26, 100, 20, "Cancel"));
        updateButtons();
    }

    private void updateButtons() {
        for (Object o : this.buttons) {
            ButtonWidget b = (ButtonWidget) o;
            if (b.id == MINUS || b.id == PLUS || b.id == MAX || b.id == CLEAR) b.active = selected >= 0 && kits[tab][selected] != null;
            if (b.id >= TAB) b.active = b.id - TAB != tab;
        }
    }

    @Override
    protected void buttonClicked(ButtonWidget b) {
        if (!b.active) return;
        ItemStack s = selected >= 0 ? kits[tab][selected] : null;
        if (b.id >= TAB) {
            tab = b.id - TAB;
            selected = -1;
        } else if (b.id == MINUS && s != null) {
            s.size = Math.max(1, s.size - 1);
        } else if (b.id == PLUS && s != null) {
            s.size = Math.min(64, s.size + 1);
        } else if (b.id == MAX && s != null) {
            s.size = Math.max(1, s.getMaxSize());
        } else if (b.id == CLEAR && s != null) {
            kits[tab][selected] = null;
        } else if (b.id == RESET) {
            kits[tab] = parse(get(defaults, tab));
            selected = -1;
        } else if (b.id == DONE) {
            for (int i = 0; i < TABS.length; i++) set(i, write(kits[i]));
            c.save();
            EndPractice.cfg = c;
            this.minecraft.openScreen(parent);
            return;
        } else if (b.id == CANCEL) {
            this.minecraft.openScreen(parent);
            return;
        }
        updateButtons();
    }

    /** Screen position of a kit slot: armor down the left (helmet on top), then 3 inventory rows, then the hotbar. */
    private int[] slotPos(int slot) {
        if (slot >= 36) return new int[]{x0, y0 + (39 - slot) * S};
        if (slot < 9) return new int[]{x0 + 22 + slot * S, y0 + 3 * S + 4};
        int i = slot - 9;
        return new int[]{x0 + 22 + (i % 9) * S, y0 + (i / 9) * S};
    }

    private int slotAt(int mx, int my) {
        for (int i = 0; i < SLOTS; i++) {
            int[] p = slotPos(i);
            if (mx >= p[0] && mx < p[0] + S - 1 && my >= p[1] && my < p[1] + S - 1) return i;
        }
        return -1;
    }

    private int paletteAt(int mx, int my) {
        if (mx < px || my < y0 || mx >= px + COLS * S || my >= y0 + ROWS * S) return -1;
        int i = (scroll + (my - y0) / S) * COLS + (mx - px) / S;
        return i < palette.size() ? palette.get(i) : -1;
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        int id = paletteAt(mx, my);
        if (id > 0) {
            ItemStack s = new ItemStack(id, 1, 0);
            if (button == 0) s.size = Math.max(1, s.getMaxSize());
            cursor = s;
            return;
        }
        int slot = slotAt(mx, my);
        if (slot >= 0) {
            if (button == 1) {
                kits[tab][slot] = null;
                if (selected == slot) selected = -1;
            } else if (cursor != null) {
                kits[tab][slot] = cursor.copy();
                selected = slot;
            } else {
                selected = slot;
            }
            updateButtons();
            return;
        }
        if (button == 1) cursor = null;
        super.mouseClicked(mx, my, button);
    }

    @Override
    public void handleMouse() {
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll = Math.max(0, Math.min(maxScroll(), scroll + (wheel > 0 ? -1 : 1)));
        super.handleMouse();
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (key == Keyboard.KEY_ESCAPE) {
            if (cursor != null) cursor = null;
            else this.minecraft.openScreen(parent);
            return;
        }
        if (key == Keyboard.KEY_BACK) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
        } else if (chr >= ' ' && chr < 127 && search.length() < 20) {
            search += chr;
        } else {
            return;
        }
        scroll = 0;
        filter();
    }

    @Override
    public void render(int mx, int my, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, "Kit Editor", this.width / 2, 4, 0xFFFFFF);
        // slot backgrounds
        for (int i = 0; i < SLOTS; i++) {
            int[] p = slotPos(i);
            this.fill(p[0], p[1], p[0] + S - 1, p[1] + S - 1, i == selected ? 0xC0A060E0 : 0x80000000);
        }
        for (int r = 0; r < ROWS; r++) {
            for (int col = 0; col < COLS; col++) {
                int i = (scroll + r) * COLS + col;
                if (i >= palette.size()) break;
                int x = px + col * S, y = y0 + r * S;
                this.fill(x, y, x + S - 1, y + S - 1, 0x60000000);
            }
        }
        this.drawString(this.textRenderer, search.isEmpty() ? "§7Type to search, scroll for more§r" : "Search: §e" + search + "_§r", px, y0 - 10, 0xFFFFFF);
        this.drawString(this.textRenderer, "§7Armor§r", x0, y0 - 10, 0xFFFFFF);
        // items
        GL11.glPushMatrix();
        Lighting.turnOnGui();
        GL11.glEnable(32826);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = kits[tab][i];
            if (s == null) continue;
            int[] p = slotPos(i);
            drawItem(s, p[0] + 1, p[1] + 1, true);
        }
        for (int r = 0; r < ROWS; r++) {
            for (int col = 0; col < COLS; col++) {
                int i = (scroll + r) * COLS + col;
                if (i >= palette.size()) break;
                drawItem(new ItemStack(palette.get(i), 1, 0), px + col * S + 1, y0 + r * S + 1, false);
            }
        }
        if (cursor != null) drawItem(cursor, mx - 8, my - 8, true);
        GL11.glDisable(32826);
        Lighting.turnOff();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();

        int infoY = y0 + 4 * S + 38;
        String info;
        if (cursor != null) info = "Placing §e" + name(cursor) + " x" + cursor.size + "§r: click slots. Right click or Esc to stop";
        else if (selected >= 0 && kits[tab][selected] != null) info = "Slot: §e" + name(kits[tab][selected]) + " x" + kits[tab][selected].size + "§r";
        else info = "§7Pick an item on the right (right click: just 1), then click slots. Right click a slot to empty it§r";
        for (String line : wrap(info, 190)) {
            this.drawString(this.textRenderer, line, x0, infoY, 0xFFFFFF);
            infoY += 10;
        }
        super.render(mx, my, tickDelta);
        // tooltip
        ItemStack hover = null;
        int pid = paletteAt(mx, my);
        if (pid > 0) hover = new ItemStack(pid, 1, 0);
        int slot = slotAt(mx, my);
        if (slot >= 0) hover = kits[tab][slot];
        if (hover != null && cursor == null) {
            String n = name(hover) + (slot >= 0 ? " x" + hover.size : "");
            int w = this.textRenderer.getWidth(n);
            this.fill(mx + 10, my - 12, mx + 14 + w, my, 0xF0100010);
            this.drawString(this.textRenderer, n, mx + 12, my - 10, 0xFFFFFF);
        }
    }

    private void drawItem(ItemStack s, int x, int y, boolean count) {
        try {
            items.renderGuiItemWithEnchantmentGlint(this.textRenderer, this.minecraft.textureManager, s, x, y);
            if (count) items.renderGuiItemDecoration(this.textRenderer, this.minecraft.textureManager, s, x, y);
        } catch (RuntimeException e) {
            // an item that can't be drawn in a GUI: leave the slot empty-looking
        }
    }

    private List<String> wrap(String text, int max) {
        List<String> out = new ArrayList<>();
        String line = "";
        for (String w : text.split(" ")) {
            String next = line.isEmpty() ? w : line + " " + w;
            if (!line.isEmpty() && this.textRenderer.getWidth(next) > max) {
                out.add(line);
                line = w;
            } else {
                line = next;
            }
        }
        if (!line.isEmpty()) out.add(line);
        return out;
    }
}
