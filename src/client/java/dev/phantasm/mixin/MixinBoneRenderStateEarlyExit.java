package dev.phantasm.mixin;

import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.duck.EntityRenderStateEntityId;
import dev.phantasm.registry.ModelEngineRegistry;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 Bone render state early exit - cancel render() at HEAD for server-culled
 bones before any state population or draw call runs.
 */
@Mixin(value = EntityRenderer.class, priority = 900)
public abstract class MixinBoneRenderStateEarlyExit<S extends EntityRenderState> {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void phantasm_earlyExitServerCulledBone(S renderState,
                                                     MatrixStack matrices,
                                                     OrderedRenderCommandQueue queue,
                                                     CameraRenderState cameraState,
                                                     CallbackInfo ci) {
        if (!PhantasmConfig.get().enableFrustumCulling) return;
        if (ModelEngineRegistry.get().isServerCulled(((EntityRenderStateEntityId) renderState).phantasm_getEntityId())) {
            ci.cancel();
        }
    }
}
