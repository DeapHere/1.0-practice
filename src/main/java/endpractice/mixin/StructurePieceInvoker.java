package endpractice.mixin;

import net.minecraft.world.gen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A piece's local coordinates to world ones, the way the piece itself places its blocks. */
@Mixin(StructurePiece.class)
public interface StructurePieceInvoker {
    @Invoker("transformX")
    int endpractice$transformX(int x, int z);

    @Invoker("transformY")
    int endpractice$transformY(int y);

    @Invoker("transformZ")
    int endpractice$transformZ(int x, int z);
}
