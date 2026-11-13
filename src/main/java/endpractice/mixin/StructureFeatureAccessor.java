package endpractice.mixin;

import java.util.HashMap;
import net.minecraft.world.gen.structure.StructureFeature;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The structures a feature has laid out so far (start chunk -> pieces). */
@Mixin(StructureFeature.class)
public interface StructureFeatureAccessor {
    @Accessor("structures")
    HashMap<Long, StructureStart> endpractice$getStructures();

    /** Strongholds: the three spots (chunk centres at y 64) an eye uses before any stronghold has generated near you. */
    @Invoker("getPotentialPositions")
    java.util.List endpractice$potentialPositions();
}
