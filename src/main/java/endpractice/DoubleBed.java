package endpractice;

import endpractice.mixin.EnderDragonAccessor;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.mob.monster.boss.EnderDragonEntity;
import net.minecraft.entity.mob.player.PlayerEntity;

/**
 * Double beds: two beds on one charge, both counting in full.
 *
 * In 1.0 (the same as 1.7) every part of the dragon shares one invulnerable time. A hit makes it invulnerable for
 * 10 ticks (half a second), and a hit in that time only counts for what it does over the hit that started it. So the
 * second bed has to go off 10 or more ticks after the first. Every bed that hits also points the dragon at a spot
 * 5 blocks ahead and a little up, which slows it down. The dragon only drops about 0.6 blocks per block it flies, so
 * sent from 64 out it has to start 10 to 15 up to reach you at head height (96 out: about 20): from 50 up it flies over.
 */
public final class DoubleBed {
    private DoubleBed() {
    }

    /** Ticks two beds can be apart and still count as a pair. */
    static final int PAIR_TICKS = 25;
    /** The dragon's invulnerable time after a hit that a second hit only counts over the first. */
    static final int INVULNERABLE = 10;

    /** This session: pairs of beds, and the ones that were doubles (both counted, together more than one perfect bed: over 81). */
    static int pairs, doubles;
    static int bestDouble;
    /** The world time of the last bed that hit the dragon, for the HUD cue. */
    private static long lastHitTime = -1000;
    private static int lastHeight = -1;

    static void clear() {
        lastHitTime = -1000;
    }

    /** Sends the dragon at you from where you're looking, this many blocks up and 64 out. */
    static void charge(Minecraft mc, int height) {
        EnderDragonEntity d = EndPractice.dragon(mc);
        if (mc.world == null || mc.world.dimension.id != 1 || d == null || d.getHealth() <= 0) {
            EndPractice.chat(mc, "§cNo dragon here.§r /charge sends the dragon at you in the End fight.");
            return;
        }
        PlayerEntity p = mc.player;
        double feet = p.y - p.eyeHeight;
        float r = p.yaw * (float) Math.PI / 180.0F;
        double dx = -Math.sin(r), dz = Math.cos(r); // the way you look
        int out = EndPractice.cfg.doubleBedDistance;
        double x = p.x + dx * out, z = p.z + dz * out, y = Math.min(feet + height, 250);
        // the dragon's yaw for flying back at you: it flies along (sin yaw, -cos yaw)
        float yaw = (float) (180.0 - Math.atan2(-dx, -dz) * 180.0 / Math.PI);
        d.setPositionAndAngles(x, y, z, yaw, 0.0F);
        d.velocityX = -dx;
        d.velocityZ = -dz;
        d.velocityY = 0.0;
        ((EnderDragonAccessor) d).endpractice$setTarget(p);
        d.needsNewTarget = false;
        lastHeight = height;
        if (active) return;
        Commands.cheat(mc);
        EndPractice.chat(mc, "Dragon charging at you from " + height + " up, " + out + " out where you're looking. /charge " + height + " again repeats it.");
    }

    // ---- the double bed mode ----------------------------------------------------------------------------------------

    private static int countdownTicks() {
        return Math.max(1, EndPractice.cfg.doubleBedCountdown) * 20;
    }
    /** Ticks after the first bed that the rep waits for a second. */
    private static final int REP_AFTER = PAIR_TICKS + 20;
    private static final int GIVE_UP = 400;

    private static boolean active;
    private enum State { COUNTDOWN, INCOMING, AFTER }
    private static State state;
    private static long stateTime, firstHit;
    private static int rep, firstDealt;
    private static boolean repDone;
    private static String result;
    private static int allTimeBest = -1;
    private static int allTimePairs, allTimeDoubles;

    public static boolean isActive() {
        return active;
    }

    static void stop() {
        active = false;
    }

