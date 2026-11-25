package endpractice;

import endpractice.compat.RankedCompat;

import endpractice.compat.Versions;
import endpractice.mixin.IntCacheAccessor;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SurvivalInteractionManager;
import net.minecraft.entity.EnderCrystalEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.monster.boss.EnderDragonEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.mob.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

/**
 * End fight practice. Each attempt is a new world with a random seed (so a new island and tower layout), entered
 * through vanilla's own End portal code (Minecraft.changeDimension, which also builds the obsidian platform), with the
 * kit in your inventory. The fight is timed in game ticks from arriving to the dragon dying.
 */
public final class EndPractice {
    public static final String FOLDER = "EndPractice";
    public static final String VERSION = "1.3";

    public static Config cfg;
    public static Minecraft mc;
    /** Set while startGame runs for a practice world: the overworld's Building terrain is skipped, it is never played. */
    public static boolean skipOverworldPrep;

    private static boolean startQueued;
    private static Endgame.Mode endgameQueued;
    private static boolean startWasDown;
    private static final boolean[] endgameWasDown = new boolean[3];
    private static boolean kitWasDown;
    private static long kitFirstPress;

    // the current attempt
    private static boolean active;
    private static long startTime;
    private static long endTime = -1;
    private static String result;
    private static int beds;
    private static int bedDamage;

    private static long pb = -1;
    private static int attempts;
    private static final List<Long> sessionTimes = new ArrayList<>();

    private EndPractice() {
    }

    public static void init() {
        if (cfg != null) return;
        cfg = Config.load();
        pb = readPb();
    }

    /** The End fight PB in ticks, -1 for none. */
    static long endPb() {
        return pb;
    }

    public static void log(String s) {
        System.out.println("[OldschoolPrac] " + s);
    }

    public static boolean isActive() {
        return active;
    }

    /** A practice world of either mode is loaded or being made. */
    public static boolean inPracticeWorld() {
        return active || DoubleBed.isActive() || Endgame.isActive() || Endgame.preparing || CrystalTrainer.isActive() || BlazePractice.isActive() || EyeTrainer.isActive()
                || PitSandbox.isActive() || Overworld.isActive() || BlazePractice.preparing || skipOverworldPrep;
    }

    /** End fight tracking (bed report, dragon history) applies: End fight practice, or the End in an endgame run. */
    static boolean endFight() {
        return active || DoubleBed.isActive() || (mc != null && Endgame.inEndFight(mc));
    }

    private static boolean pitWasDown;
    private static boolean chargeWasDown;
    private static boolean crystalsQueued;
    private static boolean crystalsRandom;
    private static boolean crystalsWasDown;
    private static boolean randomCrystalsWasDown;

    private static boolean eyesQueued, eyesWasDown, guessWasDown;

    private static boolean sandboxQueued, sandboxWasDown, overworldQueued, overworldWasDown, pitMenuWasDown, endermanWasDown, hudWasDown;

    static void toggleHud(Minecraft mc) {
        cfg.hud = !cfg.hud;
        cfg.save();
        chat(mc, "§7HUD " + (cfg.hud ? "on" : "off") + " (" + Keyboard.getKeyName(cfg.keyHud) + " or /hud)§r");
    }

    /** Overworld practice, from the menu or its key. */
    public static void queueOverworld() {
        overworldQueued = true;
    }

    /** Pit sandbox, from the menu, its key or /sandbox new. */
    public static void queueSandbox() {
        sandboxQueued = true;
    }

    private static boolean doubleQueued;

    /** Double bed practice, from the menu or its key. */
    public static void queueDoubleBeds() {
        doubleQueued = true;
    }

    /** Eye throw trainer, from the menu or its key. */
    public static void queueEyes() {
        eyesQueued = true;
    }

    /** From the practice menu or the title screen key. */
    public static void queueCrystals() {
        queueCrystals(false);
    }

    /** Crystal aim trainer, or its random mode (one crystal at a time from random spots). */
    private static Boolean blazeQueued;
    private static boolean blazeOpenWasDown, blazeEnclosedWasDown;

    /** Blaze practice, from the menu or a key: enclosed or open spawner. */
    public static void queueBlaze(boolean enclosed) {
        blazeQueued = enclosed;
    }

    public static void queueCrystals(boolean random) {
        crystalsQueued = true;
        crystalsRandom = random;
    }

