package endpractice.mixin;

import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ArrowEntity.class)
public interface ArrowEntityAccessor {
    /** The arrow has stuck in a block: it can't hit anything any more. */
    @Accessor("inGround")
    boolean endpractice$isInGround();
}