    /** A new End with no crystals: the dragon charges at you from where you look, again and again. */
    static void start(Minecraft mc) {
        EndPractice.log("starting double bed practice");
        active = false;
        BedReport.clear();
        EndPractice.newEnd(mc);
        for (Object o : mc.world.entities) {
            if (o instanceof net.minecraft.entity.EnderCrystalEntity) ((net.minecraft.entity.Entity) o).remove();
        }
        PlayerEntity p = mc.player;
        for (int x = -96; x <= 96; x += 16) {
            for (int z = -96; z <= 96; z += 16) mc.world.getBlock(x, 64, z);
        }
        spot = null;
        snapAt = null;
        newSpot(mc);
        kit(p, true);
        pairs = doubles = bestDouble = deaths = 0;
        rep = 0;
        result = null;
        lastHeight = EndPractice.cfg.chargeHeight;
        loadBest();
        active = true;
        countdown(mc);
        EndPractice.chat(mc, "§dDouble beds§r: the dragon charges at you from about " + lastHeight + " up, " + EndPractice.cfg.doubleBedDistance + " out the way you're looking. Bed 1 one block down in the ground as it comes in, "
                + "turn round, bed 2 behind you once the HUD says now. After each double you move to a new spot. " + Config.keyName(EndPractice.cfg.keyCharge) + " moves you to a new spot and starts the next one, /charge <height> changes the height.");
    }

    private static final java.util.Random random = new java.util.Random();
    /** Where you stand this rep. */
    private static int[] spot;
    private static boolean movePending;
    private static int deaths;
    private static int thisHeight;

    /**
     * Puts you on a fairly flat end stone spot on the island, away from the last one: a 7x7 of end stone within a block
     * of your height, no obsidian close by, open sky above, and the island 64 blocks out on most sides.
     */
    static void newSpot(Minecraft mc) {
        int[] best = null;
        double bestScore = Double.MAX_VALUE;
        for (int tries = 0; tries < 300; tries++) {
            double a = random.nextDouble() * Math.PI * 2, r = 8 + random.nextDouble() * 52;
            int x = (int) Math.floor(Math.cos(a) * r), z = (int) Math.floor(Math.sin(a) * r);
            if (spot != null && Math.hypot(x - spot[0], z - spot[2]) < 20) continue;
            int y = mc.world.getHeight(x, z);
            if (y <= 1 || mc.world.getBlock(x, y - 1, z) != net.minecraft.block.Block.END_STONE.id) continue;
            int bumps = 0;
            boolean bad = false;
            for (int dx = -3; dx <= 3 && !bad; dx++) {
                for (int dz = -3; dz <= 3 && !bad; dz++) {
                    int h = mc.world.getHeight(x + dx, z + dz);
                    if (Math.abs(h - y) > 1 || mc.world.getBlock(x + dx, h - 1, z + dz) != net.minecraft.block.Block.END_STONE.id) bad = true;
                    else if (h != y) bumps++;
                }
            }
            if (bad) continue;
            for (int dx = -5; dx <= 5 && !bad; dx++)
                for (int dz = -5; dz <= 5 && !bad; dz++)
                    for (int dy = 0; dy <= 20 && !bad; dy++)
                        if (mc.world.getBlock(x + dx, y + dy, z + dz) == net.minecraft.block.Block.OBSIDIAN.id
                                || mc.world.getBlock(x + dx, y - 1 - (dy % 3), z + dz) == net.minecraft.block.Block.OBSIDIAN.id) bad = true;
            if (bad) continue;
            double score = bumps + random.nextDouble() * 4;
            if (score < bestScore) {
                bestScore = score;
                best = new int[]{x, y, z};
            }
            if (tries > 60 && best != null) break;
        }
        if (best == null) best = CrystalTrainer.middle(mc);
        restore(mc);
        spot = best;
        snapshot(mc, best);
        PlayerEntity p = mc.player;
        // face the middle of the island, where most of the open sky is
        float yaw = (float) (Math.atan2(best[0], -best[2]) * 180.0 / Math.PI);
        p.setPositionAndAngles(best[0] + 0.5, best[1] + 0.01, best[2] + 0.5, yaw, 0.0F);
        p.velocityX = p.velocityY = p.velocityZ = 0.0;
        heal(p);
    }