    /** From the practice menu. */
    public static void queueEndgame(Endgame.Mode m) {
        endgameQueued = m;
    }

    /** Title screen key for the endgame practice modes. */
    public static boolean queueEndgameKey(int key) {
        Endgame.Mode m = endgameModeFor(key);
        if (m == null) return false;
        endgameQueued = m;
        return true;
    }

    private static Endgame.Mode endgameModeFor(int key) {
        if (key == Keyboard.KEY_NONE) return null;
        if (key == cfg.keyEndgame) return Endgame.Mode.FULL;
        if (key == cfg.keyTower) return Endgame.Mode.TOWER;
        if (key == cfg.keyStronghold) return Endgame.Mode.STRONGHOLD;
        return null;
    }

    /** Title screen key. */
    public static void queueStart() {
        startQueued = true;
    }

    /** Start of every client tick. Keys are read here so they work while playing. */
    public static void tickHead(Minecraft mc) {
        EndPractice.mc = mc;
        init();
        if (RankedCompat.busy()) {
            rankedLock(mc);
            return;
        }
        rankedLocked = false;
        boolean down = isDown(cfg.keyStart);
        // in game the key only restarts practice, so it can never quit a normal world
        if (down && !startWasDown && mc.screen == null && active) startQueued = true;
        startWasDown = down;

        // endgame keys: in game they only work while an endgame attempt is going
        int[] keys = {cfg.keyEndgame, cfg.keyTower, cfg.keyStronghold};
        for (int i = 0; i < keys.length; i++) {
            boolean d = isDown(keys[i]);
            if (d && !endgameWasDown[i] && mc.screen == null && Endgame.isActive()) endgameQueued = endgameModeFor(keys[i]);
            endgameWasDown[i] = d;
        }

        boolean cd = isDown(cfg.keyCrystals);
        if (cd && !crystalsWasDown && mc.screen == null && CrystalTrainer.isActive()) queueCrystals(false);
        crystalsWasDown = cd;
        boolean rd = isDown(cfg.keyRandomCrystals);
        if (rd && !randomCrystalsWasDown && mc.screen == null && CrystalTrainer.isActive()) queueCrystals(true);
        randomCrystalsWasDown = rd;

        boolean bo = isDown(cfg.keyBlazeOpen), be = isDown(cfg.keyBlazeEnclosed);
        if (bo && !blazeOpenWasDown && mc.screen == null && BlazePractice.isActive()) queueBlaze(false);
        if (be && !blazeEnclosedWasDown && mc.screen == null && BlazePractice.isActive()) queueBlaze(true);
        blazeOpenWasDown = bo;
        blazeEnclosedWasDown = be;

        boolean ed = isDown(cfg.keyEyeTrainer);
        if (ed && !eyesWasDown && mc.screen == null && EyeTrainer.isActive()) queueEyes();
        eyesWasDown = ed;
        boolean gd = isDown(cfg.keyGuess);
        if (gd && !guessWasDown && mc.screen == null && EyeTrainer.isActive() && mc.player != null)
            EyeTrainer.guess(mc, (int) Math.floor(mc.player.x), (int) Math.floor(mc.player.z));
        guessWasDown = gd;
        boolean sd = isDown(cfg.keyPitSandbox);
        if (sd && !sandboxWasDown && mc.screen == null && PitSandbox.isActive()) queueSandbox();
        sandboxWasDown = sd;
        boolean od = isDown(cfg.keyOverworld);
        if (od && !overworldWasDown && mc.screen == null && Overworld.isActive()) queueOverworld();
        overworldWasDown = od;
        Commands.tick(mc);

        boolean pitDown = isDown(cfg.keyPitPlan);
        if (pitDown && !pitWasDown && mc.screen == null && (Endgame.showPitPlan() || PitSandbox.tracking(mc)) && mc.player != null) {
            PitPlanner.plan(mc, (int) Math.floor(mc.player.x), (int) Math.floor(mc.player.z), false);
            chat(mc, "Planning the pit from here...");
        }
        pitWasDown = pitDown;

        boolean hudDown = isDown(cfg.keyHud);
        if (hudDown && !hudWasDown && mc.screen == null && mc.player != null) toggleHud(mc);
        hudWasDown = hudDown;
        boolean menuDown = isDown(cfg.keyPitMenu);
        if (menuDown && !pitMenuWasDown && mc.screen == null && PitSandbox.isActive()) mc.openScreen(new PitMenuScreen());
        pitMenuWasDown = menuDown;
        boolean enderDown = isDown(cfg.keyEnderman);
        if (enderDown && !endermanWasDown && mc.screen == null && PitSandbox.tracking(mc)) {
            double[] at = PitSandbox.summonSpot(mc);
            if (PitSandbox.summon(mc, "Enderman", 1, at[0], at[1], at[2]) > 0) {
                Commands.cheat(mc);
                chat(mc, "§7Enderman at " + (int) Math.floor(at[0]) + " " + (int) Math.floor(at[1]) + " " + (int) Math.floor(at[2]) + "§r");
            }
        }
        endermanWasDown = enderDown;

        boolean chargeDown = isDown(cfg.keyCharge);
        if (chargeDown && !chargeWasDown && mc.screen == null && DoubleBed.isActive()) DoubleBed.skip(mc);
        chargeWasDown = chargeDown;

        boolean kitDown = isDown(cfg.keySaveKit);
        if (kitDown && !kitWasDown && mc.screen == null && (active || Endgame.isActive() || BlazePractice.isActive() || EyeTrainer.isActive() || Overworld.isActive() || DoubleBed.isActive()) && mc.player != null) {
            // two presses within a second, so a stray press never overwrites the kit
            long now = System.currentTimeMillis();
            if (now - kitFirstPress < 1000) {
                kitFirstPress = 0;
                saveKit(mc.player);
            } else {
                kitFirstPress = now;
                chat(mc, "Press " + Config.keyName(cfg.keySaveKit) + " again to save your inventory as the kit.");
            }
        }
        kitWasDown = kitDown;
    }

