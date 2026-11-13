package endpractice.mixin;

import endpractice.SpawnOverlay;
import net.minecraft.entity.Entity;
import net.minecraft.world.NaturalSpawner;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spawn overlay: watches the vanilla spawner (it changes nothing). */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private static void endpractice$start(World world, boolean a, boolean b, CallbackInfoReturnable<Integer> cir) {
        SpawnOverlay.spawnerStart(world);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"), require = 0)
    private static boolean endpractice$spawned(World world, Entity entity) {
        boolean added = world.addEntity(entity);
        if (added) SpawnOverlay.spawned(entity);
        return added;
    }

    @Inject(method = "tick", at = @At("RETURN"), require = 0)
    private static void endpractice$end(World world, boolean a, boolean b, CallbackInfoReturnable<Integer> cir) {
        SpawnOverlay.spawnerEnd(world);
    }
}
