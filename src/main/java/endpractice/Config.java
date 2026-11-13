package endpractice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

/** config/endpractice.properties, written with the defaults on first start. */
public final class Config {
    /** Starts a new attempt: on the title screen, or anywhere in game. */
    public int keyStart = Keyboard.KEY_P;
    /** Saves your current inventory as the kit for every later attempt. */
    public int keySaveKit = Keyboard.KEY_K;
    /** slot:itemId:count[:damage], comma separated. Slots 0-8 are the hotbar, 9-35 the inventory, 36-39 armor (boots first). */
    public String kit = "0:267:1,1:261:1,2:355:1,3:355:1,4:355:1,5:355:1,6:355:1,7:355:1,8:355:1,9:262:64,10:326:1,11:4:64,12:368:16,13:297:16,14:257:1";
    /** Endgame practice: tower build, stronghold, End, dragon. */
    public int keyEndgame = Keyboard.KEY_O;
    /** Endgame practice, tower build only. */
    public int keyTower = Keyboard.KEY_I;
    /** Endgame practice from the stronghold kit (eyes ready) to the dragon. */
    public int keyStronghold = Keyboard.KEY_L;
    /** Crystal aim trainer: a new End with no dragon, shoot every crystal. */
    public int keyCrystals = Keyboard.KEY_C;
    /** Crystal aim trainer, random mode: one crystal at a time from random spots, with a streak. */
    public int keyRandomCrystals = Keyboard.KEY_R;
    /** The mode keys on the title screen (off: start modes from the practice menu only; in game they still restart). */
    public boolean modeKeys = true;
    public int keyBlazeOpen = Keyboard.KEY_B;
    public int keyBlazeEnclosed = Keyboard.KEY_N;
    /** Eye throw trainer, and its guess key (guess the stronghold is where you stand). */
    public int keyEyeTrainer = Keyboard.KEY_Y;
    public int keyGuess = Keyboard.KEY_G;
    /** Eye throw trainer: say each eye's angle in chat. */
    public boolean eyeAngles = false;
    /** Eye throw trainer: after each eye, eye bug warnings, where the throws cross and where the other strongholds can be. */
    public boolean eyeCoaching = true;
    /** Singleplayer chat and commands (/gamemode and the rest). */
    public boolean commands = true;
    /**
     * Blaze practice: rods to collect, and the kit, what a run has at the spawner: stone tools, 3 stacks of leaves,
     * 48 oak planks, 5 gravel, 5 feathers, 3 string, 4 cobblestone, 16 steak, a crafting table and flint and steel.
     */
    public int blazeRods = 7;
    /** Blaze practice also needs the 1.0 tower crafts: a pressure plate, a fence gate, 3 ladders, 3 nether brick fences, a boat. */
    public boolean blazeTowerCrafts = false;
    /** Eye throw trainer: 16 eyes (kept topped up to that many), a stack of cobble to pillar with, and food. */
    public String eyeKit = "0:381:16,1:4:64,2:364:16";
    public String blazeKit = "0:272:1,1:274:1,2:275:1,3:273:1,4:18:64,5:364:16,6:259:1,7:58:1,"
            + "9:18:64,10:18:64,11:5:48,12:13:5,13:288:5,14:287:3,15:4:4";
    /** The blaze kit before 0.8.2 (no cobblestone), replaced by the new default when it was never changed. */
    private static final String OLD_BLAZE_KIT = "0:272:1,1:274:1,2:275:1,3:273:1,4:18:64,5:364:16,6:259:1,7:58:1,"
            + "9:18:64,10:18:64,11:5:48,12:13:5,13:288:5,14:287:3";
    /** Tower build kit. */
    public String towerKit = "0:112:17,1:18:64,2:113:4,3:333:1,4:107:1,5:81:4,6:72:1,7:326:1,"
            + "9:18:64,10:274:1,11:275:1,12:273:1,13:272:1,14:364:16,15:5:30,16:287:3,17:288:5,18:318:5,19:280:8,20:369:7,"
            + "21:35:21,22:18:64,23:58:1,24:259:1,25:65:3,26:3:3";
    /** Stronghold kit: what's left after the tower, with 14 eyes of ender. */
    public String strongholdKit = "0:4:16,1:18:64,3:333:1,7:326:1,8:381:14,"
            + "10:274:1,11:275:1,12:273:1,13:272:1,14:364:16,15:5:30,16:287:3,17:288:5,18:318:5,19:280:8,20:368:1,21:35:21,23:58:1,24:259:1";
    /** World time a tower build starts at, in ticks: 10200 is 8:30 into a run. -1 leaves it. */
    public long towerStartTime = 10200;
    /** World time night starts at: 13200 is 11:00 into a run. The tower build counts down to it. */
    public long nightTime = 13200;
    /** Endgame practice pearls: average (1 every 2 endermen), lucky (2 every 3) or vanilla (random 0 or 1). */
    public String pearlLuck = "average";
    /** World time stronghold attempts start at: 22500 is the end of the night, turning to day. */
    public long strongholdStartTime = 22500;
    /** Saves the endermen of every full endgame run (spawned, killed, pearls) and shows the averages. */
    public boolean endermanTracker = false;

