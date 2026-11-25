package endpractice.mixin;

import endpractice.PitSandbox;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.dimension.Dimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pit sandbox, flat desert: it's all desert biome (mob spawning and the sky go by biome). */
@Mixin(BiomeSource.class)
public abstract class FlatBiomeMixin {
    @Inject(method = "getBiome(II)Lnet/minecraft/world/biome/Biome;", at = @At("HEAD"), cancellable = true)
    private void endpractice$desert(int x, int z, CallbackInfoReturnable<Biome> cir) {
        if (PitSandbox.flatGen) cir.setReturnValue(Biome.DESERT);
    }

    @Inject(method = "getBiomes([Lnet/minecraft/world/biome/Biome;IIIIZ)[Lnet/minecraft/world/biome/Biome;", at = @At("HEAD"), cancellable = true)
    private void endpractice$desertArea(Biome[] biomes, int x, int z, int sizeX, int sizeZ, boolean useCache, CallbackInfoReturnable<Biome[]> cir) {
        if (!PitSandbox.flatGen) return;
        if (biomes == null || biomes.length < sizeX * sizeZ) biomes = new Biome[sizeX * sizeZ];
        java.util.Arrays.fill(biomes, 0, sizeX * sizeZ, Biome.DESERT);
        cir.setReturnValue(biomes);
    }
}
