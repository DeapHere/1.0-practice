package endpractice.mixin;

import endpractice.EndPractice;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void endpractice$tickHead(CallbackInfo ci) {
        EndPractice.tickHead((Minecraft) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void endpractice$tickTail(CallbackInfo ci) {
        EndPractice.tickTail((Minecraft) (Object) this);
    }

    // a practice world goes straight to the End, so its overworld spawn area is never needed
    @Inject(method = "prepareWorld", at = @At("HEAD"), cancellable = true)
    private void endpractice$skipOverworldPrep(String message, CallbackInfo ci) {
        if (EndPractice.skipOverworldPrep) ci.cancel();
    }
}
