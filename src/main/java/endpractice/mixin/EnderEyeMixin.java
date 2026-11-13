package endpractice.mixin;

import endpractice.EyeTrainer;
import net.minecraft.entity.EnderEyeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every eye of ender thrown, with the stronghold it's flying to. */
@Mixin(EnderEyeEntity.class)
public abstract class EnderEyeMixin {
    @Inject(method = "setTarget", at = @At("HEAD"))
    private void endpractice$thrown(double x, int y, double z, CallbackInfo ci) {
        EyeTrainer.onEyeThrown((EnderEyeEntity) (Object) this, x, z);
    }
}
