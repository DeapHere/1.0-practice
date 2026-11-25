package endpractice.mixin;

import endpractice.PitSandbox;
import net.minecraft.entity.mob.monster.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pit sandbox: where endermen teleport from and to. */
@Mixin(EndermanEntity.class)
public abstract class EndermanTeleportMixin {
    @Unique private double endpractice$fromX, endpractice$fromY, endpractice$fromZ;

    @Inject(method = "teleportTo(DDD)Z", at = @At("HEAD"))
    private void endpractice$before(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        EndermanEntity e = (EndermanEntity) (Object) this;
        endpractice$fromX = e.x;
        endpractice$fromY = e.y;
        endpractice$fromZ = e.z;
    }

    @Inject(method = "teleportTo(DDD)Z", at = @At("RETURN"))
    private void endpractice$after(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            EndermanEntity e = (EndermanEntity) (Object) this;
            PitSandbox.onTeleport(e, endpractice$fromX, endpractice$fromY, endpractice$fromZ);
        }
    }
}
