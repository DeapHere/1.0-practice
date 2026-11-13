package endpractice.mixin;

import endpractice.Commands;
import endpractice.EndPractice;
import net.minecraft.client.entity.mob.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Singleplayer chat goes to the commands (multiplayer's player overrides sendChat), and spectators aren't pushed out of blocks. */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerMixin {
    @Inject(method = "sendChat", at = @At("HEAD"))
    private void endpractice$chat(String content, CallbackInfo ci) {
        if (EndPractice.mc != null && (Object) this == EndPractice.mc.player) Commands.onChat(EndPractice.mc, content);
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void endpractice$noPush(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (Commands.spectating()) cir.setReturnValue(false);
    }
}
