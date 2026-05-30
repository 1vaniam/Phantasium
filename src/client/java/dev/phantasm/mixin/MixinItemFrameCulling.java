package dev.phantasm.mixin;

import dev.phantasm.cache.ClientFrustumCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*IF1 - Frustum + back-angle cull for item frames and glow item frames*/
@Mixin(EntityRenderer.class)
public abstract class MixinItemFrameCulling<T extends Entity> {

    private static final double BACK_ANGLE_COS = Math.cos(Math.toRadians(100.0));

    @Inject(method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z",
            at = @At("HEAD"), cancellable = true)
    private void phantasm_cullItemFrame(T entity, net.minecraft.client.render.Frustum frustum,
                                        double camX, double camY, double camZ,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ItemFrameEntity) && !(entity instanceof GlowItemFrameEntity)) {
            return;
        }

        Box box = entity.getBoundingBox();

        // Frustum cull
        if (!ClientFrustumCache.get().isVisible(box)) {
            cir.setReturnValue(false);
            return;
        }

        // Seyan angle cull
        Vec3d cam = new Vec3d(camX, camY, camZ);
        Vec3d toFrame = box.getCenter().subtract(cam).normalize();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer != null) {
            Vec3d forward = Vec3d.fromPolar(
                mc.gameRenderer.getCamera().getPitch(),
                mc.gameRenderer.getCamera().getYaw()
            );
            if (forward.dotProduct(toFrame) < BACK_ANGLE_COS) {
                cir.setReturnValue(false);
            }
        }
    }
}