    /** The tracker only runs with vanilla pearl luck, so the saved drop rates are real. */
    public boolean trackerOn() {
        return endermanTracker && "vanilla".equals(pearlLuck);
    }
    /** Surface endermen only count within this many blocks of you (87: as far as 1.0 draws them). 0 counts the whole loaded world. */
    public int endermanRange = 87;
    /** Draws every enderman's hitbox, through blocks, in practice worlds. */
    public boolean endermanOutline = false;
    /** A red dot above every enderman that's angry at you, seen through blocks, in practice worlds. */
    public boolean endermanAggroDot = false;
    /** HUD lines with the loaded monsters against the cap, the next spawn wave and the last waves, in practice worlds. */
    public boolean spawnOverlay = false;
    /** HUD line with how many of the loaded monsters despawned when you respawned on the tower. */
    public boolean flushOverlay = false;
    /** With the enderman tracker: count the death reset cycles each night and show the average. */
    public boolean cycleTracker = true;
    /** After the first death in the pit: count down to the first tick monsters can spawn on the surface. */
    public boolean respawnTimer = true;
    /** Endgame practice: plans the pit at pitDistance from the spawn tower and draws the line to it. */
    public boolean pitPlanner = true;
    public int pitDistance = 205;
    /** Plans the pit again from where you stand. */
    public int keyPitPlan = Keyboard.KEY_J;
    public boolean hud = true;
    public boolean bedReport = true;
    /** Shows the report for beds that did no damage to the dragon too, while it was within this many blocks. */
    public double bedReportRange = 16.0;

    /** Earlier default kits: a config still holding one of these gets the current default. */
    private static final String OLD_TOWER_KIT = "0:112:17,1:18:1,2:113:4,3:333:1,4:85:1,5:81:4,6:72:1,7:326:1," + "9:18:64,10:274:1,11:275:1,12:273:1,13:272:1,14:364:16,15:17:30,16:287:3,17:288:5,18:318:5,19:280:8,20:369:7,21:35:21";
    private static final String OLD_STRONGHOLD_KIT = "0:112:17,1:18:1,2:113:4,3:333:1,4:85:1,5:81:4,6:72:1,7:326:1,8:381:14," + "9:18:64,10:274:1,11:275:1,12:273:1,13:272:1,14:364:16,15:17:30,16:287:3,17:288:5,18:318:5,19:280:8,21:35:21";

    private static final String OLD_TOWER_KIT_021 = "0:112:17,1:18:64,2:113:4,3:333:1,4:85:1,5:81:4,6:72:1,7:326:1," + "9:18:64,10:274:1,11:275:1,12:273:1,13:272:1,14:364:16,15:5:30,16:287:3,17:288:5,18:318:5,19:280:8,20:369:7," + "21:35:21,22:18:64,23:58:1,24:259:1";
    private static final String OLD_STRONGHOLD_KIT_021 = "0:4:16,1:18:64,3:333:1,7:326:1,8:381:14," + "10:274:1,11:275:1,12:273:1,13:272:1,14:364:16,15:5:30,16:287:3,17:288:5,18:318:5,19:280:8,20:368:1,21:35:21,23:58:1,24:259:1";

    private File file;

