package endpractice.selftest;

import endpractice.EndPractice;
import endpractice.Endgame;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * -Dendpractice.endgame=N: runs N endgame attempts, cycling full / tower only / stronghold: dies for the tower split,
 * respawns, goes to the stronghold's portal room, into the End, and kills the dragon.
 */
@Mixin(Minecraft.class)
public abstract class EndgameTestMixin {
    private static int egleft = Integer.getInteger("endpractice.endgame", 0);
    private static int egn;
    private static int egstep;
    private static int egwait;
    private static int egsearch;
    private static boolean egSecondDeath;

    @Inject(method = "tick", at = @At("TAIL"))
    private void endgameTest(CallbackInfo ci) {
        if (egleft <= 0 && egstep == 0) return;
        Minecraft mc = (Minecraft) (Object) this;
        if (++egwait < 20) return;
        int[] keys = {Keyboard.KEY_O, Keyboard.KEY_I, Keyboard.KEY_L};
        switch (egstep) {
            case 0:
                if (!(mc.screen instanceof TitleScreen) && !Endgame.isActive()) return;
                Endgame.Mode m = Endgame.Mode.values()[egn % 3];
                System.out.println("[egtest] attempt " + (egn + 1) + " mode " + m);
                EndPractice.queueEndgameKey(keys[egn % 3]);
                egn++;
                egleft--;
                egstep = 1;
                egwait = 0;
                return;
            case 1: // in the new world
                if (!Endgame.isActive() || mc.player == null) return;
                System.out.println("[egtest] time=" + (mc.world.getTime() % 24000) + " spawn biome=" + mc.world.getBiomeSource().getBiome((int) mc.player.x, (int) mc.player.z).name + " " + (int) mc.player.x + " " + (int) mc.player.y + " " + (int) mc.player.z
                        + " items slot0=" + (mc.player.inventory.items[0] == null ? "-" : mc.player.inventory.items[0].id)
                        + " slot8=" + (mc.player.inventory.items[8] == null ? "-" : mc.player.inventory.items[8].id + "x" + mc.player.inventory.items[8].size));
                int water = 0, cols = 0;
                for (int x = (int) mc.player.x - 64; x <= (int) mc.player.x + 64; x += 2)
                    for (int z = (int) mc.player.z - 64; z <= (int) mc.player.z + 64; z += 2) {
                        cols++;
                        if (mc.world.getMaterial(x, mc.world.getHeight(x, z) - 1, z).isLiquid()) water++;
                    }
                System.out.println("[egtest] water columns within 64: " + water + "/" + cols);
                hud(mc);
                if (Endgame.Mode.values()[(egn - 1) % 3] == Endgame.Mode.STRONGHOLD) {
                    egstep = 3;
                } else if (Endgame.Mode.values()[(egn - 1) % 3] == Endgame.Mode.FULL) {
                    // up the tower first, then the death reset
                    mc.player.setPosition(mc.player.x, mc.player.y + 40, mc.player.z);
                    egstep = 9;
                } else if (egn == 2) {
                    // average pearl luck: kill 6 endermen
                    for (int i = 0; i < 6; i++) {
                        net.minecraft.entity.mob.monster.EndermanEntity e = new net.minecraft.entity.mob.monster.EndermanEntity(mc.world);
                        e.setPosition(mc.player.x + 2, mc.player.y, mc.player.z);
                        mc.world.addEntity(e);
                        e.takeDamage(DamageSource.player(mc.player), 1000);
                    }
                    egstep = 8;
                } else {
                    // tower only: an accidental death on the ground, which must not count
                    mc.player.takeDamage(DamageSource.OUT_OF_WORLD, 1000);
                    egstep = 2;
                }
                egwait = 0;
                return;
            case 8: {
                int n = 0;
                for (Object o : mc.world.entities)
                    if (o instanceof net.minecraft.entity.ItemEntity && ((net.minecraft.entity.ItemEntity) o).item.id == 368)
                        n += ((net.minecraft.entity.ItemEntity) o).item.size;
                System.out.println("[egtest] pearls on the ground after 6 endermen: " + n);
                mc.player.takeDamage(DamageSource.OUT_OF_WORLD, 1000);
                egstep = 2;
                egwait = 0;
                return;
            }
            case 9:
                mc.player.takeDamage(DamageSource.OUT_OF_WORLD, 1000);
                egstep = 2;
                egwait = 0;
                return;
            case 2: // dead: respawn
                if (mc.player.getHealth() > 0) return;
                hud(mc);
                if (Endgame.Mode.values()[(egn - 1) % 3] == Endgame.Mode.TOWER) {
                    done(mc);
                    return;
                }
                mc.respawnPlayer(false, 0, false);
                mc.openScreen(null);
                System.out.println("[egtest] respawned at " + (int) mc.player.x + " " + (int) mc.player.y + " " + (int) mc.player.z);
                if (!egSecondDeath && Endgame.Mode.values()[(egn - 1) % 3] == Endgame.Mode.FULL) {
                    // a second death reset in the pit: the run must go on
                    egSecondDeath = true;
                    mc.player.takeDamage(DamageSource.OUT_OF_WORLD, 1000);
                    egstep = 2;
                    egwait = 0;
                    return;
                }
                egSecondDeath = false;
                egstep = 3;
                egwait = 0;
                return;
            case 3: { // go to the stronghold
                BlockPos s = mc.world.findNearestStructure("Stronghold", (int) mc.player.x, (int) mc.player.y, (int) mc.player.z);
                System.out.println("[egtest] stronghold " + (s == null ? "none" : s.x + " " + s.y + " " + s.z + " dist " + (int) Math.hypot(s.x - mc.player.x, s.z - mc.player.z)));
                if (s == null) {
                    egstep = 5;
                    return;
                }
                mc.player.setPosition(s.x + 0.5, 120, s.z + 0.5);
                egsearch = 0;
                egstep = 4;
                egwait = 0;
                return;
            }
            case 4: { // find a portal frame in the loaded chunks around and stand on it
                int px = (int) mc.player.x, pz = (int) mc.player.z;
                int id = Block.END_PORTAL_FRAME.id;
                for (int x = px - 80; x <= px + 80; x++)
                    for (int z = pz - 80; z <= pz + 80; z++)
                        for (int y = 5; y < 60; y++)
                            if (mc.world.getBlock(x, y, z) == id) {
                                System.out.println("[egtest] portal frame at " + x + " " + y + " " + z);
                                mc.player.setPosition(x + 0.5, y + 1.0 + mc.player.eyeHeight, z + 0.5);
                                egstep = 5;
                                egwait = 0;
                                return;
                            }
                if (++egsearch > 5) {
                    System.out.println("[egtest] no portal frame found");
                    egstep = 5;
                }
                return;
            }
            case 5:
                hud(mc);
                mc.changeDimension(1);
                egstep = 6;
                egwait = 0;
                return;
            case 6:
                if (egwait < 100) return;
                hud(mc);
                if (EndPractice.dragon(mc) != null) EndPractice.dragon(mc).setHealth(0);
                egstep = 7;
                egwait = 0;
                return;
            case 7:
                done(mc);
                return;
        }
    }

    private static void done(Minecraft mc) {
        hud(mc);
        egstep = 0;
        egwait = 0;
        if (egleft <= 0) System.out.println("[egtest] ENDGAME TEST DONE");
    }

    private static void hud(Minecraft mc) {
        System.out.println("[egtest] hud: " + String.join(" | ", EndPractice.hud(mc)).replaceAll("§.", ""));
    }
}
