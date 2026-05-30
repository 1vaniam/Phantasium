package dev.phantasm.mixin;

import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.registry.FurnitureRegistry;
import dev.phantasm.registry.ModelEngineRegistry;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class MixinHitboxSkip {

    @Inject(method = "drawHitbox", at = @At("HEAD"), cancellable = true)
    private void phantasm_skipHitbox(Entity entity, float tickProgress, boolean inLocalServer,
                                      CallbackInfo ci) {
        if (!PhantasmConfig.get().enableHitboxSkip) return;
        int id = entity.getId();
        if (ModelEngineRegistry.get().isBone(id) || FurnitureRegistry.get().isFurniture(id)) {
            ci.cancel();
        }
    }
}
