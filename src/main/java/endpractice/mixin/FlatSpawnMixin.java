package endpractice.mixin;

import endpractice.PitSandbox;
import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pit sandbox, flat desert: there's no grass, so the world's first spawn search takes anywhere (spawn is then set to 0 0). */
@Mixin(Dimension.class)
public abstract class FlatSpawnMixin {
    @Inject(method = "isValidSpawnPoint", at = @At("HEAD"), cancellable = true)
    private void endpractice$anywhere(int x, int z, CallbackInfoReturnable<Boolean> cir) {
        if (PitSandbox.flatGen) cir.setReturnValue(true);
    }
}
