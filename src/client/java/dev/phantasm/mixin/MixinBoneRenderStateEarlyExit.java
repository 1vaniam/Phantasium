package dev.phantasm.mixin;

import dev.phantasm.cache.BackfaceCullCounter;
import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.duck.EntityRenderStateEntityId;
import dev.phantasm.registry.ModelEngineRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = EntityRenderer.class, priority = 900)
public abstract class MixinBoneRenderStateEarlyExit<S extends EntityRenderState> {

    private static final float BACKFACE_THRESHOLD = 0.1f;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void phantasm_earlyExitServerCulledBone(S renderState,
                                                     MatrixStack matrices,
                                                     OrderedRenderCommandQueue queue,
                                                     CameraRenderState cameraState,
                                                     CallbackInfo ci) {

        PhantasmConfig cfg = PhantasmConfig.get();

        //existing server-cull check
        if (cfg.enableFrustumCulling) {
            int entityId = ((EntityRenderStateEntityId) renderState).phantasm_getEntityId();
            if (ModelEngineRegistry.get().isServerCulled(entityId)) {
                ci.cancel();
                return;
            }
        }

        //new back-face cull check
        if (!cfg.enableBackFaceCulling) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        int entityId = ((EntityRenderStateEntityId) renderState).phantasm_getEntityId();

        // Only cull ME bones, leave furniture/display entities alone
        if (!ModelEngineRegistry.get().isBone(entityId)) return;

        // Safety guard: skip culling for very close bones to avoid pop-in
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();

        net.minecraft.entity.Entity entity = mc.world.getEntityById(entityId);
        if (entity == null) return;

        double dx = camPos.x - entity.getX();
        double dy = camPos.y - entity.getY();
        double dz = camPos.z - entity.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        double closeLimit = cfg.closeDistanceBlocks;
        if (distSq < closeLimit * closeLimit) return;

        Matrix4f pose = matrices.peek().getPositionMatrix();
        Vector3f boneForward = new Vector3f(pose.m02(), pose.m12(), pose.m22()).normalize();

        double invLen = 1.0 / Math.sqrt(distSq);
        float toCamX = (float) (dx * invLen);
        float toCamY = (float) (dy * invLen);
        float toCamZ = (float) (dz * invLen);

        float dot = boneForward.x * toCamX + boneForward.y * toCamY + boneForward.z * toCamZ;

        if (dot < BACKFACE_THRESHOLD) {
            BackfaceCullCounter.increment();
            ci.cancel();
        }
    }
}
