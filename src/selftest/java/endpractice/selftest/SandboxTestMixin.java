package endpractice.selftest;

import endpractice.Config;
import endpractice.EndPractice;
import endpractice.PitSandbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** -Dendpractice.sandbox=1: the pit sandbox, its commands and overlays, a pit test; =2: profiles each overlay. */
@Mixin(Minecraft.class)
public abstract class SandboxTestMixin {
    private static int sbMode = Integer.getInteger("endpractice.sandbox", 0);
    private static int sbTicks, wx, wy, wz;
    private static final java.util.Set<Object> enders = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private static int items(Minecraft mc) {
        int n = 0;
        for (Object o : mc.player.inventory.items) if (o != null) n++;
        return n;
    }
    private static boolean sbStarted;
    private static long frameNs, frames, lastNano, tickNs, ticksTimed, tickStart;

    @Inject(method = "tick", at = @At("HEAD"))
    private void sbTickHead(CallbackInfo ci) {
        tickStart = System.nanoTime();
    }

    private static void overlays(boolean paths, boolean tps, boolean spawns, boolean spawnable, boolean rings) {
        Config c = EndPractice.cfg;
        c.sbPaths = paths; c.sbTeleports = tps; c.sbSpawns = spawns; c.sbSpawnable = spawnable; c.sbRings = rings;
    }

    private static void report(String what) {
        long[] p = PitSandbox.prof;
        double f = Math.max(1, p[8]), t = Math.max(1, p[9]);
        System.out.println(String.format(java.util.Locale.ROOT,
                "[sandbox] PROF %-10s frame %.2f ms (%d frames) | per tick: sandbox %.3f planner %.3f spawnOverlay %.3f spawnableScan %.3f ms | per frame: spawnable %.3f rings %.3f paths %.3f tp+spawns %.3f ms",
                what, frameNs / 1e6 / Math.max(1, frames), frames, p[0] / 1e6 / t, p[1] / 1e6 / t, p[2] / 1e6 / t, p[3] / 1e6 / t,
                p[4] / 1e6 / f, p[5] / 1e6 / f, p[6] / 1e6 / f, p[7] / 1e6 / f));
        java.util.Arrays.fill(p, 0);
        System.out.println(String.format(java.util.Locale.ROOT, "[sandbox] TICK %-10s %.2f ms per client tick", what, tickNs / 1e6 / Math.max(1, ticksTimed)));
        frameNs = frames = tickNs = ticksTimed = 0;
    }

