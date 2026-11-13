package endpractice.mixin;

import endpractice.Endgame;
import java.util.Random;
import net.minecraft.entity.mob.monster.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndermanEntity.class)
public abstract class EndermanMixin {
    /** The number of pearls: random 0 or 1 in vanilla, or exactly 1 every 2 endermen with average pearl luck. */
    @Redirect(method = "dropItems", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"))
    private int endpractice$pearls(Random random, int bound) {
        int n = Endgame.averagePearls(bound);
        if (n < 0) n = random.nextInt(bound);
        Endgame.onEndermanKilled(n);
        return n;
    }
}
