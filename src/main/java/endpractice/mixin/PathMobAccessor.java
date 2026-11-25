package endpractice.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.PathFinderMobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Pit sandbox: the path a mob is walking and what it's walking to. */
@Mixin(PathFinderMobEntity.class)
public interface PathMobAccessor {
    @Accessor("path")
    Path endpractice$getPath();

    @Accessor("walkTarget")
    Entity endpractice$getWalkTarget();
}