    @Inject(method = "runGame", at = @At("HEAD"), require = 0)
    private void sbFrame(CallbackInfo ci) {
        if (sbMode != 2 || !PitSandbox.isActive()) return;
        long now = System.nanoTime();
        if (lastNano > 0) {
            frameNs += now - lastNano;
            frames++;
        }
        lastNano = now;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void sandboxTest(CallbackInfo ci) {
        if (sbMode <= 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        sbTicks++;
        if (tickStart > 0 && PitSandbox.isActive()) {
            tickNs += System.nanoTime() - tickStart;
            ticksTimed++;
        }
        if (!sbStarted) {
            if (sbTicks > 40 && mc.screen instanceof TitleScreen) {
                EndPractice.cfg.sbSpawnable = true;
                EndPractice.queueSandbox();
                sbStarted = true;
                sbTicks = 0;
            }
            return;
        }
        if (sbMode == 3) {
            rankedCheck(mc);
            return;
        }
        if (sbMode == 4) {
            boatCheck(mc);
            return;
        }
        if (sbMode == 5) {
            funnelCheck(mc);
            return;
        }
        if (sbMode == 6) {
            funnelShots(mc);
            return;
        }
        if (sbMode == 7) {
            mixedCheck(mc);
            return;
        }
        if (sbMode == 8) {
            strafeCheck(mc);
            return;
        }
        if (sbMode == 9) {
            sprintCheck(mc);
            return;
        }
        if (mc.player == null || mc.world == null || !PitSandbox.isActive()) return;
        if (sbMode == 2) {
            // overlays one at a time, 200 ticks each, after the pit planner is done (600)
            String[] phase = {"none", "paths", "teleports", "spawns", "spawnable", "rings", "all", "survival"};
            if (sbTicks == 100) {
                mc.player.pitch = 20.0F;
                mc.player.sendChat("/summon enderman 10");
                mc.player.sendChat("/summon zombie 10");
            }
            if (sbTicks == 400) System.out.println("[sandbox] planner phase: " + EndPractice.hud(mc));
            if (sbTicks >= 400 && (sbTicks - 400) % 200 == 0) {
                int i = (sbTicks - 400) / 200;
                if (i > 0) report(phase[i - 1]);
                else { report("startup"); }
                if (i >= phase.length) {
                    System.out.println("[sandbox] DONE " + mc.fpsDebugInfo);
                    Runtime.getRuntime().halt(0);
                }
                String ph = phase[i];
                if (ph.equals("survival")) {
                    mc.player.sendChat("/gm s");
                    mc.player.sendChat("/summon zombie 30");
                    mc.player.abilities.invulnerable = true;
                    ph = "all";
                }
                overlays(ph.equals("paths") || ph.equals("all"), ph.equals("teleports") || ph.equals("all"), ph.equals("spawns") || ph.equals("all"),
                        ph.equals("spawnable") || ph.equals("all"), ph.equals("rings") || ph.equals("all"));
                System.out.println("[sandbox] fps before " + ph + ": " + mc.fpsDebugInfo + " entities " + mc.world.entities.size());
            }
            return;
        }
        if (sbTicks == 100) {
            System.out.println("[sandbox] READY creative=" + mc.interactionManager.creative + " " + EndPractice.hud(mc));
            mc.player.pitch = 20.0F;
            mc.player.sendChat("/help");
            mc.player.sendChat("/summon enderman 3");
            mc.player.sendChat("/summon zombie 2 ~6 ~ ~6");
            mc.player.sendChat("/summon pigman");
            mc.player.sendChat("/summon nothing");
            mc.player.sendChat("/mobs");
            mc.player.sendChat("/sandbox");
            mc.player.sendChat("/test flat ground 2");
        }
        if (sbTicks == 99) System.out.println("[sandbox] RESET before: pos " + (int) mc.player.x + " " + (int) mc.player.y + " " + (int) mc.player.z + " items " + items(mc));
        if (sbTicks == 106) System.out.println("[sandbox] RESET after /test: pos " + (int) mc.player.x + " " + (int) mc.player.y + " " + (int) mc.player.z + " items " + items(mc)
                + " tod " + Math.floorMod(mc.world.getTime(), 24000L) + " creative " + mc.interactionManager.creative + " spawn " + mc.world.getData().getSpawnX() + " " + mc.world.getData().getSpawnZ());
        if (sbTicks == 140) mc.player.sendChat("/summon zombie 4");
        if (sbTicks == 200) {
            int fx = (int) Math.floor(mc.player.x), fy = (int) Math.floor(mc.player.y - mc.player.eyeHeight + 0.01), fz = (int) Math.floor(mc.player.z);
            mc.world.setBlock(fx, fy - 1, fz, 20);
            mc.world.setBlock(fx + 1, fy - 3, fz, 89);
            mc.player.sendChat("/test stop");
            mc.player.sendChat("/pit save glass pit");
            mc.world.setBlock(fx, fy - 1, fz, 1);
            mc.world.setBlock(fx + 1, fy - 3, fz, 1);
            mc.player.sendChat("/tp ~ ~ ~");
            mc.player.sendChat("/pit load glass pit");
            System.out.println("[sandbox] PIT after load: " + mc.world.getBlock(fx, fy - 1, fz) + " " + mc.world.getBlock(fx + 1, fy - 3, fz) + " (want 20 89)");
            mc.player.sendChat("/pit list");
            mc.player.sendChat("/pit pos1");
            mc.player.sendChat("/tp ~5 ~3 ~5");
            mc.player.sendChat("/pit pos2");
            mc.player.sendChat("/test");
        }
        if (sbTicks > 200 && sbTicks < 960) {
            for (Object o : mc.world.entities) {
                if (!(o instanceof net.minecraft.entity.mob.monster.EndermanEntity) || !enders.add(o)) continue;
                net.minecraft.entity.Entity e = (net.minecraft.entity.Entity) o;
                int x = (int) Math.floor(e.x), z = (int) Math.floor(e.z);
                System.out.println("[sandbox] ENDER new at " + x + " " + String.format("%.2f", e.y) + " " + z + " dist " + Math.round(Math.sqrt((e.x - mc.player.x) * (e.x - mc.player.x) + (e.z - mc.player.z) * (e.z - mc.player.z)))
                        + " height " + mc.world.getHeight(x, z) + " surface " + PitSandbox.onSurface(mc, e, 87) + " hud " + EndPractice.hud(mc).get(1));
            }
        }
        if (sbTicks == 250) {
            int fx = (int) Math.floor(mc.player.x), fy = (int) Math.floor(mc.player.y - mc.player.eyeHeight + 0.01), fz = (int) Math.floor(mc.player.z);
            wx = fx; wy = fy; wz = fz;
            mc.world.setBlock(fx + 6, fy + 4, fz, 12);
            mc.world.setBlock(fx - 6, fy + 2, fz, 8);
            System.out.println("[sandbox] FLOW placed sand " + mc.world.getBlock(fx + 6, fy + 4, fz) + " water " + mc.world.getBlock(fx - 6, fy + 2, fz)
                    + " time " + mc.world.getTime() + " sky " + mc.world.getTimeOfDay(1.0F));
        }
        if (sbTicks == 260) {
            int fx = wx, fy = wy, fz = wz;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                mc.world.setBlock(fx + 12 + dx, fy + 4, fz + dz, 18);
                mc.world.setBlock(fx - 12 + dx, fy + 4, fz + dz, 1);
            }
            net.minecraft.entity.Entity a1 = net.minecraft.entity.Entities.createSilently("Enderman", mc.world);
            a1.setPositionAndAngles(fx + 12.5, mc.world.getHeight(fx + 12, fz) > fy + 4 ? fy : fy, fz + 0.5, 0, 0);
            net.minecraft.entity.Entity a2 = net.minecraft.entity.Entities.createSilently("Enderman", mc.world);
            a2.setPositionAndAngles(fx - 11.5, fy, fz + 0.5, 0, 0);
            net.minecraft.entity.Entity a3 = net.minecraft.entity.Entities.createSilently("Enderman", mc.world);
            a3.setPositionAndAngles(fx + 0.5, fy, fz + 20.5, 0, 0);
            System.out.println("[sandbox] SURFACE under leaves " + PitSandbox.onSurface(mc, a1, 87) + " (want true), under stone " + PitSandbox.onSurface(mc, a2, 87)
                    + " (want false), open " + PitSandbox.onSurface(mc, a3, 87) + " (want true); ground y " + fy + " block below " + mc.world.getBlock(fx, fy - 1, fz));
        }
        if (sbTicks == 330) {
            int below = -1;
            for (int y = wy + 4; y > wy - 10; y--) if (mc.world.getBlock(wx + 6, y, wz) == 12) { below = y; break; }
            int water = 0;
            for (int dx = -4; dx <= 4; dx++) for (int dy = -6; dy <= 2; dy++) for (int dz = -4; dz <= 4; dz++) {
                int id = mc.world.getBlock(wx - 6 + dx, wy + dy, wz + dz);
                if (id == 8 || id == 9) water++;
            }
            System.out.println("[sandbox] FLOW sand now at y " + below + " (placed at " + (wy + 4) + "), water blocks " + water + " (1 if it didn't flow), time "
                    + mc.world.getTime() + " sky " + mc.world.getTimeOfDay(1.0F) + " hud " + EndPractice.hud(mc).get(0));
        }
        if (sbTicks == 300) {
            System.out.println("[sandbox] PAUSE before " + EndPractice.hud(mc) + " creative " + mc.interactionManager.creative);
            mc.openScreen(new net.minecraft.client.gui.screen.GameMenuScreen());
        }
        if (sbTicks == 360) {
            System.out.println("[sandbox] PAUSE after 60 paused ticks (paused=" + mc.paused + ") " + EndPractice.hud(mc));
            mc.openScreen(null);
        }
        if (sbTicks == 400) mc.player.sendChat("/gamemode s");
        if (sbTicks == 590) mc.player.sendChat("/heal");
        if (sbTicks == 600) {
            System.out.println("[sandbox] HUD " + EndPractice.hud(mc));
            String keep = EndPractice.cfg.sbHud;
            EndPractice.cfg.sbHud = "light,-title,loaded,bogus";
            System.out.println("[sandbox] HUD custom " + EndPractice.hud(mc));
            EndPractice.cfg.sbHud = keep;
            System.out.println("[sandbox] SHOT1");
        }
        if (sbTicks == 960) {
            mc.player.sendChat("/time unlock");
            mc.player.sendChat("/time lock 14000");
            mc.player.sendChat("/test stop");
            mc.player.sendChat("/killall monsters");
            mc.player.sendChat("/mobs");
            mc.player.sendChat("/tests");
            System.out.println("[sandbox] after HUD " + EndPractice.hud(mc));
        }
        if (sbTicks == 970) {
            mc.player.sendChat("/tp ~20 ~ ~20");
            mc.player.sendChat("/ws");
            System.out.println("[sandbox] WS pos " + (int) mc.player.x + " " + (int) mc.player.y + " " + (int) mc.player.z);
            mc.player.sendChat("/gm c");
            mc.player.sendChat("/clear");
            System.out.println("[sandbox] CLEAR items " + items(mc));
            mc.player.respawn();
            System.out.println("[sandbox] CREATIVE RESPAWN items " + items(mc) + " (want 0) creative " + mc.interactionManager.creative);
        }
        if (sbTicks == 975) {
            mc.player.sendChat("/sandbox save selftest");
            java.io.File f = new java.io.File(Minecraft.getWorkingDirectory(), "saves/Pit_selftest");
            System.out.println("[sandbox] SAVEWORLD exists " + f.exists() + " level.dat " + new java.io.File(f, "level.dat").exists()
                    + " name " + mc.getWorldStorageSource().getData("Pit_selftest").getName());
        }
        if (sbTicks == 985) {
            EndPractice.cfg.sbHud = "test,kills,pearls,surface,spawned,title,-loaded,-light,planner";
            mc.openScreen(new endpractice.HudEditorScreen(null));
        }
        if (sbTicks == 1000) System.out.println("[sandbox] SHOT2");
        if (sbTicks == 1060) {
            System.out.println("[sandbox] DONE");
            Runtime.getRuntime().halt(0);
        }
    }

    /** =3: Oldschool Ranked in devmods/. Pretends ranked went into a private room mid sandbox, then back to idle. */
    private static void setRanked(String state) {
        try {
            Class<?> r = Class.forName("oldschoolranked.Ranked");
            Class<?> st = Class.forName("oldschoolranked.Ranked$State");
            r.getField("state").set(null, st.getField(state).get(null));
        } catch (Throwable t) {
            System.out.println("[sandbox] RANKED can't set state: " + t);
        }
    }

    private static void rankedCheck(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;
        if (sbTicks == 60) {
            mc.player.sendChat("/time set day");
            mc.player.sendChat("/tp ~ 80 ~");
            mc.player.pitch = 25.0F;
            mc.player.abilities.flying = true;
        }
        if (sbTicks == 95) System.out.println("[sandbox] SHOT2 block under spawn " + mc.world.getBlock(0, 63, 0) + " " + mc.world.getBlock(200, 63, -200) + " biome " + mc.world.getBiomeSource().getBiome(200, -200).name);
        if (sbTicks == 100) {
            System.out.println("[sandbox] RANKED installed " + endpractice.compat.RankedCompat.installed() + " busy " + endpractice.compat.RankedCompat.busy());
            mc.player.sendChat("/summon enderman 2");
            mc.player.sendChat("/sandbox on");
            mc.player.sendChat("/pit");
            System.out.println("[sandbox] RANKED before: sandbox " + PitSandbox.isActive() + " tracking " + PitSandbox.tracking(mc) + " hud " + EndPractice.hud(mc));
            mc.player.sendChat("/gm sp");
            System.out.println("[sandbox] RANKED spectating " + endpractice.Commands.spectating(mc));
            setRanked("ROOM");
        }
        if (sbTicks == 105) {
            String mode = mc.interactionManager.creative ? "creative" : endpractice.Commands.spectating(mc) ? "spectator" : "survival";
            mc.player.sendChat("/gm c");
            mc.player.sendChat("/ws");
            String after = mc.interactionManager.creative ? "creative" : endpractice.Commands.spectating(mc) ? "spectator" : "survival";
            System.out.println("[sandbox] RANKED room: sandbox " + PitSandbox.isActive() + " practice world " + EndPractice.inPracticeWorld() + " tracking " + PitSandbox.tracking(mc)
                    + " hud " + EndPractice.hud(mc) + " right " + EndPractice.hudRight(mc) + " mode " + mode + " after /gm c " + after + " (want false false false [] [] survival survival)");
            System.out.println("[sandbox] SHOT1");
        }
        if (sbTicks == 140) setRanked("IDLE");
        if (sbTicks == 145) {
            mc.player.sendChat("/gm c");
            System.out.println("[sandbox] RANKED idle again: tracking " + PitSandbox.tracking(mc) + " creative " + mc.interactionManager.creative + " (want true true)");
        }
        if (sbTicks == 150) {
            // both mods change blaze and enderman drops: make sure the two stack and still drop
            for (Object o : new java.util.ArrayList<Object>(mc.world.entities)) {
                if (o instanceof net.minecraft.entity.mob.monster.EndermanEntity) ((net.minecraft.entity.mob.MobEntity) o).takeDamage(net.minecraft.entity.damage.DamageSource.OUT_OF_WORLD, 1000);
            }
            net.minecraft.entity.mob.monster.BlazeEntity b = new net.minecraft.entity.mob.monster.BlazeEntity(mc.world);
            b.setPosition(mc.player.x + 2, mc.player.y, mc.player.z);
            mc.world.addEntity(b);
            b.takeDamage(net.minecraft.entity.damage.DamageSource.OUT_OF_WORLD, 1000);
            System.out.println("[sandbox] RANKED drops ran");
        }
        if (sbTicks == 160) {
            System.out.println("[sandbox] DONE");
            Runtime.getRuntime().halt(0);
        }
    }

    // =4: standing on a boat with mobs chasing you. Logs every jump a mob starts and what made it jump.
    private static net.minecraft.entity.vehicle.BoatEntity boat;
    private static double[] stand;
    private static final java.util.Map<String, Integer> jumps = new java.util.TreeMap<>();
    private static final java.util.Map<net.minecraft.entity.Entity, Boolean> wasGround = new java.util.HashMap<>();
    private static int ground;

    private static void boatSetup(Minecraft mc, int raise) {
        for (Object o : new java.util.ArrayList<Object>(mc.world.entities)) if (o != mc.player) ((net.minecraft.entity.Entity) o).remove();
        int px = (int) Math.floor(mc.player.x) + (raise > 0 ? 40 : 0), pz = (int) Math.floor(mc.player.z);
        // a flat 21x21 grass floor with air above, so the terrain plays no part
        int gy = mc.world.getHeight(px, pz);
        for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
            for (int y = gy - 3; y < gy; y++) mc.world.setBlock(px + dx, y, pz + dz, 2);
            for (int y = gy; y < gy + 8; y++) mc.world.setBlock(px + dx, y, pz + dz, 0);
        }
        ground = gy;
        for (int y = gy; y < gy + raise; y++) mc.world.setBlock(px, y, pz, 1);
        boat = new net.minecraft.entity.vehicle.BoatEntity(mc.world, px + 0.5, gy + raise, pz + 0.5);
        mc.world.addEntity(boat);
        stand = null;
        jumps.clear();
        wasGround.clear();
        mc.player.sendChat("/gm s");
        mc.player.abilities.invulnerable = true;
        mc.player.setPosition(px + 0.5, gy + raise + 3, pz + 0.5);
    }

