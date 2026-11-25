package endpractice.mixin;

import endpractice.EyeTrainer;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** In practice an eye flies to the closest portal room (no eye bug), unless Vanilla eye targeting is on. */
@Mixin(EnderEyeItem.class)
public abstract class EyeTargetMixin {
    @Redirect(method = "startUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;findNearestStructure(Ljava/lang/String;III)Lnet/minecraft/util/math/BlockPos;"))
    private BlockPos endpractice$closestRoom(World world, String type, int x, int y, int z) {
        BlockPos room = EyeTrainer.eyeGoesTo(world, x, z);
        return room != null ? room : world.findNearestStructure(type, x, y, z);
    }
}
