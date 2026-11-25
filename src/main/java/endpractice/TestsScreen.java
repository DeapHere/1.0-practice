package endpractice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ListWidget;
import net.minecraft.client.render.vertex.Tesselator;
import org.lwjgl.input.Keyboard;

/**
 * Pit test stats (/tests, or Test stats in the saved pits menu): each design with its totals, and every test under it,
 * newest first, in a scrolling list. Click a row and Delete (twice) removes that test, or every test of a design.
 */
public final class TestsScreen extends Screen {
    private static final int DONE = 0, DELETE = 1, TOP = 32, BOTTOM = 34, ROW = 22;
    private final Screen parent;
    /** Rows: a design (its tests in tests) or one test. */
    private final List<Object[]> rows = new ArrayList<>();
    private int selected = -1;
    private boolean confirm;
    private TestList list;
    private ButtonWidget delete;

    public TestsScreen(Screen parent) {
        this.parent = parent;
        load();
    }

    private void load() {
        rows.clear();
        Map<String, List<String[]>> byDesign = new LinkedHashMap<>();
        List<String[]> all = PitSandbox.testRows();
        Collections.reverse(all); // newest first
        for (String[] t : all) byDesign.computeIfAbsent(t[1], k -> new ArrayList<>()).add(t);
        for (Map.Entry<String, List<String[]>> e : byDesign.entrySet()) {
            rows.add(new Object[]{e.getKey(), e.getValue()});
            for (String[] t : e.getValue()) rows.add(new Object[]{null, t});
        }
    }

    @Override
    public void init() {
        this.buttons.clear();
        list = new TestList();
        int cx = this.width / 2;
        delete = new ButtonWidget(DELETE, cx - 154, this.height - 27, 150, 20, "Delete");
        this.buttons.add(delete);
        this.buttons.add(new ButtonWidget(DONE, cx + 4, this.height - 27, 150, 20, "Done"));
        updateDelete();
    }

    private void updateDelete() {
        delete.active = selected >= 0 && selected < rows.size();
        if (!delete.active) {
            delete.message = "Delete";
            return;
        }
        Object[] r = rows.get(selected);
        String what = r[0] != null ? "all of " + r[0] : "this test";
        delete.message = confirm ? "§cSure? Delete " + what : "Delete " + what;
        if (this.textRenderer.getWidth(delete.message) > 140) delete.message = confirm ? "§cSure?" : r[0] != null ? "Delete all its tests" : "Delete this test";
    }

    @Override
    protected void buttonClicked(ButtonWidget b) {
        if (!b.active) return;
        if (b.id == DONE) {
            this.minecraft.openScreen(parent);
        } else if (b.id == DELETE) {
            if (!confirm) {
                confirm = true;
            } else {
                Object[] r = rows.get(selected);
                if (r[0] != null) PitSandbox.deleteTests(null, (String) r[0]);
                else PitSandbox.deleteTests(Collections.singleton(((String[]) r[1])[0]), null);
                confirm = false;
                selected = -1;
                load();
            }
            updateDelete();
        } else {
            list.buttonClicked(b);
        }
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (key == Keyboard.KEY_ESCAPE) this.minecraft.openScreen(parent);
    }

    private static long num(String[] t, int i) {
        try {
            return i < t.length ? Long.parseLong(t[i].trim()) : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String pct(long a, long b) {
        return b > 0 ? Math.round(a * 100.0 / b) + "%" : "-";
    }

    private static String perMin(long n, long ticks) {
        return String.format(Locale.ROOT, "%.1f", n * 1200.0 / Math.max(1, ticks));
    }

    @SuppressWarnings("unchecked")
    private String[] text(Object[] r) {
        if (r[0] != null) {
            long ticks = 0, killed = 0, pearls = 0;
            List<String[]> ts = (List<String[]>) r[1];
            for (String[] t : ts) {
                ticks += num(t, 2);
                killed += num(t, 6);
                pearls += num(t, 7);
            }
            return new String[]{"§e" + r[0] + "§r  §7" + ts.size() + " test" + (ts.size() == 1 ? "" : "s") + ", " + EndPractice.time(ticks) + "§r",
                    "Killed §a" + perMin(killed, ticks) + "/min§r   pearls §d" + perMin(pearls, ticks) + "/min§r   §7(" + killed + " killed, " + pearls + " pearls)§r"};
        }
        String[] t = (String[]) r[1];
        long ticks = num(t, 2), killed = num(t, 6), pearls = num(t, 7);
        String when = new SimpleDateFormat("d MMM HH:mm", Locale.ROOT).format(new Date(num(t, 0)));
        return new String[]{"  §7" + when + "  " + EndPractice.time(ticks) + "§r   killed §a" + killed + "§r (" + perMin(killed, ticks) + "/min)   pearls §d" + pearls
                + "§r (" + perMin(pearls, ticks) + "/min)", null};
    }

    @Override
    public void render(int mx, int my, float tickDelta) {
        list.render(mx, my, tickDelta);
        this.drawCenteredString(this.textRenderer, "Pit test stats", this.width / 2, 8, 0xFFFFFF);
        this.drawCenteredString(this.textRenderer, rows.isEmpty() ? "§7No tests yet: /test <name> in the pit sandbox§r"
                : "§7Click a design or a test, then Delete to remove it§r", this.width / 2, 19, 0xFFFFFF);
        super.render(mx, my, tickDelta);
    }

    /** The same scrolling list as Select World: mouse wheel, dragging, or the scrollbar on the right. */
    private final class TestList extends ListWidget {
        TestList() {
            super(TestsScreen.this.minecraft, TestsScreen.this.width, TestsScreen.this.height, TOP, TestsScreen.this.height - BOTTOM, ROW);
        }

        @Override
        protected int size() {
            return rows.size();
        }

        @Override
        protected void entryClicked(int index, boolean doubleClick) {
            if (selected != index) confirm = false;
            selected = index;
            updateDelete();
        }

        @Override
        protected boolean isEntrySelected(int index) {
            return index == selected;
        }

        @Override
        protected void renderBackground() {
            TestsScreen.this.renderBackground();
        }

        @Override
        protected void renderEntry(int index, int x, int y, int height, Tesselator tesselator) {
            String[] t = text(rows.get(index));
            int left = TestsScreen.this.width / 2 - 150;
            if (t[1] != null) {
                TestsScreen.this.drawString(TestsScreen.this.textRenderer, t[0], left, y + 1, 0xFFFFFF);
                TestsScreen.this.drawString(TestsScreen.this.textRenderer, t[1], left, y + 11, 0xFFFFFF);
            } else {
                TestsScreen.this.drawString(TestsScreen.this.textRenderer, t[0], left, y + 6, 0xFFFFFF);
            }
        }
    }
}
