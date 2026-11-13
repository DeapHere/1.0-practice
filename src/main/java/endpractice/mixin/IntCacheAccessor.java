package endpractice.mixin;

import java.util.List;
import net.minecraft.world.biome.IntCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** IntCache's static array pool, so a new practice world can start with an empty pool like the first world after launch. */
@Mixin(IntCache.class)
public interface IntCacheAccessor {
    @Accessor("maxSize")
    static void endpractice$setMaxSize(int size) {
        throw new AssertionError();
    }

    @Accessor("tcache")
    static void endpractice$setTcache(List<int[]> list) {
        throw new AssertionError();
    }

    @Accessor("tallocated")
    static void endpractice$setTallocated(List<int[]> list) {
        throw new AssertionError();
    }

    @Accessor("cache")
    static void endpractice$setCache(List<int[]> list) {
        throw new AssertionError();
    }

    @Accessor("allocated")
    static void endpractice$setAllocated(List<int[]> list) {
        throw new AssertionError();
    }
}
