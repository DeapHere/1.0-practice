package endpractice.mixin;

import endpractice.BedReport;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionMixin {
    @Shadow private World world;
    @Shadow public double x;
    @Shadow public double y;
    @Shadow public double z;
    @Shadow public float power;

    // only reads the dragon before and after an explosion damages it. Changes nothing.
    @Inject(method = "damageEntities", at = @At("HEAD"))
    private void endpractice$before(CallbackInfo ci) {
        BedReport.beforeExplosion(this.world, this.x, this.y, this.z, this.power);
    }

    @Inject(method = "damageEntities", at = @At("TAIL"))
    private void endpractice$after(CallbackInfo ci) {
        BedReport.afterExplosion(this.world, this.power);
    }
}