    private static void boatTick(Minecraft mc) {
        if (boat == null || boat.removed) return;
        if (stand == null && boat.onGround) {
            stand = new double[]{boat.x, boat.shape.maxY + mc.player.eyeHeight + 0.01, boat.z};
            String[] mobs = {"Zombie", "Spider", "Enderman", "Creeper"};
            for (int i = 0; i < mobs.length; i++) {
                double a = i * Math.PI / 2;
                PitSandbox.summon(mc, mobs[i], 1, boat.x + 7 * Math.cos(a), ground, boat.z + 7 * Math.sin(a));
            }
            System.out.println("[sandbox] BOAT box y " + String.format("%.2f..%.2f", boat.shape.minY, boat.shape.maxY) + " ground " + ground);
        }
        if (stand == null) return;
        mc.player.setPosition(stand[0], stand[1], stand[2]);
        mc.player.velocityX = mc.player.velocityZ = 0;
        boat.setPosition(stand[0], boat.y, stand[2]);
        boat.velocityX = boat.velocityZ = 0;
        for (Object o : mc.world.entities) {
            if (!(o instanceof net.minecraft.entity.mob.PathFinderMobEntity)) continue;
            net.minecraft.entity.mob.PathFinderMobEntity m = (net.minecraft.entity.mob.PathFinderMobEntity) o;
            String type = m.getClass().getSimpleName().replace("Entity", "");
            if (m instanceof net.minecraft.entity.mob.monster.CreeperEntity) {
                try { java.lang.reflect.Field fu = net.minecraft.entity.mob.monster.CreeperEntity.class.getDeclaredField("fuse"); fu.setAccessible(true); fu.setInt(m, 0); } catch (Throwable t) { }
            }
            try {
                java.lang.reflect.Field wt = net.minecraft.entity.mob.PathFinderMobEntity.class.getDeclaredField("walkTarget");
                wt.setAccessible(true);
                if (wt.get(m) == null) wt.set(m, mc.player);
            } catch (Throwable t) { }
            boolean was = wasGround.getOrDefault(m, true);
            wasGround.put(m, m.onGround);
            if (!was || m.onGround || m.velocityY <= 0.2) continue; // a jump just started
            net.minecraft.entity.ai.pathing.Path path = ((endpractice.mixin.PathMobAccessor) m).endpractice$getPath();
            int i3 = net.minecraft.util.math.MathHelper.floor(m.shape.minY + 0.5);
            String why;
            if (path != null) {
                net.minecraft.entity.ai.pathing.PathNode[] nodes = ((endpractice.mixin.PathAccessor) path).endpractice$getNodes();
                int idx = ((endpractice.mixin.PathAccessor) path).endpractice$getCurrentIndex();
                net.minecraft.entity.ai.pathing.PathNode next = nodes[Math.min(idx, nodes.length - 1)], end = nodes[nodes.length - 1];
                why = next.y > i3 ? "next path node is higher (node y " + next.y + ", mob y " + i3 + ", path ends at y " + end.y + ")" : "path, not higher (spider lunge?)";
            } else {
                why = m.collidingHorizontally ? "no path and bumping into something" : "no path";
            }
            String dist = String.format("%.1f", Math.sqrt((m.x - boat.x) * (m.x - boat.x) + (m.z - boat.z) * (m.z - boat.z)));
            jumps.merge(type + ": " + why, 1, Integer::sum);
            if (jumps.get(type + ": " + why) <= 2) System.out.println("[sandbox] BOAT jump " + type + " at " + dist + " from the boat: " + why + " bump " + m.collidingHorizontally);
        }
    }

