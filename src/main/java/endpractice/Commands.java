package endpractice;

import endpractice.mixin.WorldDataAccessor;
import net.minecraft.client.CreativeInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.SurvivalInteractionManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.screen.ChatScreen;

/**
 * Singleplayer chat and commands, since 1.0 only has chat in multiplayer. T opens chat, and a line starting with /
 * runs a command here instead of going nowhere.
 */
public final class Commands {
    /** A command that changes the game was used since the current practice attempt started: nothing it does is saved. */
    public static boolean cheated;

    private Commands() {
    }

    public static boolean spectating(Minecraft mc) {
        return mc != null && mc.interactionManager instanceof SpectatorInteractionManager;
    }

    public static boolean spectating() {
        return spectating(EndPractice.mc);
    }

    static void spectatorAbilities(PlayerEntity p) {
        p.abilities.canFly = true;
        p.abilities.flying = true;
        p.abilities.creativeMode = false;
        p.abilities.invulnerable = true;
    }

    /** Start of every client tick. */
    /** Ranked is active: out of spectator, normal brightness. */
    static void rankedLock(Minecraft mc) {
        if (mc.world == null || mc.player == null) {
            if (brightDimension != null) restoreBrightness();
            return;
        }
        if (spectating(mc)) {
            PlayerEntity p = mc.player;
            mc.interactionManager = new SurvivalInteractionManager(mc);
            CreativeInteractionManager.setSurvivalMode(p);
            p.noClip = false;
        }
        if (brightDimension != null) fullbright(mc, false);
    }

    static void tick(Minecraft mc) {
        if (mc.world == null || mc.player == null) {
            if (brightDimension != null) restoreBrightness();
            return;
        }
        if (EndPractice.cfg.commands && !mc.isMultiplayer() && mc.screen == null) {
            // vanilla only opens chat in multiplayer
            if (mc.options.chatKey.consumeClick()) mc.openScreen(new ChatScreen());
        }
        fullbright(mc, spectating(mc));
        if (spectating(mc)) {
            PlayerEntity p = mc.player;
            spectatorAbilities(p);
            p.noClip = true;
            p.fallDistance = 0.0F;
            p.onGround = false;
        } else if (mc.player.noClip) {
            mc.player.noClip = false;
        }
    }

    /** The dimension made full bright for spectator, and its own brightness to put back. */
    private static Object brightDimension;
    private static float[] savedBrightness;

    /** Spectators see everything lit, caves and strongholds included: every light level draws at full brightness. */
    private static void fullbright(Minecraft mc, boolean on) {
        float[] table = mc.world.dimension.brightnessTable;
        if (on && brightDimension != mc.world.dimension) {
            if (brightDimension != null) restoreBrightness(); // a new dimension: put the old one back first
            savedBrightness = table.clone();
            brightDimension = mc.world.dimension;
            java.util.Arrays.fill(table, 1.0F);
            if (mc.worldRenderer != null) mc.worldRenderer.reload();
        } else if (!on && brightDimension != null) {
            restoreBrightness();
            if (mc.worldRenderer != null) mc.worldRenderer.reload();
        }
    }

    private static void restoreBrightness() {
        if (brightDimension instanceof net.minecraft.world.dimension.Dimension && savedBrightness != null) {
            float[] t = ((net.minecraft.world.dimension.Dimension) brightDimension).brightnessTable;
            System.arraycopy(savedBrightness, 0, t, 0, Math.min(t.length, savedBrightness.length));
        }
        brightDimension = null;
        savedBrightness = null;
    }

    /** A chat line typed in singleplayer. */
    public static void onChat(Minecraft mc, String line) {
        if (endpractice.compat.RankedCompat.busy()) return; // ranked's chat, no commands in rooms or matches
        if (!line.startsWith("/")) {
            EndPractice.chat(mc, "<" + mc.player.name + "> " + line);
            return;
        }
        if (!EndPractice.cfg.commands) {
            EndPractice.chat(mc, "§cCommands are off in the settings.");
            return;
        }
        String[] a = line.substring(1).trim().split("\\s+");
        String cmd = a[0].toLowerCase();
        try {
            run(mc, cmd, a);
        } catch (NumberFormatException e) {
            EndPractice.chat(mc, "§cThat's not a number: " + e.getMessage().replace("For input string: ", ""));
        }
    }

