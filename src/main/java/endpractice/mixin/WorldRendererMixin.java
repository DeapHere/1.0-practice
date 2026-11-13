package endpractice.mixin;

import endpractice.PitPlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Culler;
import net.minecraft.client.render.world.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pit planner: the line to run, drawn after the entities. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow private Minecraft minecraft;

    @Inject(method = "renderEntities", at = @At("TAIL"), require = 0)
    private void endpractice$pitLine(Vec3d camera, Culler culler, float tickDelta, CallbackInfo ci) {
        PitPlanner.render(this.minecraft, tickDelta);
    }
}