    /** Full health and food, not on fire, no fall to land: every new spot and every rep. */
    private static void heal(PlayerEntity p) {
        if (p.getHealth() <= 0) return;
        p.setHealth(20);
        p.getHungerManager().add(20, 20.0F);
        p.extinguish();
        p.fallDistance = 0.0F;
    }

    /**
     * The double beds kit (iron pickaxe, a stack of cobblestone, 7 beds unless changed in the kit editor or with K twice).
     * Fresh: the whole kit. Otherwise every kit slot that's empty or holds the same item goes back to the kit's count.
     */
    /** In the mode, items that aren't in the kit aren't picked up. */
    public static boolean keepOut(net.minecraft.item.ItemStack stack) {
        if (!active || stack == null) return false;
        for (net.minecraft.item.ItemStack k : KitEditorScreen.parse(EndPractice.cfg.doubleBedKit)) {
            if (k != null && k.id == stack.id) return false;
        }
        return true;
    }

    /** Exactly the kit again, in its slots, everything else gone. Keeps the slot you have selected. */
    private static void resetKit(PlayerEntity p) {
        int sel = p.inventory.selectedSlot;
        EndPractice.giveKit(p, EndPractice.cfg.doubleBedKit);
        p.inventory.selectedSlot = sel;
    }

    private static void kit(PlayerEntity p, boolean fresh) {
        String kit = EndPractice.cfg.doubleBedKit;
        if (fresh) {
            EndPractice.giveKit(p, kit);
            for (int i = 0; i < 9; i++) {
                if (p.inventory.items[i] != null && p.inventory.items[i].id == 355) {
                    p.inventory.selectedSlot = i;
                    break;
                }
            }
            return;
        }
        net.minecraft.item.ItemStack[] items = p.inventory.items;
        net.minecraft.item.ItemStack[] want = KitEditorScreen.parse(kit);
        for (int i = 0; i < want.length && i < items.length; i++) {
            if (want[i] == null) continue;
            if (items[i] == null || items[i].id == want[i].id) items[i] = new net.minecraft.item.ItemStack(want[i].id, want[i].size, want[i].getMetadata());
        }
    }

    private static void countdown(Minecraft mc) {
        state = State.COUNTDOWN;
        stateTime = mc.world.getTime();
        repDone = false;
        EnderDragonEntity d = EndPractice.dragon(mc);
        if (d != null && d.getHealth() > 0) {
            d.setHealth(200);
            d.invulnerableTimer = 0;
        }
    }

    /** The mode key in the mode: a new spot (the old one put back), healed and restocked, and the next charge. */
    static void skip(Minecraft mc) {
        if (!active || mc.world == null || mc.player == null || mc.player.getHealth() <= 0) return;
        movePending = false;
        newSpot(mc);
        resetKit(mc.player);
        heal(mc.player);
        clear();
        countdown(mc);
        EndPractice.chat(mc, "New spot. Next charge in " + EndPractice.cfg.doubleBedCountdown + " s.");
    }

    /** /charge in the mode: charge now. */
    static void chargeNow(Minecraft mc, int height) {
        lastHeight = height;
        chargeNow(mc);
    }

    static void chargeNow(Minecraft mc) {
        if (!active || mc.world == null) return;
        go(mc);
    }

    private static void go(Minecraft mc) {
        EnderDragonEntity d = EndPractice.dragon(mc);
        if (d == null || d.getHealth() <= 0) return;
        d.setHealth(200);
        d.invulnerableTimer = 0;
        clear();
        rep++;
        int base = lastHeight > 0 ? lastHeight : EndPractice.cfg.chargeHeight;
        int spread = Math.max(0, EndPractice.cfg.doubleBedSpread);
        thisHeight = Math.max(4, base + random.nextInt(2 * spread + 1) - spread);
        int keep = lastHeight;
        charge(mc, thisHeight);
        lastHeight = keep;
        state = State.INCOMING;
        stateTime = mc.world.getTime();
        repDone = false;
    }