    private static boolean rankedLocked;

    /** Oldschool Ranked is queued, in a room or in a match: every practice mode stops and nothing of ours runs. */
    private static void rankedLock(Minecraft mc) {
        startQueued = crystalsQueued = eyesQueued = sandboxQueued = overworldQueued = doubleQueued = false;
        endgameQueued = null;
        blazeQueued = null;
        if (!rankedLocked) {
            rankedLocked = true;
            log("Oldschool Ranked is active: practice is off until it's done");
            abandonIfRunning();
            Endgame.stop();
            CrystalTrainer.stop();
            DoubleBed.stop();
            BlazePractice.stop();
            EyeTrainer.stop();
            PitSandbox.stop();
            Overworld.stop();
            PitPlanner.clear();
        }
        Commands.rankedLock(mc);
    }

    /** End of every client tick. */
    public static void tickTail(Minecraft mc) {
        if (RankedCompat.busy()) return;
        if (startQueued || blazeQueued != null || crystalsQueued || endgameQueued != null || eyesQueued || sandboxQueued || overworldQueued || doubleQueued) Commands.cheated = false;
        if (startQueued || blazeQueued != null || crystalsQueued || endgameQueued != null || eyesQueued || sandboxQueued || overworldQueued || doubleQueued) {
            PitSandbox.stop();
            Overworld.stop();
        }
        if (doubleQueued) {
            doubleQueued = false;
            overworldQueued = false;
            sandboxQueued = false;
            eyesQueued = false;
            startQueued = false;
            endgameQueued = null;
            crystalsQueued = false;
            blazeQueued = null;
            abandonIfRunning();
            Endgame.stop();
            CrystalTrainer.stop();
            BlazePractice.stop();
            EyeTrainer.stop();
            DoubleBed.start(mc);
            return;
        }
        if (overworldQueued) {
            overworldQueued = false;
            sandboxQueued = false;
            eyesQueued = false;
            startQueued = false;
            endgameQueued = null;
            crystalsQueued = false;
            blazeQueued = null;
            abandonIfRunning();
            Endgame.stop();
            CrystalTrainer.stop();
            DoubleBed.stop();
            BlazePractice.stop();
            EyeTrainer.stop();
            Overworld.start(mc);
            return;
        }
        if (sandboxQueued) {
            sandboxQueued = false;
            eyesQueued = false;
            startQueued = false;
            endgameQueued = null;
            crystalsQueued = false;
            blazeQueued = null;
            abandonIfRunning();
            Endgame.stop();
            CrystalTrainer.stop();
            DoubleBed.stop();
            BlazePractice.stop();
            EyeTrainer.stop();
            PitSandbox.start(mc);
            return;
        }
        if (eyesQueued) {
            eyesQueued = false;
            startQueued = false;
            endgameQueued = null;
            crystalsQueued = false;
            blazeQueued = null;
            abandonIfRunning();
            Endgame.stop();
            CrystalTrainer.stop();
            DoubleBed.stop();
            BlazePractice.stop();
            EyeTrainer.start(mc);
            return;
        }
        if (startQueued) {
            startQueued = false;
            EyeTrainer.stop();
            endgameQueued = null;
            crystalsQueued = false;
            blazeQueued = null;
            Endgame.stop();
            CrystalTrainer.stop();
            DoubleBed.stop();
            BlazePractice.stop();
            start(mc);
            return;
        }
        if (blazeQueued != null) {
            boolean enclosed = blazeQueued;
            blazeQueued = null;
            crystalsQueued = false;
            endgameQueued = null;
            abandonIfRunning();
            Endgame.stop();
            CrystalTrainer.stop();
            DoubleBed.stop();
            EyeTrainer.stop();
            BlazePractice.start(mc, enclosed);
            return;
        }
        if (crystalsQueued) {
            crystalsQueued = false;
            endgameQueued = null;
            abandonIfRunning();
            DoubleBed.stop();
            Endgame.stop();
            BlazePractice.stop();
            EyeTrainer.stop();
            CrystalTrainer.start(mc, crystalsRandom);
            return;
        }
        if (endgameQueued != null) {
            Endgame.Mode m = endgameQueued;
            endgameQueued = null;
            abandonIfRunning();
            CrystalTrainer.stop();
            DoubleBed.stop();
            BlazePractice.stop();
            EyeTrainer.stop();
            Endgame.start(mc, m);
            return;
        }
        if (mc.world == null) {
            // back on the title screen: whatever is opened next isn't a practice world
            abandonIfRunning();
            Endgame.stop();
            CrystalTrainer.stop();
            DoubleBed.stop();
            BlazePractice.stop();
            EyeTrainer.stop();
            PitSandbox.stop();
            Overworld.stop();
            return;
        }
        if (DoubleBed.isActive()) {
            DoubleBed.tick(mc);
            return;
        }
        if (Overworld.isActive()) {
            Overworld.tick(mc);
            return;
        }
        if (PitSandbox.isActive()) {
            PitSandbox.tick(mc);
            long so = System.nanoTime();
            SpawnOverlay.tick(mc);
            PitSandbox.prof[2] += System.nanoTime() - so;
            return;
        }
        if (EyeTrainer.isActive()) {
            EyeTrainer.tick(mc);
            return;
        }
        if (BlazePractice.isActive()) {
            BlazePractice.tick(mc);
            return;
        }
        if (CrystalTrainer.isActive()) {
            CrystalTrainer.tick(mc);
            return;
        }
        if (Endgame.isActive()) {
            SpawnOverlay.tick(mc);
            Endgame.tick(mc);
            return;
        }
        if (!active && !inPracticeWorld()) {
            // a normal world: /test, /pit and the overlays (when on) still work
            PitSandbox.tick(mc);
            return;
        }
        if (!active || mc.player == null) return;
        if (mc.world.dimension.id != 1) {
            // died, or left through the exit portal
            if (endTime < 0) finish(mc, null, "Left the End");
            return;
        }
        BedReport.tick(mc);
        if (endTime >= 0) return;
        if (mc.player.getHealth() <= 0) {
            finish(mc, null, "You died");
            return;
        }
        EnderDragonEntity dragon = dragon(mc);
        if (dragon != null && dragon.getHealth() <= 0) {
            finish(mc, dragon, null);
        }
    }

