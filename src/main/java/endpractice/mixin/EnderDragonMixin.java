package endpractice.mixin;

import endpractice.BedReport;
import net.minecraft.entity.mob.monster.boss.EnderDragonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonMixin {
    // records where the head part is after each tick, for the bed report. Changes nothing.
    @Inject(method = "mobTick", at = @At("TAIL"))
    private void endpractice$recordHead(CallbackInfo ci) {
        BedReport.recordDragon((EnderDragonEntity) (Object) this);
    }
}
