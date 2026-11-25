package endpractice.mixin;

import net.minecraft.client.CreativeInteractionManager;
import net.minecraft.entity.mob.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 1.0's creative fills an empty hotbar with 9 random blocks whenever you spawn in it (like after dying). Just creative, no blocks. */
@Mixin(CreativeInteractionManager.class)
public abstract class CreativeHotbarMixin {
    @Inject(method = "adjustPlayer", at = @At("HEAD"), cancellable = true)
    private void endpractice$noRandomBlocks(PlayerEntity player, CallbackInfo ci) {
        if (endpractice.compat.RankedCompat.busy()) return;
        CreativeInteractionManager.setCreativeMode(player);
        ci.cancel();
    }
}
