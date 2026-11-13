package endpractice;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ListWidget;
import net.minecraft.client.render.vertex.Tesselator;
import org.lwjgl.input.Keyboard;

/**
 * OldschoolPrac's settings in game (Settings in the practice menu). It edits a
 * fresh copy of endpractice.properties; Done writes the file and the mod uses it straight away, Escape throws it away.
 */
public final class SettingsScreen extends Screen {
    private static final int DONE = 912;

    private final Screen parent;
    private final boolean lab;
    private final Config c;
    private final List<Entry> entries = new ArrayList<>();
    private Entry waitingForKey;

    public SettingsScreen(Screen parent) {
        this(parent, false);
    }

    /**
     * lab: the Endgame Lab screen instead, with the testing tools for the endgame night (the enderman tracker and
     * stats, outlines, the spawn and flush overlays, the pit planner).
     */
    public SettingsScreen(Screen parent, boolean lab) {
        this.parent = parent;
        this.lab = lab;
        EndPractice.init();
        this.c = Config.load();
        if (lab) {
            link("Enderman stats...", "Your enderman averages and the last runs from the enderman tracker",
                    () -> this.minecraft.openScreen(new StatsScreen(this)));
            choice("Pearl luck", "Average: 1 pearl every 2 endermen. Lucky: 2 every 3. Vanilla: random 0 or 1 each. The tracker needs vanilla",
                    new String[]{"average", "lucky", "vanilla"}, () -> c.pearlLuck, v -> c.pearlLuck = v);
            entries.add(new Entry("Enderman tracker") {
                String label() {
                    return "vanilla".equals(c.pearlLuck) ? name + ": " + (c.endermanTracker ? "ON" : "OFF") : name + ": OFF (vanilla luck only)";
                }
                String help() {
                    return "vanilla".equals(c.pearlLuck)
                            ? "Saves the endermen spawned, killed and pearls of every full endgame run and shows the averages"
                            : "Only with Pearl luck on vanilla, so the saved drop rates are real ones. Set Pearl luck to vanilla first";
                }
                void click(boolean back) {
                    if ("vanilla".equals(c.pearlLuck)) c.endermanTracker = !c.endermanTracker;
                }
            });
            entries.add(new Entry("Tracker: cycles a night") {
                String label() {
                    return "vanilla".equals(c.pearlLuck) ? name + ": " + (c.cycleTracker ? "ON" : "OFF") : name + ": OFF (vanilla luck only)";
                }
                String help() {
                    return "With the enderman tracker: counts your death reset cycles each night (die in the pit, respawn on the tower) and shows the average";
                }
                void click(boolean back) { if ("vanilla".equals(c.pearlLuck)) c.cycleTracker = !c.cycleTracker; }
            });
            list("Surface enderman range", "Surface endermen only count this close to you. 87 is as far as endermen are drawn, 0 is anywhere loaded",
                    new int[]{32, 48, 64, 87, 100, 128, 0}, () -> c.endermanRange, v -> c.endermanRange = v);
            bool("Enderman outlines", "Draws every enderman's hitbox, seen through blocks, in practice worlds",
                    () -> c.endermanOutline, v -> c.endermanOutline = v);
            bool("Enderman aggro dot", "A red dot above every enderman that's angry at you, seen through blocks, in practice worlds",
                    () -> c.endermanAggroDot, v -> c.endermanAggroDot = v);
            bool("Spawn overlay", "Top right: the monsters loaded against the mob cap, the despawns, and the last spawn burst",
                    () -> c.spawnOverlay, v -> c.spawnOverlay = v);
            bool("Flush overlay", "Top right: how many of the loaded monsters despawned when you last respawned on the tower",
                    () -> c.flushOverlay, v -> c.flushOverlay = v);
            bool("Respawn timer", "After your first death in the pit: counts down to when monsters start spawning (the best time to respawn) and says how early or late you were",
                    () -> c.respawnTimer, v -> c.respawnTimer = v);
            bool("Pit planner", "Picks the best pit spot from the spawn tower and draws the line to run on the ground",
                    () -> c.pitPlanner, v -> c.pitPlanner = v);
            list("Pit distance", "Blocks from the tower to the pit. At 256 every mob around the tower despawns when you reach the pit",
                    new int[]{180, 205, 220, 235, 250, 256}, () -> c.pitDistance, v -> c.pitDistance = v);
            key("Plan pit from here", () -> c.keyPitPlan, v -> c.keyPitPlan = v);
            return;
        }
        Config d = new Config(); // the defaults, for the kit resets

        heading("Hotkeys");
        bool("Mode keys on the title screen", "Off: start modes from the practice menu only. In a mode its key still restarts it, unless set to NONE",
                () -> c.modeKeys, v -> c.modeKeys = v);
        key("Dragon fight", () -> c.keyStart, v -> c.keyStart = v);
        key("Full run", () -> c.keyEndgame, v -> c.keyEndgame = v);
        key("Tower build", () -> c.keyTower, v -> c.keyTower = v);
        key("Stronghold", () -> c.keyStronghold, v -> c.keyStronghold = v);
        key("Crystals", () -> c.keyCrystals, v -> c.keyCrystals = v);
        key("Random crystals", () -> c.keyRandomCrystals, v -> c.keyRandomCrystals = v);
        key("Open spawner", () -> c.keyBlazeOpen, v -> c.keyBlazeOpen = v);
        key("Enclosed spawner", () -> c.keyBlazeEnclosed, v -> c.keyBlazeEnclosed = v);
        key("Eye throw", () -> c.keyEyeTrainer, v -> c.keyEyeTrainer = v);
        key("Guess stronghold here", () -> c.keyGuess, v -> c.keyGuess = v);
        key("Save kit (press twice)", () -> c.keySaveKit, v -> c.keySaveKit = v);
        key("Plan pit from here", () -> c.keyPitPlan, v -> c.keyPitPlan = v);
        action("Set all hotkeys to NONE", "Turns every hotkey off, for menu only", () -> {
            c.keyStart = c.keyEndgame = c.keyTower = c.keyStronghold = c.keyCrystals = c.keyRandomCrystals = Keyboard.KEY_NONE;
            c.keyBlazeOpen = c.keyBlazeEnclosed = c.keySaveKit = c.keyPitPlan = c.keyEyeTrainer = c.keyGuess = Keyboard.KEY_NONE;
        });
        action("Reset hotkeys", "Back to the default keys", () -> {
            c.keyStart = d.keyStart; c.keyEndgame = d.keyEndgame; c.keyTower = d.keyTower; c.keyStronghold = d.keyStronghold;
            c.keyCrystals = d.keyCrystals; c.keyRandomCrystals = d.keyRandomCrystals; c.keyBlazeOpen = d.keyBlazeOpen;
            c.keyBlazeEnclosed = d.keyBlazeEnclosed; c.keySaveKit = d.keySaveKit; c.keyPitPlan = d.keyPitPlan;
            c.keyEyeTrainer = d.keyEyeTrainer; c.keyGuess = d.keyGuess;
        });

        heading("Commands");
        bool("Singleplayer commands", "T opens chat in singleplayer, and /gamemode, /tp, /time and more work. /help lists them",
                () -> c.commands, v -> c.commands = v);
        bool("Eye trainer: coaching", "Eye bug warnings, where two throws cross, whether they went to the same stronghold, and where the other two strongholds can be",
                () -> c.eyeCoaching, v -> c.eyeCoaching = v);
        bool("Eye trainer: eye angles", "The eye throw trainer says the angle each eye flew at, in Minecraft's own yaw (like F3 in later versions)",
                () -> c.eyeAngles, v -> c.eyeAngles = v);

        heading("End fight");
        bool("HUD", "The timer, dragon health, splits and last bed in the top left", () -> c.hud, v -> c.hud = v);
        bool("Bed report", "The chat report after every bed near the dragon", () -> c.bedReport, v -> c.bedReport = v);
        list("Bed report range", "Only beds this many blocks or closer to the dragon's head get a report",
                new int[]{8, 10, 12, 16, 20, 24, 32, 48}, () -> (int) c.bedReportRange, v -> c.bedReportRange = v);

        heading("Endgame and blaze");
        list("Blaze rods", "Blaze practice ends when you have this many rods (PBs are kept for each number)",
                new int[]{6, 7}, () -> c.blazeRods, v -> c.blazeRods = v);
        bool("Blaze: tower crafts", "For 1.0 tower runs: blaze practice also needs a pressure plate, a fence gate, 3 ladders, 3 nether brick fences and a boat",
                () -> c.blazeTowerCrafts, v -> c.blazeTowerCrafts = v);
        choice("Pearl luck", "Average: 1 pearl every 2 endermen. Lucky: 2 every 3. Vanilla: random 0 or 1 each",
                new String[]{"average", "lucky", "vanilla"}, () -> c.pearlLuck, v -> c.pearlLuck = v);
        time("Tower build starts at", "In-game time the tower build starts at (8:30 by default)",
                () -> c.towerStartTime, v -> c.towerStartTime = v);
        time("Night starts at", "The tower build counts down to this (11:00 by default)", () -> c.nightTime, v -> c.nightTime = v);
        time("Stronghold starts at", "In-game time stronghold attempts start at (18:45, the end of the night, by default)",
                () -> c.strongholdStartTime, v -> c.strongholdStartTime = v);

        heading("Kits");
        action("Reset tower kit", "Back to the default tower build kit", () -> c.towerKit = d.towerKit);
        action("Reset stronghold kit", "Back to the default stronghold kit", () -> c.strongholdKit = d.strongholdKit);
        action("Reset blaze kit", "Back to the default blaze kit", () -> c.blazeKit = d.blazeKit);
        action("Reset End kit", "Back to the default End fight kit", () -> c.kit = d.kit);
    }