    private static void run(Minecraft mc, String cmd, String[] a) {
        PlayerEntity p = mc.player;
        switch (cmd) {
            case "help": case "?":
                EndPractice.chat(mc, "§dCommands:§r /gamemode (/gm) s, c or sp, /tp x y z, /time set day|night|<ticks>, /time add <ticks>,");
                EndPractice.chat(mc, "/weather clear|rain|thunder, /give <id> [count] [damage], /heal, /kill, /pos, /seed, /guess x z and /keima <pixels> (eye trainer)");
                EndPractice.chat(mc, "/summon <mob> [count] [x y z], /killall [mob|monsters], /clear (inventory), /ws (world spawn), /time lock|unlock, /mobs, /charge [height] (sends the dragon at you), and in the pit sandbox or your own worlds /pit (save and load designs), /test <name> [minutes], /test stop, /tests, /sandbox on|off");
                return;
            case "gamemode": case "gm": case "gmode":
                if (a.length < 2) {
                    EndPractice.chat(mc, "You're in " + modeName(mc) + ". /gamemode s, c or sp");
                    return;
                }
                gamemode(mc, a[1].toLowerCase());
                return;
            case "charge": {
                int h = a.length >= 2 ? Integer.parseInt(a[1]) : EndPractice.cfg.chargeHeight;
                if (DoubleBed.isActive()) DoubleBed.chargeNow(mc, Math.max(0, Math.min(120, h)));
                else DoubleBed.charge(mc, Math.max(0, Math.min(120, h)));
                return;
            }
            case "gms": gamemode(mc, "s"); return;
            case "gmc": gamemode(mc, "c"); return;
            case "gmsp": case "spec": case "spectator": gamemode(mc, "sp"); return;
            case "tp": case "teleport": {
                if (a.length < 4) {
                    EndPractice.chat(mc, "§c/tp x y z§r (~ for where you are, ~5 for 5 from there)");
                    return;
                }
                double x = coord(a[1], p.x), y = coord(a[2], p.y - p.eyeHeight) - (a[2].startsWith("~") || a[2].contains(".") ? 0 : 0.5), z = coord(a[3], p.z);
                p.setPositionAndAngles(x, y, z, p.yaw, p.pitch); // it adds the eye height itself
                p.velocityX = p.velocityY = p.velocityZ = 0.0;
                p.fallDistance = 0.0F;
                cheat(mc);
                EndPractice.chat(mc, "Teleported to " + (int) Math.floor(x) + " " + (int) Math.floor(y) + " " + (int) Math.floor(z));
                return;
            }
            case "time": {
                if (a.length >= 3 && a[1].equalsIgnoreCase("set")) {
                    String v = a[2].toLowerCase();
                    long t = v.equals("day") ? 1000 : v.equals("noon") ? 6000 : v.equals("night") ? 13000 : v.equals("midnight") ? 18000
                            : Long.parseLong(v);
                    long now = mc.world.getTime();
                    mc.world.setTime(now - Math.floorMod(now, 24000L) + Math.floorMod(t, 24000L) + (Math.floorMod(t, 24000L) < Math.floorMod(now, 24000L) ? 24000 : 0));
                    if (PitSandbox.lockedTime >= 0) PitSandbox.lockedTime = Math.floorMod(t, 24000L);
                } else if (a.length >= 2 && (a[1].equalsIgnoreCase("lock") || a[1].equalsIgnoreCase("hold") || a[1].equalsIgnoreCase("freeze"))) {
                    if (a.length >= 3) run(mc, "time", new String[]{"time", "set", a[2]});
                    PitSandbox.lockedTime = Math.floorMod(mc.world.getTime(), 24000L);
                    cheat(mc);
                    EndPractice.chat(mc, "Time held at " + PitSandbox.lockedTime + ". /time unlock lets it run.");
                    return;
                } else if (a.length >= 2 && (a[1].equalsIgnoreCase("unlock") || a[1].equalsIgnoreCase("run") || a[1].equalsIgnoreCase("unfreeze"))) {
                    PitSandbox.lockedTime = -1;
                    EndPractice.chat(mc, "Time runs again.");
                    return;
                } else if (a.length >= 3 && a[1].equalsIgnoreCase("add")) {
                    mc.world.setTime(mc.world.getTime() + Long.parseLong(a[2]));
                    if (PitSandbox.lockedTime >= 0) PitSandbox.lockedTime = Math.floorMod(mc.world.getTime(), 24000L);
                } else {
                    long t = mc.world.getTime();
                    EndPractice.chat(mc, "World time " + t + " (" + Math.floorMod(t, 24000L) + " of the day). /time set day|night|<ticks>, /time add <ticks>, /time lock [time] or /time unlock");
                    return;
                }
                cheat(mc);
                EndPractice.chat(mc, "Time is now " + Math.floorMod(mc.world.getTime(), 24000L) + " of the day.");
                return;
            }
            case "weather": {
                String v = a.length >= 2 ? a[1].toLowerCase() : "";
                if (!v.equals("clear") && !v.equals("rain") && !v.equals("thunder")) {
                    EndPractice.chat(mc, "§c/weather clear, rain or thunder");
                    return;
                }
                mc.world.getData().setRaining(!v.equals("clear"));
                mc.world.getData().setThundering(v.equals("thunder"));
                mc.world.getData().setRainTime(12000);
                mc.world.getData().setThunderTime(12000);
                cheat(mc);
                EndPractice.chat(mc, "Weather set to " + v + ".");
                return;
            }
            case "give": case "i": case "item": {
                if (a.length < 2) {
                    EndPractice.chat(mc, "§c/give <id> [count] [damage]§r, like /give 381 12 for 12 eyes of ender");
                    return;
                }
                int id = Integer.parseInt(a[1]);
                int count = a.length >= 3 ? Integer.parseInt(a[2]) : 1;
                int damage = a.length >= 4 ? Integer.parseInt(a[3]) : 0;
                if (id <= 0 || id >= Item.BY_ID.length || Item.BY_ID[id] == null) {
                    EndPractice.chat(mc, "§cNo item with id " + id + ".");
                    return;
                }
                int left = Math.max(1, Math.min(count, 64 * 36));
                int max = Item.BY_ID[id].getMaxStackSize();
                while (left > 0) {
                    int n = Math.min(left, max);
                    if (!p.inventory.addItem(new ItemStack(id, n, damage))) break;
                    left -= n;
                }
                cheat(mc);
                String name;
                try {
                    ItemStack st = new ItemStack(id, 1, damage);
                    name = st.getItem().getName(st);
                } catch (RuntimeException e) {
                    name = null;
                }
                EndPractice.chat(mc, "Gave " + (count - left) + " of " + id + (name != null && !name.isEmpty() ? " (" + name + ")" : "") + ".");
                return;
            }
            case "heal":
                p.setHealth(20);
                p.getHungerManager().add(20, 20.0F);
                cheat(mc);
                EndPractice.chat(mc, "Healed.");
                return;
            case "kill":
                p.takeDamage(DamageSource.OUT_OF_WORLD, 1000);
                return;
            case "pos": case "where":
                EndPractice.chat(mc, "You're at " + (int) Math.floor(p.x) + " " + (int) Math.floor(p.y - p.eyeHeight) + " " + (int) Math.floor(p.z)
                        + ", facing " + String.format(java.util.Locale.ROOT, "%.1f", wrap(p.yaw)));
                return;
            case "seed":
                EndPractice.chat(mc, "Seed: " + mc.world.getSeed());
                return;
            case "keima":
                if (!EyeTrainer.isActive() && !(a.length > 1 && (a[1].equalsIgnoreCase("reset") || a[1].equalsIgnoreCase("fixed")))) {
                    EndPractice.chat(mc, "§c/keima only works in the eye throw trainer§r (/keima reset and /keima fixed work anywhere).");
                    return;
                }
                if (a.length < 2) {
                    EndPractice.chat(mc, "§c/keima <pixels>§r (or your distance guess, 100 or more), §c/keima calibrate§r (5 guided tries), §c/keima reset§r, §c/keima fixed <n>§r. Distance is about " + EyeTrainer.keimaConstant() + " / pixels.");
                    return;
                }
                if (a[1].equalsIgnoreCase("calibrate")) {
                    EyeTrainer.startCalibration(mc);
                } else if (a[1].equalsIgnoreCase("reset")) {
                    EyeTrainer.resetCalibration(mc);
                } else if (a[1].equalsIgnoreCase("fixed")) {
                    if (a.length > 2) {
                        EndPractice.cfg.keimaFixed = Math.max(500, Math.min(20000, Integer.parseInt(a[2])));
                        EndPractice.cfg.save();
                    }
                    EndPractice.chat(mc, "Fixed Keima constant: " + EndPractice.cfg.keimaFixed + ".");
                } else {
                    EyeTrainer.keimaAnswer(mc, Double.parseDouble(a[1]));
                }
                return;
            case "guess":
                if (!EyeTrainer.isActive()) {
                    EndPractice.chat(mc, "§c/guess only works in the eye throw trainer.");
                    return;
                }
                if (a.length < 3) {
                    EndPractice.chat(mc, "§c/guess x z§r, where you think the stronghold is");
                    return;
                }
                EyeTrainer.guess(mc, (int) Math.floor(coord(a[1], p.x)), (int) Math.floor(coord(a[a.length - 1], p.z)));
                return;
            case "summon": case "mob": {
                if (a.length < 2 || a[1].equalsIgnoreCase("list")) {
                    EndPractice.chat(mc, "§c/summon <mob> [count] [x y z]§r, at the block you're looking at. Mobs: " + PitSandbox.mobList());
                    return;
                }
                String key = PitSandbox.mobKey(a[1]);
                if (key == null) {
                    EndPractice.chat(mc, "§cNo mob called " + a[1] + ".§r /summon list");
                    return;
                }
                int count = a.length >= 3 ? Math.max(1, Math.min(100, Integer.parseInt(a[2]))) : 1;
                double[] at = PitSandbox.summonSpot(mc);
                if (a.length >= 6) at = new double[]{coord(a[3], p.x), coord(a[4], p.y - p.eyeHeight) - (a[4].contains(".") || a[4].startsWith("~") ? 0 : 0.5), coord(a[5], p.z)};
                int n = PitSandbox.summon(mc, key, count, at[0], at[1], at[2]);
                cheat(mc);
                EndPractice.chat(mc, "Summoned " + n + " " + key + " at " + (int) Math.floor(at[0]) + " " + (int) Math.floor(at[1]) + " " + (int) Math.floor(at[2]) + ".");
                return;
            }
            case "hud":
                EndPractice.toggleHud(mc);
                return;
            case "clear": case "ci": {
                for (int i = 0; i < p.inventory.items.length; i++) p.inventory.items[i] = null;
                for (int i = 0; i < p.inventory.armor.length; i++) p.inventory.armor[i] = null;
                EndPractice.chat(mc, "Inventory cleared.");
                return;
            }
            case "ws": case "spawn": case "worldspawn": {
                // world spawn, on top of whatever's there (your tower), like respawning
                int x = mc.world.getData().getSpawnX(), z = mc.world.getData().getSpawnZ();
                int y = mc.world.getHeight(x, z);
                p.setPositionAndAngles(x + 0.5, y + p.eyeHeight + 0.01, z + 0.5, p.yaw, p.pitch);
                p.velocityX = p.velocityY = p.velocityZ = 0.0;
                p.fallDistance = 0.0F;
                cheat(mc);
                EndPractice.chat(mc, "Teleported to world spawn, " + x + " " + y + " " + z + ".");
                return;
            }
            case "killall": case "butcher": {
                String what = a.length >= 2 ? a[1].toLowerCase() : "all";
                int n = PitSandbox.killAll(mc, what);
                if (n < 0) {
                    EndPractice.chat(mc, "§cNo mob called " + a[1] + ".§r /killall, /killall monsters or /killall <mob>");
                    return;
                }
                cheat(mc);
                EndPractice.chat(mc, "Removed " + n + " mob" + (n == 1 ? "" : "s") + ".");
                return;
            }
            case "mobs": {
                java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
                for (Object o : mc.world.entities) {
                    if (!(o instanceof net.minecraft.entity.mob.MobEntity) || o instanceof PlayerEntity || ((net.minecraft.entity.Entity) o).removed) continue;
                    counts.merge(String.valueOf(net.minecraft.entity.Entities.getKey((net.minecraft.entity.Entity) o)), 1, Integer::sum);
                }
                EndPractice.chat(mc, counts.isEmpty() ? "No mobs loaded." : "Loaded: " + counts.toString().replace("{", "").replace("}", "").replace("=", " "));
                return;
            }
            case "test": {
                if (a.length >= 2 && (a[1].equalsIgnoreCase("stop") || a[1].equalsIgnoreCase("end"))) {
                    PitSandbox.endTest(mc, "stopped");
                    return;
                }
                if (a.length < 2 || a.length == 2 && a[1].matches("\\d+")) {
                    if (PitDesigns.current != null) {
                        PitSandbox.startTest(mc, PitDesigns.current, a.length == 2 ? Long.parseLong(a[1]) : 0);
                        return;
                    }
                    EndPractice.chat(mc, "§c/test <name> [minutes]§r times a pit design, /test stop ends it, /tests compares them.");
                    return;
                }
                long minutes = 0;
                int nameEnd = a.length;
                if (a.length >= 3 && a[a.length - 1].matches("\\d+")) {
                    minutes = Long.parseLong(a[a.length - 1]);
                    nameEnd = a.length - 1;
                }
                StringBuilder name = new StringBuilder();
                for (int i = 1; i < nameEnd; i++) name.append(i > 1 ? " " : "").append(a[i]);
                PitSandbox.startTest(mc, name.toString(), minutes);
                return;
            }
            case "pit": case "pits":
                if (!PitSandbox.tracking(mc)) {
                    EndPractice.chat(mc, "§c/pit works in the pit sandbox or a normal world, not in the practice modes.");
                    return;
                }
                PitDesigns.command(mc, a);
                return;
            case "tests": case "designs":
                // the stats page opens once chat has closed
                PitSandbox.openTests = true;
                return;
            case "sandbox": case "overlay": case "overlays": {
                Config c = EndPractice.cfg;
                String what = a.length >= 2 ? a[1].toLowerCase() : "";
                switch (what) {
                    case "paths": case "path": c.sbPaths = !c.sbPaths; break;
                    case "teleports": case "tp": c.sbTeleports = !c.sbTeleports; break;
                    case "spawns": c.sbSpawns = !c.sbSpawns; break;
                    case "spawnable": case "light": c.sbSpawnable = !c.sbSpawnable; break;
                    case "rings": case "ring": c.sbRings = !c.sbRings; break;
                    case "start": case "new": EndPractice.queueSandbox(); return;
                    case "save": case "saveworld":
                        if (a.length < 3) {
                            EndPractice.chat(mc, "§c/sandbox save <name>§r copies this sandbox world into your saves to open again");
                            return;
                        }
                        PitSandbox.saveWorld(mc, String.join(" ", java.util.Arrays.copyOfRange(a, 2, a.length)));
                        return;
                    case "on": c.sbInWorlds = true; break;
                    case "off": c.sbInWorlds = false; break;
                    default:
                        EndPractice.chat(mc, "Overlays: /sandbox paths " + onOff(c.sbPaths) + ", teleports " + onOff(c.sbTeleports) + ", spawns " + onOff(c.sbSpawns)
                                + ", spawnable " + onOff(c.sbSpawnable) + ", rings " + onOff(c.sbRings) + ". In normal worlds " + onOff(c.sbInWorlds)
                                + " (/sandbox on|off). /sandbox new for a new sandbox world.");
                        return;
                }
                if (!PitSandbox.isActive() && !what.equals("off")) c.sbInWorlds = true;
                c.save();
                if (!PitSandbox.isActive()) EndPractice.chat(mc, "Overlays in normal worlds " + onOff(c.sbInWorlds) + " (/sandbox on|off).");
                EndPractice.chat(mc, "Paths " + onOff(c.sbPaths) + ", teleports " + onOff(c.sbTeleports) + ", spawns " + onOff(c.sbSpawns)
                        + ", spawnable " + onOff(c.sbSpawnable) + ", rings " + onOff(c.sbRings));
                return;
            }
            default:
                EndPractice.chat(mc, "§cUnknown command /" + cmd + ".§r /help lists them.");
        }
    }