    private static void boatCheck(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;
        if (sbTicks == 100) boatSetup(mc, 0);
        if (sbTicks > 100 && sbTicks < 700) boatTick(mc);
        if (sbTicks == 300) System.out.println("[sandbox] SHOT1");
        if (sbTicks == 700) {
            System.out.println("[sandbox] BOAT on the ground, player feet y " + String.format("%.2f", mc.player.shape.minY) + ": " + jumps);
            boatSetup(mc, 1);
        }
        if (sbTicks > 700 && sbTicks < 1300) boatTick(mc);
        if (sbTicks == 1000) System.out.println("[sandbox] SHOT2");
        if (sbTicks == 1300) {
            System.out.println("[sandbox] BOAT on a block, player feet y " + String.format("%.2f", mc.player.shape.minY) + ": " + jumps);
            System.out.println("[sandbox] DONE");
            Runtime.getRuntime().halt(0);
        }
    }

    // =5: ash's funnel designs. You sit in a boat over the pit; aggroed endermen come in waves of 4 from 16-40 blocks out,
    // the same spots for every design. Counts how many fall in, how fast, and their jumps.
    private static final String[] DESIGNS = {"ash_circle", "ash_cactus", "ash_plain"};
    private static final int WAVES = 12, PER_WAVE = 4, TIMEOUT = 900;
    private static int design = -1, wave, waveTick, fx, fy, fz;
    private static java.util.Random spots;
    private static final java.util.Map<net.minecraft.entity.Entity, int[]> track = new java.util.IdentityHashMap<>(); // start tick, jumps
    private static int fell, timedOut, jumpsTotal, jumpsOnFell;
    private static long fellTicks;
    private static final java.util.List<String> results = new java.util.ArrayList<>();

