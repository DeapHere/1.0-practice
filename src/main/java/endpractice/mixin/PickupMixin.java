package endpractice.mixin;

import endpractice.Commands;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spectators don't pick up items, xp or arrows. */
@Mixin({ItemEntity.class, ExperienceOrbEntity.class, ArrowEntity.class})
public abstract class PickupMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void endpractice$noPickup(PlayerEntity player, CallbackInfo ci) {
        if (Commands.spectating()) ci.cancel();
        // double bed practice: mined end stone and anything else that isn't in the kit stays out of the inventory
        if ((Object) this instanceof ItemEntity && endpractice.DoubleBed.keepOut(((ItemEntity) (Object) this).item)) ci.cancel();
    }
}
