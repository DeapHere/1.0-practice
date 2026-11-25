package endpractice.mixin;

import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MobEntity.class)
public interface MobEntityAccessor {
    /** The damage of the hit that started the current invulnerable time. A new hit only counts above it. */
    @Accessor("prevDamageTaken")
    int endpractice$getPrevDamageTaken();

    /** Set by a killing hit. Double bed practice clears it to bring the dragon back. */
    @Accessor("dead")
    void endpractice$setDead(boolean dead);
}