    private static void nextDesign(Minecraft mc) {
        design++;
        if (design >= DESIGNS.length) {
            for (String r : results) System.out.println("[sandbox] FUNNEL " + r);
            System.out.println("[sandbox] DONE");
            Runtime.getRuntime().halt(0);
        }
        for (Object o : new java.util.ArrayList<Object>(mc.world.entities)) if (o != mc.player && !(o instanceof net.minecraft.entity.vehicle.BoatEntity)) ((net.minecraft.entity.Entity) o).remove();
        if (mc.player.vehicle != null) mc.player.startRiding(null);
        for (Object o : new java.util.ArrayList<Object>(mc.world.entities)) if (o instanceof net.minecraft.entity.vehicle.BoatEntity) ((net.minecraft.entity.Entity) o).remove();
        // a fresh patch of desert for each design
        fx = design * 200 + 8; fz = 8;
        fy = PitSandbox.FLAT_TOP + 1;
        mc.player.setPosition(fx + 0.5, fy + mc.player.eyeHeight + 0.01, fz + 0.5);
        mc.player.yaw = 0;
        mc.player.sendChat("/pit load " + DESIGNS[design]);
        spots = new java.util.Random(1234);
        wave = 0;
        waveTick = -40;
        track.clear();
        fell = timedOut = jumpsTotal = jumpsOnFell = 0;
        fellTicks = 0;
        boat = null;
    }