    static void tick(Minecraft mc) {
        if (!active || mc.player == null || mc.world == null) return;
        PlayerEntity p = mc.player;
        if (mc.world.dimension.id != 1) {
            // died with "can't be hurt" off, and respawned: go again
            if (p.getHealth() > 0) EndPractice.queueDoubleBeds();
            return;
        }
        if (p.getHealth() <= 0) {
            // can't be hurt is off and your own bed got you: back up on the spot, no death screen or new world
            p.setHealth(20);
            p.deathTicks = 0;
            ((endpractice.mixin.MobEntityAccessor) p).endpractice$setDead(false);
            if (mc.screen instanceof net.minecraft.client.gui.screen.DeathScreen) mc.openScreen(null);
            for (Object o : mc.world.entities) {
                if (o instanceof net.minecraft.entity.ItemEntity) ((net.minecraft.entity.Entity) o).remove(); // the dropped kit
            }
            kit(p, true);
            heal(p);
            deaths++;
            EndPractice.chat(mc, "§cYou died§r (" + deaths + (deaths == 1 ? " death" : " deaths") + " this session). Back up with your kit, next charge in " + EndPractice.cfg.doubleBedCountdown + " s.");
            result = "§cDied§r";
            if (state == State.AFTER) repDone = true;
            next(mc);
            return;
        }
        for (Object o : mc.world.entities) {
            if (o instanceof net.minecraft.entity.ItemEntity && keepOut(((net.minecraft.entity.ItemEntity) o).item)) ((net.minecraft.entity.Entity) o).remove();
        }
        p.abilities.invulnerable = EndPractice.cfg.doubleBedProtect; // the beds' own blasts don't end a rep
        p.getHungerManager().add(20, 20.0F);
        BedReport.tick(mc);
        EnderDragonEntity d = EndPractice.dragon(mc);
        long t = mc.world.getTime() - stateTime;
        switch (state) {
            case COUNTDOWN:
                if (t >= countdownTicks()) go(mc);
                break;
            case INCOMING:
                if (t > GIVE_UP) {
                    result = "§7No bed hit that charge§r";
                    EndPractice.chat(mc, "No bed hit that charge. Next one in " + EndPractice.cfg.doubleBedCountdown + " s.");
                    next(mc);
                }
                break;
            case AFTER:
                if (repDone || mc.world.getTime() - firstHit > REP_AFTER) {
                    if (!repDone) {
                        pairs++;
                        result = "Single " + firstDealt + ", no second bed";
                        EndPractice.chat(mc, "§7Rep " + rep + ": one bed (" + firstDealt + "), no second in time. Doubles " + doubles + " of " + pairs + "§r");
                        loadBest();
                        append(Math.max(0, firstDealt), 0, -1, false);
                        allTimePairs++;
                    }
                    next(mc);
                }
                break;
        }
        revive(d);
    }

    private static void next(Minecraft mc) {
        if (movePending || EndPractice.cfg.doubleBedMoveEveryRep) {
            movePending = false;
            newSpot(mc);
        }
        resetKit(mc.player);
        heal(mc.player);
        countdown(mc);
    }

    /** From BedReport, after a bed's damage is known: the first one of a rep. */
    static void dealt(int dealt) {
        if (active && state == State.AFTER && firstDealt < 0) firstDealt = dealt;
        if (active) revive(EndPractice.dragon(EndPractice.mc));
    }

    /** More beds than a rep needs can kill the dragon: it's put straight back to full health, with no new world. */
    private static void revive(EnderDragonEntity d) {
        if (d == null || (d.getHealth() > 0 && d.ticksSinceDeath == 0)) return;
        d.setHealth(200);
        d.ticksSinceDeath = 0;
        d.removed = false;
        ((endpractice.mixin.MobEntityAccessor) d).endpractice$setDead(false);
    }

    // ---- putting each spot back as it was -------------------------------------------------------------------------

    private static final int SNAP_R = 10, SNAP_DOWN = 6, SNAP_UP = 8;
    private static int[] snapAt;
    private static int[] snapIds, snapMetas;

