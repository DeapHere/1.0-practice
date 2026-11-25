package endpractice.mixin;

import endpractice.PitSandbox;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSource;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.OverworldChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pit sandbox, flat desert: every chunk is flat sand, with nothing placed on it (no caves, lakes, cacti or structures). */
@Mixin(OverworldChunkGenerator.class)
public abstract class FlatDesertMixin {
    @Shadow private World world;

    @Inject(method = "getChunk", at = @At("HEAD"), cancellable = true)
    private void endpractice$flat(int chunkX, int chunkZ, CallbackInfoReturnable<WorldChunk> cir) {
        if (!PitSandbox.flatGen) return;
        byte[] blocks = new byte[16 * this.world.height * 16];
        PitSandbox.flatChunk(blocks, this.world.height);
        WorldChunk chunk = new WorldChunk(this.world, blocks, chunkX, chunkZ);
        chunk.populateHeightMap();
        cir.setReturnValue(chunk);
    }

    @Inject(method = "populateChunk", at = @At("HEAD"), cancellable = true)
    private void endpractice$nothingOnTop(ChunkSource source, int chunkX, int chunkZ, CallbackInfo ci) {
        if (PitSandbox.flatGen) ci.cancel();
    }
}