    private static void funnelCheck(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;
        if (sbTicks < 60) return;
        if (sbTicks == 60) {
            mc.player.sendChat("/gm s");
            mc.player.abilities.invulnerable = true;
            try {
                java.lang.reflect.Field t = Minecraft.class.getDeclaredField("timer");
                t.setAccessible(true);
                Object timer = t.get(mc);
                timer.getClass().getField("tpsScale").setFloat(timer, 4.0F);
            } catch (Throwable e) {
                System.out.println("[sandbox] FUNNEL no speed-up: " + e);
            }
            nextDesign(mc);
            return;
        }
        waveTick++;
        // natural mobs out, only ours count
        for (Object o : new java.util.ArrayList<Object>(mc.world.entities)) {
            if (o instanceof net.minecraft.entity.mob.MobEntity && !track.containsKey(o)) ((net.minecraft.entity.Entity) o).remove();
        }
        if (waveTick == -30) {
            boat = new net.minecraft.entity.vehicle.BoatEntity(mc.world, fx + 0.5, fy + 4, fz + 0.5);
            mc.world.addEntity(boat);
        }
        if (waveTick == -10 && boat != null) {
            mc.player.startRiding(boat);
            System.out.println("[sandbox] FUNNEL " + DESIGNS[design] + ": boat y " + String.format("%.2f", boat.y) + " player feet " + String.format("%.2f", mc.player.shape.minY)
                    + " riding " + (mc.player.vehicle == boat) + " block under boat " + mc.world.getBlock(fx, fy + 3, fz) + " under centre " + mc.world.getBlock(fx, fy - 1, fz) + " " + mc.world.getBlock(fx, fy - 3, fz));
        }
        if (waveTick == 0 && design == 0) System.out.println("[sandbox] SHOT1");
        if (waveTick < 0) return;
        if (waveTick == 1) {
            for (int i = 0; i < PER_WAVE; i++) {
                double a = spots.nextDouble() * Math.PI * 2, r = 16 + spots.nextDouble() * 24;
                net.minecraft.entity.mob.monster.EndermanEntity e = new net.minecraft.entity.mob.monster.EndermanEntity(mc.world);
                e.setPositionAndAngles(fx + 0.5 + r * Math.cos(a), fy, fz + 0.5 + r * Math.sin(a), spots.nextFloat() * 360, 0);
                mc.world.addEntity(e);
                track.put(e, new int[]{sbTicks, 0, 1});
            }
        }
        boolean any = false;
        for (java.util.Map.Entry<net.minecraft.entity.Entity, int[]> en : track.entrySet()) {
            net.minecraft.entity.mob.monster.EndermanEntity e = (net.minecraft.entity.mob.monster.EndermanEntity) en.getKey();
            int[] st = en.getValue();
            if (st[2] == 0) continue;
            any = true;
            try {
                java.lang.reflect.Field wt = net.minecraft.entity.mob.PathFinderMobEntity.class.getDeclaredField("walkTarget");
                wt.setAccessible(true);
                if (wt.get(e) == null) wt.set(e, mc.player);
            } catch (Throwable t) { }
            boolean was = wasGround.getOrDefault(e, true);
            wasGround.put(e, e.onGround);
            if (was && !e.onGround && e.velocityY > 0.2) st[1]++;
            if (e.shape.minY < fy - 2.5) {
                fell++;
                fellTicks += sbTicks - st[0];
                jumpsOnFell += st[1];
                jumpsTotal += st[1];
                st[2] = 0;
                e.remove();
            } else if (e.removed || sbTicks - st[0] > TIMEOUT) {
                timedOut++;
                jumpsTotal += st[1];
                st[2] = 0;
                if (!e.removed) {
                    System.out.println("[sandbox] FUNNEL " + DESIGNS[design] + " stuck at " + String.format("%.1f %.1f %.1f", e.x - fx - 0.5, e.shape.minY - fy, e.z - fz - 0.5) + " jumps " + st[1]);
                    e.remove();
                }
            }
        }
        if (!any && waveTick > 1) {
            wave++;
            waveTick = 0;
            if (wave >= WAVES) {
                int n = WAVES * PER_WAVE;
                results.add(String.format("%s: %d/%d fell in, %.1f s average to fall in, %.1f jumps each (%.1f for the ones that fell in), %d stuck after %d s",
                        DESIGNS[design], fell, n, fell == 0 ? 0 : fellTicks / 20.0 / fell, (double) jumpsTotal / n, fell == 0 ? 0 : (double) jumpsOnFell / fell, timedOut, TIMEOUT / 20));
                System.out.println("[sandbox] FUNNEL " + results.get(results.size() - 1));
                nextDesign(mc);
            }
        }
    }

    // =6: screenshots of the boat spot in ash's slab circle, in daylight
    private static void funnelShots(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;
        if (sbTicks == 60) {
            overlays(false, false, false, false, false);
            EndPractice.cfg.pitPlanner = false;
            mc.player.sendChat("/time set 4000");
            mc.player.sendChat("/gm s");
            mc.player.abilities.invulnerable = true;
            fx = 8; fz = 8; fy = PitSandbox.FLAT_TOP + 1;
            mc.player.setPosition(fx + 0.5, fy + mc.player.eyeHeight + 0.01, fz + 0.5);
            mc.player.sendChat("/pit load ash_circle");
        }
        for (Object o : new java.util.ArrayList<Object>(mc.world.entities)) {
            if (o instanceof net.minecraft.entity.mob.MobEntity && !(o instanceof net.minecraft.entity.mob.monster.EndermanEntity)) ((net.minecraft.entity.Entity) o).remove();
        }
        if (sbTicks == 70) {
            boat = new net.minecraft.entity.vehicle.BoatEntity(mc.world, fx + 0.5, fy + 4, fz + 0.5);
            mc.world.addEntity(boat);
        }
        if (sbTicks == 90) {
            mc.player.startRiding(boat);
            for (int i = 0; i < 3; i++) {
                net.minecraft.entity.mob.monster.EndermanEntity e = new net.minecraft.entity.mob.monster.EndermanEntity(mc.world);
                e.setPositionAndAngles(fx + 0.5 + (i - 1) * 3, fy, fz + 0.5 + 9, 180, 0);
                mc.world.addEntity(e);
            }
        }
        if (sbTicks == 110) {
            mc.options.hideGui = true;
            mc.options.perspective = 2;
            mc.player.yaw = 180;
            mc.player.pitch = 12;
        }
        if (sbTicks > 110 && sbTicks < 140) {
            mc.player.yaw = 180;
            mc.player.pitch = 12;
        }
        if (sbTicks == 130) System.out.println("[sandbox] SHOT1");
        if (sbTicks == 140) {
            mc.player.startRiding(null);
            mc.options.perspective = 0;
            mc.player.abilities.flying = true;
        }
        if (sbTicks >= 140 && sbTicks < 170) {
            mc.player.setPosition(fx + 0.5, fy + 4 + mc.player.eyeHeight, fz + 0.5 - 7);
            mc.player.velocityX = mc.player.velocityY = mc.player.velocityZ = 0;
            mc.player.yaw = 0;
            mc.player.pitch = 32;
        }
        if (sbTicks == 165) System.out.println("[sandbox] SHOT2");
        if (sbTicks == 175) {
            System.out.println("[sandbox] DONE");
            Runtime.getRuntime().halt(0);
        }
    }