    static Config load() {
        Config c = new Config();
        c.file = new File(Minecraft.getWorkingDirectory(), "config/endpractice.properties");
        Properties p = new Properties();
        if (c.file.isFile()) {
            try (InputStream in = new FileInputStream(c.file)) {
                p.load(in);
            } catch (Exception e) {
                EndPractice.log("couldn't read " + c.file + ": " + e);
            }
        }
        c.keyStart = key(p, "keyStart", c.keyStart);
        c.keySaveKit = key(p, "keySaveKit", c.keySaveKit);
        c.kit = p.getProperty("kit", c.kit).trim();
        c.keyEndgame = key(p, "keyEndgame", c.keyEndgame);
        c.keyTower = key(p, "keyTower", c.keyTower);
        c.keyStronghold = key(p, "keyStronghold", c.keyStronghold);
        c.keyCrystals = key(p, "keyCrystals", c.keyCrystals);
        c.keyRandomCrystals = key(p, "keyRandomCrystals", c.keyRandomCrystals);
        c.modeKeys = bool(p, "modeKeys", c.modeKeys);
        c.keyBlazeOpen = key(p, "keyBlazeOpen", c.keyBlazeOpen);
        c.keyBlazeEnclosed = key(p, "keyBlazeEnclosed", c.keyBlazeEnclosed);
        c.keyEyeTrainer = key(p, "keyEyeTrainer", c.keyEyeTrainer);
        c.keyGuess = key(p, "keyGuess", c.keyGuess);
        c.eyeAngles = bool(p, "eyeAngles", c.eyeAngles);
        c.eyeCoaching = bool(p, "eyeCoaching", c.eyeCoaching);
        c.commands = bool(p, "commands", c.commands);
        c.blazeRods = (int) num(p, "blazeRods", c.blazeRods);
        if (c.blazeRods != 6 && c.blazeRods != 7) c.blazeRods = 7;
        c.blazeTowerCrafts = bool(p, "blazeTowerCrafts", c.blazeTowerCrafts);
        c.blazeKit = p.getProperty("blazeKit", c.blazeKit).trim();
        c.eyeKit = p.getProperty("eyeKit", c.eyeKit).trim();
        if (c.blazeKit.equals(OLD_BLAZE_KIT)) c.blazeKit = new Config().blazeKit;
        c.towerKit = p.getProperty("towerKit", c.towerKit).trim();
        c.strongholdKit = p.getProperty("strongholdKit", c.strongholdKit).trim();
        if (c.towerKit.equals(OLD_TOWER_KIT) || c.towerKit.equals(OLD_TOWER_KIT_021)) c.towerKit = new Config().towerKit;
        if (c.strongholdKit.equals(OLD_STRONGHOLD_KIT) || c.strongholdKit.equals(OLD_STRONGHOLD_KIT_021)) c.strongholdKit = new Config().strongholdKit;
        c.towerStartTime = num(p, "towerStartTime", c.towerStartTime);
        c.nightTime = num(p, "nightTime", c.nightTime);
        c.pearlLuck = p.getProperty("pearlLuck", c.pearlLuck).trim().toLowerCase();
        c.strongholdStartTime = num(p, "strongholdStartTime", c.strongholdStartTime);
        c.endermanTracker = bool(p, "endermanTracker", c.endermanTracker);
        c.endermanRange = (int) num(p, "endermanRange", c.endermanRange);
        c.endermanOutline = bool(p, "endermanOutline", c.endermanOutline);
        c.endermanAggroDot = bool(p, "endermanAggroDot", c.endermanAggroDot);
        c.spawnOverlay = bool(p, "spawnOverlay", c.spawnOverlay);
        c.flushOverlay = bool(p, "flushOverlay", c.flushOverlay);
        c.cycleTracker = bool(p, "cycleTracker", c.cycleTracker);
        c.respawnTimer = bool(p, "respawnTimer", c.respawnTimer);
        c.pitPlanner = bool(p, "pitPlanner", c.pitPlanner);
        c.pitDistance = (int) num(p, "pitDistance", c.pitDistance);
        c.keyPitPlan = key(p, "keyPitPlan", c.keyPitPlan);
        c.hud = bool(p, "hud", c.hud);
        c.bedReport = bool(p, "bedReport", c.bedReport);
        try {
            c.bedReportRange = Double.parseDouble(p.getProperty("bedReportRange", String.valueOf(c.bedReportRange)).trim());
        } catch (NumberFormatException ignored) {
        }
        c.save();
        return c;
    }

