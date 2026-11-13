package endpractice.mixin;

import endpractice.Commands;
import endpractice.EndPractice;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A spectator is never "in a wall": no block texture over the screen, so you see through the terrain into the caves
 * around you (the block you're in isn't drawn from the inside), and no suffocation.
 */
@Mixin(Entity.class)
public abstract class EntityInWallMixin {
    @Inject(method = "isInWall", at = @At("HEAD"), cancellable = true)
    private void endpractice$spectatorSeesThrough(CallbackInfoReturnable<Boolean> cir) {
        if (EndPractice.mc != null && (Object) this == EndPractice.mc.player && Commands.spectating()) cir.setReturnValue(false);
    }
}