    // =7: the slab circle with you in the boat on the tower, creepers, spiders, zombies and endermen left to target you
    // themselves (endermen aggroed). Logs every jump and why.
    private static void mixedCheck(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;
        if (sbTicks == 60) {
            mc.player.sendChat("/gm s");
            mc.player.abilities.invulnerable = true;
            fx = 8; fz = 8; fy = PitSandbox.FLAT_TOP + 1;
            mc.player.setPosition(fx + 0.5, fy + mc.player.eyeHeight + 0.01, fz + 0.5);
            mc.player.sendChat("/pit load ash_circle");
            jumps.clear();
            wasGround.clear();
        }
        if (sbTicks == 70) {
            boat = new net.minecraft.entity.vehicle.BoatEntity(mc.world, fx + 0.5, fy + 4, fz + 0.5);
            mc.world.addEntity(boat);
        }
        if (sbTicks == 90) {
            mc.player.startRiding(boat);
            for (Object o : new java.util.ArrayList<Object>(mc.world.entities)) if (o instanceof net.minecraft.entity.mob.MobEntity) ((net.minecraft.entity.Entity) o).remove();
            java.util.Random r = new java.util.Random(99);
            String[] mobs = {"Creeper", "Spider", "Zombie", "Enderman"};
            int n = 0;
            for (String m : mobs) for (int i = 0; i < 6; i++) {
                double a = r.nextDouble() * Math.PI * 2, d = 8 + r.nextDouble() * 12;
                n += PitSandbox.summon(mc, m, 1, fx + 0.5 + d * Math.cos(a), fy, fz + 0.5 + d * Math.sin(a));
            }
            System.out.println("[sandbox] MIXED summoned " + n + ", time " + Math.floorMod(mc.world.getTime(), 24000L) + " held " + PitSandbox.lockedTime);
        }
        if (sbTicks > 90 && sbTicks < 90 + 2400) {
            for (Object o : new java.util.ArrayList<Object>(mc.world.entities)) {
                if (!(o instanceof net.minecraft.entity.mob.PathFinderMobEntity)) continue;
                net.minecraft.entity.mob.PathFinderMobEntity m = (net.minecraft.entity.mob.PathFinderMobEntity) o;
                String type = m.getClass().getSimpleName().replace("Entity", "");
                try {
                    if (m instanceof net.minecraft.entity.mob.monster.CreeperEntity) {
                        java.lang.reflect.Field fu = net.minecraft.entity.mob.monster.CreeperEntity.class.getDeclaredField("fuse"); fu.setAccessible(true); fu.setInt(m, 0);
                    }
                    if (m instanceof net.minecraft.entity.mob.monster.EndermanEntity) {
                        java.lang.reflect.Field wt = net.minecraft.entity.mob.PathFinderMobEntity.class.getDeclaredField("walkTarget");
                        wt.setAccessible(true);
                        if (wt.get(m) == null) wt.set(m, mc.player);
                    }
                } catch (Throwable t) { }
                if (m.shape.minY < fy - 2.5) {
                    jumps.merge(type + " fell in", 1, Integer::sum);
                    m.remove();
                    continue;
                }
                boolean was = wasGround.getOrDefault(m, true);
                wasGround.put(m, m.onGround);
                if (!was || m.onGround || m.velocityY <= 0.2) continue;
                net.minecraft.entity.ai.pathing.Path path = ((endpractice.mixin.PathMobAccessor) m).endpractice$getPath();
                int i3 = net.minecraft.util.math.MathHelper.floor(m.shape.minY + 0.5);
                String why;
                if (path != null) {
                    net.minecraft.entity.ai.pathing.PathNode[] nodes = ((endpractice.mixin.PathAccessor) path).endpractice$getNodes();
                    int idx = ((endpractice.mixin.PathAccessor) path).endpractice$getCurrentIndex();
                    net.minecraft.entity.ai.pathing.PathNode next = nodes[Math.min(idx, nodes.length - 1)];
                    boolean slab = mc.world.getBlock(next.x, next.y - 1, next.z) == 44;
                    why = next.y > i3 ? (slab ? "stepping onto a slab" : "next path point higher") : m instanceof net.minecraft.entity.mob.monster.SpiderEntity ? "spider lunge" : "has a path, not higher";
                } else {
                    why = m.collidingHorizontally ? "no path, bumping into something" : "no path";
                }
                if (m.isInWater()) why = "in water";
                String k = type + " jump: " + why;
                jumps.merge(k, 1, Integer::sum);
                if (jumps.get(k) <= 2) System.out.println("[sandbox] MIXED " + k + " at " + String.format("%.1f %.1f %.1f", m.x - fx - 0.5, m.shape.minY - fy, m.z - fz - 0.5));
            }
        }
        if (sbTicks == 90 + 2400) {
            System.out.println("[sandbox] MIXED 2 minutes: " + jumps);
            for (Object o : mc.world.entities) {
                if (!(o instanceof net.minecraft.entity.mob.PathFinderMobEntity)) continue;
                net.minecraft.entity.mob.PathFinderMobEntity m = (net.minecraft.entity.mob.PathFinderMobEntity) o;
                Object t = null;
                try { java.lang.reflect.Field wt = net.minecraft.entity.mob.PathFinderMobEntity.class.getDeclaredField("walkTarget"); wt.setAccessible(true); t = wt.get(m); } catch (Throwable e) { }
                System.out.println("[sandbox] MIXED left: " + m.getClass().getSimpleName() + String.format(" at %.1f %.1f %.1f", m.x - fx - 0.5, m.shape.minY - fy, m.z - fz - 0.5) + " target " + (t == null ? "none" : t.getClass().getSimpleName()) + " path " + (((endpractice.mixin.PathMobAccessor) m).endpractice$getPath() != null));
            }
            System.out.println("[sandbox] DONE");
            Runtime.getRuntime().halt(0);
        }
    }