    private static void snapshot(Minecraft mc, int[] at) {
        int n = (2 * SNAP_R + 1) * (2 * SNAP_R + 1) * (SNAP_DOWN + SNAP_UP + 1), i = 0;
        snapIds = new int[n];
        snapMetas = new int[n];
        for (int x = -SNAP_R; x <= SNAP_R; x++)
            for (int z = -SNAP_R; z <= SNAP_R; z++)
                for (int y = -SNAP_DOWN; y <= SNAP_UP; y++, i++) {
                    snapIds[i] = mc.world.getBlock(at[0] + x, at[1] + y, at[2] + z);
                    snapMetas[i] = mc.world.getBlockMetadata(at[0] + x, at[1] + y, at[2] + z);
                }
        snapAt = at;
    }

    /** Fills the holes, takes away the fire, cobblestone and leftover beds: the spot as it was when you got there. */
    private static void restore(Minecraft mc) {
        if (snapAt == null) return;
        int i = 0, fixed = 0;
        for (int x = -SNAP_R; x <= SNAP_R; x++)
            for (int z = -SNAP_R; z <= SNAP_R; z++)
                for (int y = -SNAP_DOWN; y <= SNAP_UP; y++, i++) {
                    int bx = snapAt[0] + x, by = snapAt[1] + y, bz = snapAt[2] + z;
                    if (by < 1 || by > 127) continue;
                    if (mc.world.getBlock(bx, by, bz) != snapIds[i] || mc.world.getBlockMetadata(bx, by, bz) != snapMetas[i]) {
                        mc.world.setBlockWithMetadata(bx, by, bz, snapIds[i], snapMetas[i]);
                        fixed++;
                    }
                }
        if (fixed > 0) EndPractice.log("double beds: put back " + fixed + " blocks at the last spot");
        snapAt = null;
    }


    /** From BedReport, as a bed goes off: remembers it for the HUD cue. */
    static void onHit(Minecraft mc, boolean wasInvulnerable) {
        if (wasInvulnerable) return;
        lastHitTime = mc.world.getTime();
        if (active && state == State.INCOMING) {
            state = State.AFTER;
            firstHit = mc.world.getTime();
            firstDealt = -1;
        }
    }

    /** One line about the charge, under the report of a bed that hit. */
    static String chargeLine(boolean inGround, double speed, double headUp) {
        String s = "§7Charge: " + f1(speed) + " blocks/tick, head " + (headUp < -0.25 ? f1(-headUp) + " below" : f1(Math.abs(headUp)) + " above") + " your feet";
        if (speed < 0.75) return s + (inGround ? ", in the end stone" : "") + ": slow enough for a double§r";
        return s + ": fast, it'll be a few blocks past you after 10 ticks, so put bed 2 where it's going§r";
    }

    /** The second bed of a pair: what the two did together. */
    static void pair(Minecraft mc, int gap, int first, int second, int secondWouldBe, boolean cut, int prevDamage) {
        if (active && (state != State.AFTER || repDone)) return; // a third bed, or one between reps
        pairs++;
        if (active) {
            repDone = true;
            movePending = true; // a new spot for the next one
        }
        int total = first + second;
        String head = "§dDouble§r " + first + " + " + second + " = §e" + total + "§r, " + gap + (gap == 1 ? " tick" : " ticks") + " apart (" + f2(gap / 20.0) + " s)";
        boolean good = !cut && gap >= INVULNERABLE && second > 0 && total > 81;
        if (good) {
            doubles++;
            if (total > bestDouble) bestDouble = total;
        }
        EndPractice.chat(mc, head);
        if (cut) {
            int lost = Math.max(0, secondWouldBe - second);
            EndPractice.chat(mc, "§cToo soon:§r the dragon was still invulnerable, so only what bed 2 did over bed 1's " + prevDamage
                    + " counted (" + second + " of " + secondWouldBe + ", lost " + lost + "). Wait " + Math.max(1, INVULNERABLE - gap) + " more "
                    + (INVULNERABLE - gap == 1 ? "tick" : "ticks") + ".");
        } else if (total <= 81) {
            EndPractice.chat(mc, "Both counted, but the dragon had moved off by bed 2: put it further back, where the head is going.");
        }
        EndPractice.chat(mc, "§7Doubles this session: " + doubles + " of " + pairs + (bestDouble > 0 ? ", best " + bestDouble : "") + "§r");
        if (active) {
            result = (good ? "§aDouble " : "§eDouble ") + total + "§r, " + gap + " ticks apart";
            loadBest();
            append(first, second, gap, cut);
            allTimePairs++;
            if (good) allTimeDoubles++;
            if (good && total > allTimeBest) {
                if (allTimeBest > 0) EndPractice.chat(mc, "§6New best double!§r " + total + " (was " + allTimeBest + ")");
                allTimeBest = total;
            }
        }
    }