    static void start(Minecraft mc) {
        init();
        abandonIfRunning();
        attempts++;
        log("starting attempt " + attempts);
        BedReport.clear();
        active = false;
        newEnd(mc);

        PlayerEntity p = mc.player;
        giveKit(p, cfg.kit);
        p.setHealth(20);
        p.getHungerManager().add(20, 20.0F);
        p.velocityX = p.velocityY = p.velocityZ = 0.0;
        p.fallDistance = 0.0F;

        active = true;
        startTime = mc.world.getTime();
        endTime = -1;
        result = null;
        beds = 0;
        bedDamage = 0;
        chat(mc, "§dEnd fight§r attempt " + attempts + ". " + Config.keyName(cfg.keyStart) + " restarts, "
                + Config.keyName(cfg.keySaveKit) + " twice saves your inventory as the kit.");
    }

    /** Leaves the current world (saving it) and puts the player on the obsidian platform of a new practice End. */
    static void newEnd(Minecraft mc) {
        // leave the current world (saving it), then replace the practice world with a new one
        mc.setWorld(null);
        mc.getWorldStorageSource().delete(FOLDER);
        mc.interactionManager = new SurvivalInteractionManager(mc);
        resetBiomeArrayPool();
        skipOverworldPrep = true;
        try {
            // structures off: the End is the same either way, and the overworld you respawn in after dying then
            // never runs the stronghold search, which can crash in 1.0 (see resetBiomeArrayPool)
            mc.startGame(FOLDER, "End Practice", Versions.newWorldSettings(new Random().nextLong(), false));
        } finally {
            skipOverworldPrep = false;
        }
        // vanilla's End portal path: new End world, Building terrain around the platform, the platform itself
        mc.changeDimension(1);
        mc.openScreen(null);

    }

