package endpractice.mixin;

import net.minecraft.world.chunk.ChunkCache;
import net.minecraft.world.chunk.ChunkSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The chunk generator behind a world's chunk cache (the nether's has the fortresses). */
@Mixin(ChunkCache.class)
public interface ChunkCacheAccessor {
    @Accessor("generator")
    ChunkSource endpractice$getGenerator();
}