    @Override
    public void init() {
        this.buttons.clear();
        waitingForKey = null;
        this.list = new SettingsList();
        this.buttons.add(new ButtonWidget(DONE, this.width / 2 - 100, this.height - 28, "Done"));
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) return;
        if (button.id == DONE) {
            c.save();
            EndPractice.cfg = c;
            this.minecraft.openScreen(parent);
        }
    }

    private void rowClicked(int index) {
        Entry e = entries.get(index);
        if (e.isHeading()) return;
        boolean shift = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (e.isKey() && shift) {
            waitingForKey = null;
            e.setKey(Keyboard.KEY_NONE);
        } else if (e.isKey()) {
            waitingForKey = e;
        } else {
            waitingForKey = null;
            e.click(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));
        }
    }

    @Override
    protected void keyPressed(char chr, int key) {
        if (waitingForKey != null) {
            // Escape cancels, Delete or Backspace turns the key off
            if (key != Keyboard.KEY_ESCAPE) {
                waitingForKey.setKey(key == Keyboard.KEY_DELETE || key == Keyboard.KEY_BACK ? Keyboard.KEY_NONE : key);
            }
            waitingForKey = null;
            return;
        }
        // Escape leaves without saving
        if (key == Keyboard.KEY_ESCAPE) this.minecraft.openScreen(parent);
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        list.render(mouseX, mouseY, tickDelta);
        this.drawCenteredString(this.textRenderer, lab ? "Endgame Lab" : "OldschoolPrac Settings", this.width / 2, 6, 0xFFFFFF);
        String help = null;
        if (mouseY >= TOP && mouseY <= this.height - BOTTOM) {
            int i = list.getEntryAt(mouseX, mouseY);
            if (i >= 0) help = entries.get(i).help();
        }
        if (waitingForKey != null) help = "Press a key. Escape cancels, Delete or Backspace sets it to NONE";
        if (help != null) {
            List<String> lines = wrap(help, this.width - 16);
            for (int l = 0; l < lines.size() && l < 2; l++) {
                this.drawCenteredString(this.textRenderer, lines.get(l), this.width / 2, 18 + l * 10, 0xA0A0A0);
            }
        }
        super.render(mouseX, mouseY, tickDelta);
    }

    private List<String> wrap(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && this.textRenderer.getWidth(next) > maxWidth) {
                lines.add(line);
                line = word;
            } else {
                line = next;
            }
        }
        if (!line.isEmpty()) lines.add(line);
        return lines;
    }

    private static final int TOP = 40, BOTTOM = 36, ROW = 24;
    private SettingsList list;

    /** The same scrolling list as Select World: mouse wheel, dragging, or the scrollbar on the right. */
    private final class SettingsList extends ListWidget {
        SettingsList() {
            super(SettingsScreen.this.minecraft, SettingsScreen.this.width, SettingsScreen.this.height, TOP, SettingsScreen.this.height - BOTTOM, ROW);
            setRenderSelectionHighlight(false);
        }

        @Override
        protected int size() {
            return entries.size();
        }

        @Override
        protected void entryClicked(int index, boolean doubleClick) {
            rowClicked(index);
        }

        @Override
        protected boolean isEntrySelected(int index) {
            return false;
        }

        @Override
        protected void renderBackground() {
            SettingsScreen.this.renderBackground();
        }

        @Override
        protected void renderEntry(int index, int x, int y, int height, Tesselator tesselator) {
            Entry e = entries.get(index);
            int left = SettingsScreen.this.width / 2 - 110;
            if (e.isHeading()) {
                SettingsScreen.this.drawCenteredString(SettingsScreen.this.textRenderer, e.name, SettingsScreen.this.width / 2, y + 8, 0xFFFFFF);
                return;
            }
            String label = e == waitingForKey ? "> " + e.name + " <" : e.label();
            ButtonWidget b = new ButtonWidget(-1, left, y - 2, 220, 20, label);
            boolean inside = this.mouseY >= TOP && this.mouseY <= SettingsScreen.this.height - BOTTOM;
            b.render(SettingsScreen.this.minecraft, this.mouseX, inside ? this.mouseY : -100);
        }
    }

    // ---- entries

    private interface Get { int get(); }
    private interface Set { void set(int v); }
    private interface GetL { long get(); }
    private interface SetL { void set(long v); }
    private interface GetB { boolean get(); }
    private interface SetB { void set(boolean v); }
    private interface GetS { String get(); }
    private interface SetS { void set(String v); }

    private abstract static class Entry {
        final String name;
        Entry(String name) { this.name = name; }
        abstract String label();
        boolean isHeading() { return false; }
        String help() { return null; }
        void click(boolean back) {}
        boolean isKey() { return false; }
        void setKey(int key) {}
    }

    private void heading(String name) {
        entries.add(new Entry(name) {
            String label() { return name; }
            boolean isHeading() { return true; }
        });
    }

    private void bool(String name, String help, GetB get, SetB set) {
        entries.add(new Entry(name) {
            String label() { return name + ": " + (get.get() ? "ON" : "OFF"); }
            String help() { return help; }
            void click(boolean back) { set.set(!get.get()); }
        });
    }

    private void link(String name, String help, Runnable run) {
        entries.add(new Entry(name) {
            String label() { return name; }
            String help() { return help; }
            void click(boolean back) { run.run(); }
        });
    }

    private void action(String name, String help, Runnable run) {
        entries.add(new Entry(name) {
            boolean done;
            String label() { return done ? name + ": done" : name; }
            String help() { return help + ". Saved when you press Done"; }
            void click(boolean back) { run.run(); done = true; }
        });
    }

    // click goes to the next value, shift + click to the one before
    private static int step(int at, int n, boolean back) {
        return back ? (at - 1 + n) % n : (at + 1) % n;
    }

    private void list(String name, String help, int[] values, Get get, Set set) {
        entries.add(new Entry(name) {
            String label() { return name + ": " + get.get(); }
            String help() { return help + " (shift+click goes down)"; }
            void click(boolean back) {
                int cur = get.get(), at = -1;
                // the value itself, else the nearest one below it (the list needn't be sorted)
                for (int i = 0; i < values.length && at < 0; i++) if (values[i] == cur) at = i;
                for (int i = 0; i < values.length && at < 0; i++) if (values[i] <= cur && (at < 0 || values[i] > values[at])) at = i;
                if (at < 0) at = 0;
                set.set(values[step(at, values.length, back)]);
            }
        });
    }

    private void choice(String name, String help, String[] values, GetS get, SetS set) {
        entries.add(new Entry(name) {
            String label() { return name + ": " + get.get(); }
            String help() { return help; }
            void click(boolean back) {
                int at = 0;
                for (int i = 0; i < values.length; i++) if (values[i].equals(get.get())) at = i;
                set.set(values[step(at, values.length, back)]);
            }
        });
    }

    /** A world time, shown as the in-game time of a run (ticks / 20), in 15 second steps up to 20:00, or off. */
    private void time(String name, String help, GetL get, SetL set) {
        entries.add(new Entry(name) {
            String label() {
                long t = get.get();
                return name + ": " + (t < 0 ? "off" : (t / 1200) + ":" + String.format("%02d", t / 20 % 60));
            }
            String help() { return help + ". Click +0:15, shift+click -0:15"; }
            void click(boolean back) {
                long t = get.get();
                if (back) t = t < 0 ? 23700 : t - 300;
                else t = t < 0 ? 0 : t + 300;
                set.set(t >= 24000 || t < 0 ? -1 : t / 300 * 300);
            }
        });
    }

    private final List<Entry> keyEntries = new ArrayList<>();
    private final List<Get> keyGets = new ArrayList<>();

    /** Another key row (or a Minecraft control) on the same key, or null. */
    private String clash(Entry self, int k) {
        if (k == Keyboard.KEY_NONE) return null;
        for (int i = 0; i < keyEntries.size(); i++) {
            Entry o = keyEntries.get(i);
            if (o != self && keyGets.get(i).get() == k) return o.name;
        }
        if (this.minecraft != null && this.minecraft.options != null) {
            for (net.minecraft.client.options.KeyBinding b : this.minecraft.options.keyBindings) {
                if (b.keyCode == k) return "Minecraft's " + b.name.replace("key.", "") + " key";
            }
        }
        return null;
    }

    // click, then press the key. shift+click sets it to NONE
    private void key(String name, Get get, Set set) {
        Entry e = new Entry(name) {
            String label() {
                int k = get.get();
                String key = k == Keyboard.KEY_NONE ? "NONE" : Keyboard.getKeyName(k);
                return name + ": " + (clash(this, k) != null ? "\u00a7c" + key : key);
            }
            String help() {
                String other = clash(this, get.get());
                return other != null ? "\u00a7cAlso used by " + other + ".\u00a77 Click, then press a new key. Shift+click sets it to NONE"
                        : "Click, then press a key. Shift+click sets it to NONE (off)";
            }
            boolean isKey() { return true; }
            void setKey(int key) { set.set(key); }
        };
        entries.add(e);
        keyEntries.add(e);
        keyGets.add(get);
    }
}