    /**
     * Vanilla 1.0 bug: BiomeSource.findBiome (used for the spawn point and strongholds when a world is created) loops
     * over the whole array IntCache hands it, not just the part it asked for. The rest of that pooled array still holds
     * whatever the last world's biome layers left there, and a leftover -1 crashes world creation. Making many worlds
     * in one session makes that likely, so each practice world starts with an empty pool, like the first world after
     * the game is launched.
     */
    static void resetBiomeArrayPool() {
        IntCacheAccessor.endpractice$setMaxSize(256);
        IntCacheAccessor.endpractice$setTcache(new ArrayList<int[]>());
        IntCacheAccessor.endpractice$setTallocated(new ArrayList<int[]>());
        IntCacheAccessor.endpractice$setCache(new ArrayList<int[]>());
        IntCacheAccessor.endpractice$setAllocated(new ArrayList<int[]>());
    }

    private static void finish(Minecraft mc, EnderDragonEntity dragon, String failure) {
        endTime = mc.world.getTime();
        long ticks = endTime - startTime;
        if (failure != null) {
            result = failure;
            appendOutcome(failure.equals("You died") ? "died" : "left", ticks);
            chat(mc, "§c" + failure + "§r after " + time(ticks) + ". " + Config.keyName(cfg.keyStart) + " for a new attempt.");
            return;
        }
        if (Commands.cheated) {
            result = "Killed in " + time(ticks);
            chat(mc, "§aDragon killed in " + time(ticks) + "§r §7(not saved: commands were used)§r. " + Config.keyName(cfg.keyStart) + " for a new attempt.");
            return;
        }
        sessionTimes.add(ticks);
        appendTime(ticks);
        appendOutcome("kill", ticks);
        boolean newPb = pb < 0 || ticks < pb;
        String line = "§aDragon killed in " + time(ticks) + "§r";
        if (newPb) {
            line += " §6New PB!§r" + (pb >= 0 ? " (was " + time(pb) + ")" : "");
            pb = ticks;
        } else {
            line += " (PB " + time(pb) + ")";
        }
        result = "Killed in " + time(ticks);
        chat(mc, line);
        chat(mc, beds + " bed" + (beds == 1 ? "" : "s") + " for " + bedDamage + " damage. Session average "
                + time(average()) + " over " + sessionTimes.size() + ". " + Config.keyName(cfg.keyStart) + " for the next one.");
        long[] st = endFightStats();
        if (st[0] > 0) chat(mc, "All time: average §e" + time(st[1]) + "§r over " + st[0] + " kills, last 10 §e" + time(st[2])
                + "§r, completed " + st[0] + "/" + st[3] + " (" + Math.round(100.0 * st[0] / Math.max(1, st[3])) + "%).");
    }

    /** An End fight left unfinished (restarted, another mode, or back to the title screen) counts as a reset. */
    static void abandonIfRunning() {
        if (active && endTime < 0 && mc != null && mc.world != null) appendOutcome("reset", mc.world.getTime() - startTime);
        active = false;
    }

    // ---- End fight outcomes, config/endpractice-endfights.txt: time,outcome,ticks (outcome kill, died, left or reset) --