    void save() {
        Properties p = new Properties();
        p.setProperty("keyStart", keyName(keyStart));
        p.setProperty("keySaveKit", keyName(keySaveKit));
        p.setProperty("kit", kit);
        p.setProperty("keyEndgame", keyName(keyEndgame));
        p.setProperty("keyTower", keyName(keyTower));
        p.setProperty("keyStronghold", keyName(keyStronghold));
        p.setProperty("keyCrystals", keyName(keyCrystals));
        p.setProperty("keyRandomCrystals", keyName(keyRandomCrystals));
        p.setProperty("modeKeys", String.valueOf(modeKeys));
        p.setProperty("keyBlazeOpen", keyName(keyBlazeOpen));
        p.setProperty("keyBlazeEnclosed", keyName(keyBlazeEnclosed));
        p.setProperty("keyEyeTrainer", keyName(keyEyeTrainer));
        p.setProperty("keyGuess", keyName(keyGuess));
        p.setProperty("eyeAngles", String.valueOf(eyeAngles));
        p.setProperty("eyeCoaching", String.valueOf(eyeCoaching));
        p.setProperty("commands", String.valueOf(commands));
        p.setProperty("blazeRods", String.valueOf(blazeRods));
        p.setProperty("blazeTowerCrafts", String.valueOf(blazeTowerCrafts));
        p.setProperty("blazeKit", blazeKit);
        p.setProperty("eyeKit", eyeKit);
        p.setProperty("towerKit", towerKit);
        p.setProperty("strongholdKit", strongholdKit);
        p.setProperty("towerStartTime", String.valueOf(towerStartTime));
        p.setProperty("nightTime", String.valueOf(nightTime));
        p.setProperty("pearlLuck", pearlLuck);
        p.setProperty("strongholdStartTime", String.valueOf(strongholdStartTime));
        p.setProperty("endermanTracker", String.valueOf(endermanTracker));
        p.setProperty("endermanRange", String.valueOf(endermanRange));
        p.setProperty("endermanOutline", String.valueOf(endermanOutline));
        p.setProperty("endermanAggroDot", String.valueOf(endermanAggroDot));
        p.setProperty("spawnOverlay", String.valueOf(spawnOverlay));
        p.setProperty("flushOverlay", String.valueOf(flushOverlay));
        p.setProperty("cycleTracker", String.valueOf(cycleTracker));
        p.setProperty("respawnTimer", String.valueOf(respawnTimer));
        p.setProperty("pitPlanner", String.valueOf(pitPlanner));
        p.setProperty("pitDistance", String.valueOf(pitDistance));
        p.setProperty("keyPitPlan", keyName(keyPitPlan));
        p.setProperty("hud", String.valueOf(hud));
        p.setProperty("bedReport", String.valueOf(bedReport));
        p.setProperty("bedReportRange", String.valueOf(bedReportRange));
        try {
            file.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(file)) {
                p.store(out, "OldschoolPrac. Keys use LWJGL names (P, K, F7, NONE). Restart the game after editing.\n"
                        + "kit: slot:itemId:count[:damage], comma separated. Slots 0-8 hotbar, 9-35 inventory, 36-39 armor.\n"
                        + "Easier: set up your inventory in a practice attempt and press keySaveKit.");
            }
        } catch (Exception e) {
            EndPractice.log("couldn't write " + file + ": " + e);
        }
    }

    private static long num(Properties p, String k, long def) {
        try {
            return Long.parseLong(p.getProperty(k, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static boolean bool(Properties p, String k, boolean def) {
        String v = p.getProperty(k);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static int key(Properties p, String k, int def) {
        String v = p.getProperty(k);
        if (v == null) return def;
        v = v.trim().toUpperCase();
        if (v.isEmpty() || v.equals("NONE")) return Keyboard.KEY_NONE;
        int idx = Keyboard.getKeyIndex(v);
        return idx == Keyboard.KEY_NONE ? def : idx;
    }

    static String keyName(int key) {
        return key == Keyboard.KEY_NONE ? "NONE" : Keyboard.getKeyName(key);
    }
}
