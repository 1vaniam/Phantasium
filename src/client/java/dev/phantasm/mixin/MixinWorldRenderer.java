package dev.phantasm.mixin;

import dev.phantasm.cache.ClientFrustumCache;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 S1 - Per-frame frustum snapshot
 setupFrustum(Matrix4f modelView, Matrix4f projection, Vec3d pos) = Frustum
*/
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @Inject(method = "setupFrustum", at = @At("RETURN"))
    private void phantasm_captureFrustum(Matrix4f modelView, Matrix4f projection,
                                          Vec3d pos,
                                          CallbackInfoReturnable<Frustum> cir) {
        Frustum frustum = cir.getReturnValue();
        if (frustum != null) {
            ClientFrustumCache.get().update(frustum);
        }
    }
}