    // ---- config/endpractice-doublebeds.txt: time,first,second,gap,cut ---------------------------------------------

    private static java.io.File file() {
        return new java.io.File(Minecraft.getWorkingDirectory(), "config/endpractice-doublebeds.txt");
    }

    private static void append(int first, int second, int gap, boolean cut) {
        try {
            java.io.File f = file();
            f.getParentFile().mkdirs();
            java.nio.file.Files.write(f.toPath(), (System.currentTimeMillis() + "," + first + "," + second + "," + gap + "," + cut + "\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            EndPractice.log("couldn't save the double: " + e);
        }
    }

    /** The best double ever: both beds counted and together over 81. -1 for none. */
    static int allTimeBest() {
        loadBest();
        return allTimeBest;
    }

    private static void loadBest() {
        if (allTimeBest >= 0) return;
        allTimeBest = 0;
        try {
            java.io.File f = file();
            if (!f.exists()) return;
            for (String line : java.nio.file.Files.readAllLines(f.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                String[] a = line.trim().split(",");
                if (a.length < 5) continue;
                int first = Integer.parseInt(a[1]), second = Integer.parseInt(a[2]), gap = Integer.parseInt(a[3]);
                allTimePairs++;
                if (!Boolean.parseBoolean(a[4]) && gap >= INVULNERABLE && second > 0 && first + second > 81) {
                    allTimeDoubles++;
                    allTimeBest = Math.max(allTimeBest, first + second);
                }
            }
        } catch (Exception e) {
            EndPractice.log("couldn't read the doubles: " + e);
        }
    }

    /** The Stats screen line. */
    static String statsLine() {
        loadBest();
        if (allTimePairs == 0) return "§dDouble beds§r   §7none yet§r";
        return "§dDouble beds§r   best §e" + allTimeBest + "§r   doubles " + allTimeDoubles + "/" + allTimePairs
                + " (" + Math.round(100.0 * allTimeDoubles / allTimePairs) + "%)";
    }

    static java.util.List<String> modeHud(Minecraft mc) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (!active || mc.world == null) return lines;
        lines.add("§dDouble beds§r  rep " + rep + "   from " + (state == State.COUNTDOWN ? "about " + lastHeight : String.valueOf(thisHeight)) + " up");
        lines.add("Doubles " + doubles + " / " + pairs + (bestDouble > 0 ? "   best " + bestDouble : "") + (allTimeBest > 0 ? "   §7ever " + allTimeBest + "§r" : ""));
        if (state == State.COUNTDOWN) {
            long left = countdownTicks() - (mc.world.getTime() - stateTime);
            lines.add("§eCharge in " + String.format(Locale.ROOT, "%.1f", Math.max(0, left) / 20.0) + " s§r: look where it should come from");
        } else if (state == State.INCOMING) {
            lines.add("§eIncoming§r");
        }
        String cue = hud(mc);
        if (cue != null) lines.add(cue);
        String last = BedReport.lastShort();
        if (last != null) lines.add(last);
        if (result != null) lines.add(result);
        return lines;
    }

    /** The HUD cue after a bed hits: when bed 2 will count in full. */
    static String hud(Minecraft mc) {
        if (!EndPractice.cfg.doubleBedCue || mc.world == null) return null;
        long since = mc.world.getTime() - lastHitTime;
        if (since < 0 || since > INVULNERABLE + 15) return null;
        EnderDragonEntity d = EndPractice.dragon(mc);
        if (d == null || d.getHealth() <= 0) return null;
        int wait = d.invulnerableTimer - d.invulnerableTicks / 2;
        if (wait > 0) return "§cBed 2: wait " + wait + (wait == 1 ? " tick" : " ticks") + "§r";
        return "§aBed 2: now§r";
    }

    private static String f1(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }

    private static String f2(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }
}