    private static double wrap(float yaw) {
        double y = yaw % 360;
        if (y >= 180) y -= 360;
        if (y < -180) y += 360;
        return y;
    }

    /** A number, or ~ / ~5 relative to where you are. */
    private static double coord(String s, double here) {
        if (s.startsWith("~")) return here + (s.length() > 1 ? Double.parseDouble(s.substring(1)) : 0);
        double v = Double.parseDouble(s);
        return s.contains(".") ? v : v + 0.5;
    }

    private static String onOff(boolean b) {
        return b ? "§aon§r" : "§7off§r";
    }

    static String modeName(Minecraft mc) {
        return spectating(mc) ? "spectator" : mc.interactionManager.creative ? "creative" : "survival";
    }

    static void gamemode(Minecraft mc, String m) {
        gamemode(mc, m, true);
    }

    static void gamemode(Minecraft mc, String m, boolean say) {
        PlayerEntity p = mc.player;
        String name;
        if (m.equals("s") || m.equals("0") || m.equals("survival")) {
            mc.interactionManager = new SurvivalInteractionManager(mc);
            CreativeInteractionManager.setSurvivalMode(p);
            p.noClip = false;
            setSaved(mc, 0);
            name = "survival";
        } else if (m.equals("c") || m.equals("1") || m.equals("creative")) {
            mc.interactionManager = new CreativeInteractionManager(mc);
            CreativeInteractionManager.setCreativeMode(p);
            p.noClip = false;
            setSaved(mc, 1);
            name = "creative";
        } else if (m.equals("sp") || m.equals("3") || m.equals("spectator") || m.equals("spec")) {
            mc.interactionManager = new SpectatorInteractionManager(mc);
            spectatorAbilities(p);
            name = "spectator";
        } else {
            EndPractice.chat(mc, "§c/gamemode s, c or sp§r (survival, creative or spectator)");
            return;
        }
        mc.interactionManager.initWorld(mc.world);
        if (!say) return;
        cheat(mc);
        EndPractice.chat(mc, "Game mode set to " + name + ".");
    }

    /** The world remembers survival or creative when you leave (spectator is saved as survival). */
    private static void setSaved(Minecraft mc, int mode) {
        try {
            ((WorldDataAccessor) mc.world.getData()).endpractice$setGameMode(mode);
        } catch (Exception e) {
            EndPractice.log("couldn't save the game mode: " + e);
        }
    }

    static void cheat(Minecraft mc) {
        if (!EndPractice.inPracticeWorld() || cheated || PitSandbox.isActive()) return;
        cheated = true;
        EndPractice.chat(mc, "§7Commands used, so this attempt's times and stats won't be saved.§r");
    }
}
