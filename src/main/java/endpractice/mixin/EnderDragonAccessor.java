package endpractice.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.monster.boss.EnderDragonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnderDragonEntity.class)
public interface EnderDragonAccessor {
    /** The player the dragon is flying at, or null when it flies to a point. */
    @Accessor("target")
    Entity endpractice$getTarget();

    @Accessor("target")
    void endpractice$setTarget(Entity target);
}
