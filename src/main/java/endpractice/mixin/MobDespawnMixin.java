package endpractice.mixin;

import endpractice.SpawnOverlay;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Spawn overlay: counts the mobs vanilla's despawn check removes (it changes nothing). */
@Mixin(MobEntity.class)
public abstract class MobDespawnMixin {
    @Redirect(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/MobEntity;remove()V"), require = 0)
    private void endpractice$despawn(MobEntity mob) {
        boolean was = mob.removed;
        mob.remove();
        if (!was) SpawnOverlay.despawned(mob);
    }
}
