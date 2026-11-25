package endpractice.mixin;

import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * /time lock: the sky, light and mob spawning see the held time of day, while the world clock keeps running. Holding
 * the clock itself stopped scheduled block ticks, so water didn't flow and sand didn't fall.
 */
@Mixin(Dimension.class)
public abstract class DimensionTimeMixin {
    @ModifyVariable(method = "getTimeOfDay", at = @At("HEAD"), argsOnly = true)
    private long endpractice$heldTime(long time) {
        long held = endpractice.PitSandbox.lockedTime;
        return held >= 0 ? held : time;
    }
}
