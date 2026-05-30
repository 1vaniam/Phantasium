package dev.phantasm.mixin;

import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.registry.FurnitureRegistry;
import dev.phantasm.registry.ModelEngineRegistry;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinLabelSkip<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "getAndUpdateRenderState", at = @At("RETURN"))
    private void phantasm_skipLabel(T entity, float tickDelta,
                                     CallbackInfoReturnable<S> cir) {
        if (!PhantasmConfig.get().enableLabelSkip) return;
        S state = cir.getReturnValue();
        if (state == null) return;
        int id = entity.getId();
        if (ModelEngineRegistry.get().isBone(id) || FurnitureRegistry.get().isFurniture(id)) {
            state.displayName = null;
        }
    }
}