    private static File outcomesFile() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-endfights.txt");
    }

    private static void appendOutcome(String outcome, long ticks) {
        if (Commands.cheated) return;
        try {
            File f = outcomesFile();
            f.getParentFile().mkdirs();
            String line = System.currentTimeMillis() + "," + outcome + "," + ticks + "\n";
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log("couldn't save the outcome: " + e);
        }
    }

    /**
     * kills, average kill time, average of the last 10 kills (from every kill ever saved), and attempts (kills plus
     * the deaths, exits and resets saved since 0.8.6, so the completion rate starts counting then).
     */
    static long[] endFightStats() {
        List<Long> kills = new ArrayList<>();
        int tracked = 0, trackedKills = 0;
        try {
            File f = timesFile();
            if (f.isFile()) {
                for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                    String[] p = line.split(",");
                    if (p.length < 2) continue;
                    try {
                        kills.add(Long.parseLong(p[1].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            File o = outcomesFile();
            if (o.isFile()) {
                for (String line : Files.readAllLines(o.toPath(), StandardCharsets.UTF_8)) {
                    String[] p = line.split(",");
                    if (p.length < 3) continue;
                    tracked++;
                    if (p[1].trim().equals("kill")) trackedKills++;
                }
            }
        } catch (Exception e) {
            log("couldn't read the End fights: " + e);
        }
        long sum = 0, last = 0;
        for (long t : kills) sum += t;
        int n = kills.size(), from = Math.max(0, n - 10);
        for (int i = from; i < n; i++) last += kills.get(i);
        // kills from before the outcomes were saved count as completed attempts
        long attemptsAll = tracked + (n - trackedKills);
        return new long[]{n, n == 0 ? 0 : Math.round((double) sum / n), n == 0 ? 0 : Math.round((double) last / (n - from)), attemptsAll};
    }

    static void onBed(int damage) {
        if (!active || endTime >= 0) return;
        beds++;
        bedDamage += damage;
    }

    public static EnderDragonEntity dragon(Minecraft mc) {
        for (Object o : mc.world.entities) {
            if (o instanceof EnderDragonEntity) return (EnderDragonEntity) o;
        }
        return null;
    }

    public static int crystals(Minecraft mc) {
        int n = 0;
        for (Object o : mc.world.entities) {
            if (o instanceof EnderCrystalEntity && !((Entity) o).removed) n++;
        }
        return n;
    }

    /** Lines for the top left corner (F3 hides them). */
    /** Top right corner: the spawn overlay. */
    public static List<String> hudRight(Minecraft mc) {
        if (RankedCompat.busy()) return new ArrayList<>();
        return SpawnOverlay.hud(mc);
    }

    public static List<String> hud(Minecraft mc) {
        if (RankedCompat.busy()) return new ArrayList<>();
        if (Endgame.isActive()) return Endgame.hud(mc);
        if (CrystalTrainer.isActive()) return CrystalTrainer.hud(mc);
        if (BlazePractice.isActive()) return BlazePractice.hud(mc);
        if (EyeTrainer.isActive()) return EyeTrainer.hud(mc);
        if (PitSandbox.isActive()) return PitSandbox.hud(mc);
        if (Overworld.isActive()) return Overworld.hud(mc);
        if (DoubleBed.isActive()) return DoubleBed.modeHud(mc);
        if (!active && !inPracticeWorld()) return PitSandbox.hud(mc);
        List<String> lines = new ArrayList<>();
        if (!active || mc.world == null) return lines;
        long ticks = (endTime >= 0 ? endTime : mc.world.getTime()) - startTime;
        lines.add("§dEnd fight§r  " + time(ticks));
        if (result != null) lines.add(result);
        if (mc.world.dimension.id == 1) {
            EnderDragonEntity d = dragon(mc);
            if (d != null) lines.add("Dragon " + Math.max(0, d.getHealth()) + " / 200");
            lines.add("Crystals " + crystals(mc));
        }
        if (pb >= 0) lines.add("PB " + time(pb));
        String last = BedReport.lastShort();
        if (last != null) lines.add(last);
        String cue = DoubleBed.hud(mc);
        if (cue != null) lines.add(cue);
        return lines;
    }

    // ---- kit ------------------------------------------------------------------------------------------------------

    static void giveKit(PlayerEntity p, String kit) {
        PlayerInventory inv = p.inventory;
        for (int i = 0; i < inv.items.length; i++) inv.items[i] = null;
        for (int i = 0; i < inv.armor.length; i++) inv.armor[i] = null;
        for (String entry : kit.split(",")) {
            String[] f = entry.trim().split(":");
            if (f.length < 3) continue;
            try {
                int slot = Integer.parseInt(f[0].trim());
                int id = Integer.parseInt(f[1].trim());
                int count = Integer.parseInt(f[2].trim());
                int damage = f.length > 3 ? Integer.parseInt(f[3].trim()) : 0;
                ItemStack stack = new ItemStack(id, count, damage);
                if (slot >= 0 && slot < inv.items.length) inv.items[slot] = stack;
                else if (slot >= inv.items.length && slot < inv.items.length + inv.armor.length) inv.armor[slot - inv.items.length] = stack;
            } catch (RuntimeException e) {
                log("skipping kit entry '" + entry + "': " + e);
            }
        }
        inv.selectedSlot = 0;
    }

    private static void saveKit(PlayerEntity p) {
        PlayerInventory inv = p.inventory;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inv.items.length + inv.armor.length; i++) {
            ItemStack s = i < inv.items.length ? inv.items[i] : inv.armor[i - inv.items.length];
            if (s == null) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(i).append(':').append(s.id).append(':').append(s.size);
            if (s.getMetadata() != 0) sb.append(':').append(s.getMetadata());
        }
        String which;
        if (Endgame.isActive() && Endgame.mode() == Endgame.Mode.STRONGHOLD) {
            cfg.strongholdKit = sb.toString();
            which = "Stronghold kit saved. Every new stronghold attempt";
        } else if (DoubleBed.isActive()) {
            cfg.doubleBedKit = sb.toString();
            which = "Double beds kit saved. It's topped up to this every rep";
        } else if (Overworld.isActive()) {
            cfg.overworldKit = sb.toString();
            which = "Overworld kit saved. Every new overworld attempt";
        } else if (EyeTrainer.isActive()) {
            cfg.eyeKit = sb.toString();
            which = "Eye throw kit saved. Every new eye trainer attempt";
        } else if (BlazePractice.isActive()) {
            cfg.blazeKit = sb.toString();
            which = "Blaze kit saved. Every new blaze attempt";
        } else if (Endgame.isActive()) {
            cfg.towerKit = sb.toString();
            which = "Tower kit saved. Every new tower build";
        } else {
            cfg.kit = sb.toString();
            which = "Kit saved. Every new attempt";
        }
        cfg.save();
        chat(EndPractice.mc, which + " starts with this inventory.");
    }

    // ---- times ----------------------------------------------------------------------------------------------------

    private static File timesFile() {
        return new File(Minecraft.getWorkingDirectory(), "config/endpractice-times.txt");
    }

    private static void appendTime(long ticks) {
        if (Commands.cheated) return;
        try {
            File f = timesFile();
            f.getParentFile().mkdirs();
            String line = System.currentTimeMillis() + "," + ticks + "\n";
            Files.write(f.toPath(), line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log("couldn't save the time: " + e);
        }
    }

    private static long readPb() {
        long best = -1;
        try {
            File f = timesFile();
            if (!f.isFile()) return -1;
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                long t = Long.parseLong(parts[1].trim());
                if (best < 0 || t < best) best = t;
            }
        } catch (Exception e) {
            log("couldn't read the times: " + e);
        }
        return best;
    }

    private static long average() {
        long sum = 0;
        for (long t : sessionTimes) sum += t;
        return sessionTimes.isEmpty() ? 0 : Math.round((double) sum / sessionTimes.size());
    }

    /** Game ticks as m:ss.SS (one tick is 0.05 s). */
    public static String time(long ticks) {
        long hundredths = ticks * 5;
        long m = hundredths / 6000;
        long s = hundredths / 100 % 60;
        long h = hundredths % 100;
        return String.format(Locale.ROOT, "%d:%02d.%02d", m, s, h);
    }

    static void chat(Minecraft mc, String s) {
        log("chat: " + s.replaceAll("\u00a7.", ""));
        if (mc != null && mc.gui != null) mc.gui.addChatMessage(s);
    }

    private static boolean isDown(int key) {
        return key != Keyboard.KEY_NONE && Keyboard.isKeyDown(key);
    }
}
