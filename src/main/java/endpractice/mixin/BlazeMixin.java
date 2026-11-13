package endpractice.mixin;

import endpractice.BlazePractice;
import java.util.Random;
import net.minecraft.entity.mob.monster.BlazeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlazeEntity.class)
public abstract class BlazeMixin {
    /** Vanilla's rods (random 0 or 1 without looting), counted for blaze practice. */
    @Redirect(method = "dropItems", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private int endpractice$rods(Random random, int bound) {
        int n = random.nextInt(bound);
        BlazePractice.onBlazeKilled(n);
        return n;
    }
}
