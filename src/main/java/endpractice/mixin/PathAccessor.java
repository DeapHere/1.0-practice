package endpractice.mixin;

import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Pit sandbox: a path's nodes and how far along it the mob is. */
@Mixin(Path.class)
public interface PathAccessor {
    @Accessor("nodes")
    PathNode[] endpractice$getNodes();

    @Accessor("currentIndex")
    int endpractice$getCurrentIndex();
}
