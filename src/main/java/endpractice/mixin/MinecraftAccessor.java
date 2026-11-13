package endpractice.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    /** Vanilla's "Building terrain" step, run once a practice world's spawn has been accepted. */
    @Invoker("prepareWorld")
    void endpractice$prepareWorld(String message);
}
