package dev.phantasm.mixin;

import dev.phantasm.duck.EntityRenderStateEntityId;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererUpdateState<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "updateRenderState", at = @At("HEAD"))
    private void phantasm_stampEntityId(T entity, S state, float tickProgress, CallbackInfo ci) {
        ((EntityRenderStateEntityId) state).phantasm_setEntityId(entity.getId());
    }
}
