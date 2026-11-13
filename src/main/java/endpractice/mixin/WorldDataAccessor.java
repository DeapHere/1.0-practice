package endpractice.mixin;

import net.minecraft.world.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldData.class)
public interface WorldDataAccessor {
    @Accessor("defaultGameMode")
    void endpractice$setGameMode(int mode);
}
