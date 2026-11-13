package endpractice;

import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Spectator mode, which 1.0 doesn't have: fly through blocks, mobs ignore you (they never target an invulnerable
 * player), and nothing you click does anything. The rest is done every tick in {@link Commands#tick}.
 */
public final class SpectatorInteractionManager extends ClientPlayerInteractionManager {
    public SpectatorInteractionManager(Minecraft minecraft) {
        super(minecraft);
    }

    @Override public void startMiningBlock(int x, int y, int z, int face) {}
    @Override public void tickBlockMining(int x, int y, int z, int face) {}
    @Override public void stopMiningBlock() {}
    @Override public float getReach() { return 0.0F; }
    @Override public boolean hasStatusBars() { return false; }
    @Override public boolean useBlock(PlayerEntity player, World world, ItemStack itemInHand, int x, int y, int z, int face) { return false; }
    @Override public boolean useItem(PlayerEntity player, World world, ItemStack itemInHand) { return false; }
    @Override public void interactEntity(PlayerEntity player, Entity target) {}
    @Override public void attackEntity(PlayerEntity player, Entity target) {}
    @Override public boolean hasAttackCooldown() { return false; }

    @Override
    public void adjustPlayer(PlayerEntity player) {
        Commands.spectatorAbilities(player);
    }
}