    // =8: how far 4 sideways jumps take you in 1.0 (hold strafe and jump, stop on the 4th landing), for the Keima constant
    private static double sx0, sz0;
    private static int landings, trial;
    private static boolean airborne;
    private static net.minecraft.client.entity.mob.player.Input savedInput;
    private static final java.util.List<String> strafe = new java.util.ArrayList<>();

    private static void strafeCheck(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;
        if (sbTicks == 60) {
            mc.player.sendChat("/gm s");
            savedInput = mc.player.input;
        }
        int start = 80 + trial * 120;
        if (sbTicks == start) {
            mc.player.setPosition(0.5 + trial * 20, PitSandbox.FLAT_TOP + 1 + mc.player.eyeHeight + 0.01, 0.5);
            mc.player.velocityX = mc.player.velocityZ = 0;
            mc.player.yaw = trial * 37;
        }
        if (sbTicks == start + 20) {
            sx0 = mc.player.x; sz0 = mc.player.z;
            landings = 0; airborne = false;
            final boolean sneakNo = false;
            mc.player.input = new net.minecraft.client.entity.mob.player.Input() {
                public void tick(net.minecraft.entity.mob.player.PlayerEntity p) { this.movementSideways = 1.0F; this.jumping = true; this.sneaking = sneakNo; }
            };
        }
        if (sbTicks > start + 20 && mc.player.input != savedInput) {
            if (!mc.player.onGround) airborne = true;
            else if (airborne) {
                airborne = false;
                landings++;
                if (landings == 4) {
                    double d = Math.sqrt((mc.player.x - sx0) * (mc.player.x - sx0) + (mc.player.z - sz0) * (mc.player.z - sz0));
                    strafe.add(String.format("%.3f in %d ticks", d, sbTicks - start - 20));
                    mc.player.input = savedInput;
                    trial++;
                }
            }
        }
        if (trial == 3) {
            System.out.println("[sandbox] STRAFE 4 sideways jumps: " + strafe);
            System.out.println("[sandbox] DONE");
            Runtime.getRuntime().halt(0);
        }
    }

    // =9: blocks per sprint jump in 1.0, steady state on flat ground (holding forward, sprint and jump)
    private static int sjLand;
    private static double sjX, sjZ;
    private static long sjTick;
    private static final java.util.List<String> sj = new java.util.ArrayList<>();

    private static void sprintCheck(Minecraft mc) {
        if (mc.player == null || mc.world == null) return;
        if (sbTicks == 60) {
            mc.player.sendChat("/gm s");
            savedInput = mc.player.input;
            mc.player.setPosition(0.5, PitSandbox.FLAT_TOP + 1 + mc.player.eyeHeight + 0.01, 0.5);
            mc.player.yaw = -90; // +x
        }
        if (sbTicks == 80) {
            mc.player.input = new net.minecraft.client.entity.mob.player.Input() {
                public void tick(net.minecraft.entity.mob.player.PlayerEntity p) { this.movementForward = 1.0F; this.jumping = true; }
            };
            airborne = false;
            sjLand = 0;
        }
        if (sbTicks > 80) {
            mc.player.setSprinting(true);
            mc.player.yaw = -90;
            mc.player.getHungerManager().add(20, 20.0F);
            if (!mc.player.onGround) airborne = true;
            else if (airborne) {
                airborne = false;
                sjLand++;
                if (sjLand == 10) { sjX = mc.player.x; sjZ = mc.player.z; sjTick = sbTicks; }
                if (sjLand == 40) {
                    double d = Math.hypot(mc.player.x - sjX, mc.player.z - sjZ);
                    System.out.println(String.format("[sandbox] SPRINT 30 steady jumps: %.2f blocks, %.4f per jump, %d ticks (%.2f ticks per jump), %.3f blocks/s, sprinting %s",
                            d, d / 30, sbTicks - sjTick, (sbTicks - sjTick) / 30.0, d / ((sbTicks - sjTick) / 20.0), mc.player.isSprinting()));
                    System.out.println("[sandbox] DONE");
                    Runtime.getRuntime().halt(0);
                }
            }
        }
    }
}
