package endpractice.mixin;

import endpractice.EndPractice;
import java.util.Arrays;
import net.minecraft.world.biome.layer.Layer;
import net.minecraft.world.biome.source.BiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BiomeSource.class)
public abstract class BiomeSourceMixin {
    /**
     * Vanilla 1.0 bug: findBiome (world spawn and stronghold placement) loops over the whole array the biome layers
     * hand back, which is a pooled array usually longer than the area asked for. The rest still holds old layer data,
     * and a -1 there crashes the game. In practice worlds only, the loop sees just the area it asked for.
     */
    @Redirect(method = "findBiome", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/layer/Layer;getArea(IIII)[I"))
    private int[] endpractice$onlyTheArea(Layer layer, int x, int z, int width, int height) {
        int[] area = layer.getArea(x, z, width, height);
        if (!EndPractice.inPracticeWorld() || area.length <= width * height) return area;
        return Arrays.copyOf(area, width * height);
    }
}
