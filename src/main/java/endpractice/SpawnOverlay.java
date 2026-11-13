package endpractice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobCategory;
import net.minecraft.entity.mob.monster.EndermanEntity;
import net.minecraft.entity.mob.monster.Monster;
import net.minecraft.world.World;

/**
 * Spawn overlay setting: 1.0's monster spawning as it happens, read from the real spawner and despawn check.
 * <p>
 * Every tick that the loaded monsters are at or under the cap (70 x 289 chunks / 256 = 79), NaturalSpawner tries
 * every spawning chunk around you at once, so one open tick can spawn several monsters and go over the cap. Then
 * nothing spawns until enough monsters despawn (over 128 blocks away at once, over 32 blocks now and then) or die to
 * get back to the cap, and the next open tick spawns another burst. So monsters come in bursts that follow the
 * despawns.
 */
public final class SpawnOverlay {
    private static final int SHOWN = 6;
    /** Spawn ticks this close together are one burst. */
    private static final int MERGE_TICKS = 3;

    private static final class Burst {
        long start, end;
        int monsters, endermen;
        /** Monsters despawned since the burst before. */
        int despawned;
    }

    private static final Deque<Burst> bursts = new ArrayDeque<>();
    private static World lastWorld;
    private static int tickMonsters, tickEndermen;
    private static int despawned, endermenDespawned;

    /** The last respawn: monsters loaded while you were dead, and how many despawned in the FLUSH_TICKS after. */
    private static final int FLUSH_TICKS = 40;
    private static boolean wasDead;
    private static int deadCount, deadEndermen;
    private static long flushStart = -1;
    private static int earlyFlushed, earlyEndermen;
    private static Minecraft client;
    private static int flushBefore, flushed, flushedEndermen, flushEndermenBefore;
    /**
     * The monsters loaded on the last tick you were dead. Only these count as flushed, so monsters that spawn or load in
     * after the respawn (and then despawn) can't take it over 100%.
     */
    private static java.util.Set<Object> deadSet = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    private SpawnOverlay() {
    }

    private static boolean on() {
        return EndPractice.cfg != null && EndPractice.cfg.spawnOverlay && EndPractice.inPracticeWorld();
    }

    public static int cap() {
        return MobCategory.MONSTER.getCap() * 289 / 256;
    }

    /** NaturalSpawner.tick starting. */
    public static void spawnerStart(World world) {
        if (world != lastWorld) {
            bursts.clear();
            despawned = endermenDespawned = 0;
            lastWorld = world;
        }
        tickMonsters = tickEndermen = 0;
    }

    /** Every mob NaturalSpawner.tick adds. */
    public static void spawned(Entity e) {
        if (e instanceof Monster) tickMonsters++;
        if (e instanceof EndermanEntity) tickEndermen++;
    }

    /** NaturalSpawner.tick done: monsters spawned this tick start or extend a burst. */
    public static void spawnerEnd(World world) {
        if (tickMonsters == 0 || !on()) return;
        long now = world.getTime();
        Burst b = bursts.peekFirst();
        if (b == null || now - b.end > MERGE_TICKS) {
            b = new Burst();
            b.start = now;
            b.despawned = despawned;
            despawned = endermenDespawned = 0;
            bursts.addFirst(b);
            while (bursts.size() > SHOWN) bursts.removeLast();
        }
        b.end = now;
        b.monsters += tickMonsters;
        b.endermen += tickEndermen;
        EndPractice.log("spawned " + tickMonsters + " monsters" + (tickEndermen > 0 ? " (" + tickEndermen + " endermen)" : "")
                + " at " + now + ", " + world.getEntityCount(Monster.class) + "/" + cap() + " now");
    }

    private static boolean flushOn() {
        return EndPractice.cfg != null && EndPractice.cfg.flushOverlay && EndPractice.inPracticeWorld();
    }

    /** Every tick of an endgame run: notices the respawn after a death. */
    static void tick(Minecraft mc) {
        if (mc.world != lastWorld && mc.world != null) {
            lastWorld = mc.world;
            bursts.clear();
            despawned = endermenDespawned = 0;
            flushStart = -1;
        }
        if (mc.player == null || mc.world.dimension.id != 0) return;
        boolean dead = mc.player.getHealth() <= 0;
        if (dead) {
            deadSet.clear();
            deadEndermen = 0;
            for (Object o : mc.world.entities) {
                if (o instanceof Monster && !((Entity) o).removed) {
                    deadSet.add(o);
                    if (o instanceof EndermanEntity) deadEndermen++;
                }
            }
            deadCount = deadSet.size();
        } else if (wasDead) {
            // respawned this tick; the world may already have despawned some for it (counted in despawned)
            flushStart = mc.world.getTime();
            flushBefore = deadCount;
            flushEndermenBefore = deadEndermen;
            flushed = earlyFlushed;
            flushedEndermen = earlyEndermen;
        }
        earlyFlushed = earlyEndermen = 0;
        if (!dead && !flushing(mc.world)) deadSet.clear();
        wasDead = dead;
        client = mc;
    }

    private static boolean flushing(World world) {
        return flushStart >= 0 && world != null && world.getTime() - flushStart <= FLUSH_TICKS;
    }

    /** A mob vanilla's despawn check removed: too far from you. */
    public static void despawned(Entity e) {
        if (!(e instanceof Monster)) return;
        if (!deadSet.remove(e)) {
            // not one of the monsters loaded while you were dead
        } else if (wasDead && client != null && client.player != null && client.player.getHealth() > 0) {
            // the respawn happened earlier in this tick, before tick() saw it
            earlyFlushed++;
            if (e instanceof EndermanEntity) earlyEndermen++;
        } else if (flushing(e.world)) {
            flushed++;
            if (e instanceof EndermanEntity) flushedEndermen++;
        }
        if (!on()) return;
        despawned++;
        if (e instanceof EndermanEntity) endermenDespawned++;
    }

    static List<String> hud(Minecraft mc) {
        List<String> lines = new ArrayList<>();
        if (mc.world == null || mc.world.dimension.id != 0) return lines;
        if (flushOn() && flushStart >= 0) {
            // flushed at the last respawn on the tower, out of the monsters loaded while you were dead
            int pct = flushBefore == 0 ? 0 : Math.round(100.0F * flushed / flushBefore);
            lines.add("Flushed " + flushed + "/" + flushBefore + " " + pct + "%"
                    + (flushEndermenBefore > 0 ? " §d" + flushedEndermen + "/" + flushEndermenBefore + " E§r" : ""));
        }
        if (!on()) return lines;
        int count = mc.world.getEntityCount(Monster.class);
        int cap = cap();
        long now = mc.world.getTime();
        lines.add("Cap " + (count > cap ? "§c" : "§a") + count + "/" + cap + "§r");
        lines.add("Despawned " + despawned);
        Burst b = bursts.peekFirst();
        if (b != null) lines.add("Spawned " + b.monsters + (b.endermen > 0 ? "§d+E§r" : "") + ", " + (now - b.start) / 20 + "s ago");
        return lines;
    }
}
