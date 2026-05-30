package dev.phantasm.mixin;

import dev.phantasm.cache.OcclusionCullCache;
import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.registry.FurnitureRegistry;
import dev.phantasm.registry.ModelEngineRegistry;
import dev.phantasm.registry.PlayerArmorRegistry;
import dev.phantasm.util.AsyncOcclusionWorker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 S2 — Frustum + back-angle cull via shouldRender
 ME2 — Group-level culling for ModelEngine bones (one check per model, not per bone)
 ME3 — Instantly reject server-culled (invisible) ME bones
 PA2 — Cull player-worn custom armor stands/item displays when the player is culled
*/
@Mixin(EntityRenderer.class)
public abstract class MixinShouldRender<T extends Entity> {

    private static final double BACK_ANGLE_COS = Math.cos(Math.toRadians(130.0));
    private static final double BOX_EXPAND     = 1.0;

    @Inject(method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z",
            at = @At("HEAD"), cancellable = true)
    private void phantasm_shouldRender(T entity, Frustum frustum,
                                       double camX, double camY, double camZ,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof DisplayEntity.ItemDisplayEntity)
                && !(entity instanceof ArmorStandEntity)) return;

        int id = entity.getId();
        ModelEngineRegistry meReg  = ModelEngineRegistry.get();
        FurnitureRegistry   furReg = FurnitureRegistry.get();
        PlayerArmorRegistry paReg  = PlayerArmorRegistry.get();

        // ME3: server-culled bone = reject immediately
        if (meReg.isServerCulled(id)) {
            cir.setReturnValue(false);
            return;
        }

        boolean isBone        = meReg.isBone(id);
        boolean isFurniture   = furReg.isFurniture(id);
        boolean isPlayerArmor = paReg.isPlayerArmor(id);

        if (!isBone && !isFurniture && !isPlayerArmor) return;

        // PA2: player-worn armor - cull based on the player's bounding box.
        if (isPlayerArmor) {
            int playerId = paReg.getPlayerId(id);
            if (playerId >= 0) {
                Entity player = entity.getEntityWorld().getEntityById(playerId);
                if (player != null) {
                    Box playerBox = player.getBoundingBox().expand(BOX_EXPAND);
                    if (!frustum.isVisible(playerBox)) {
                        cir.setReturnValue(false);
                        return;
                    }
                    Vec3d cam = new Vec3d(camX, camY, camZ);
                    double closeDist = PhantasmConfig.get().closeDistanceBlocks;
                    if (playerBox.getCenter().squaredDistanceTo(cam) < closeDist * closeDist) return;

                    if (OcclusionCullCache.get().isOccluded(playerId)) {
                        cir.setReturnValue(false);
                        return;
                    }
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.gameRenderer != null && mc.world != null) {
                        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
                        AsyncOcclusionWorker.get().submit(playerId, playerBox.getCenter(), cameraPos, mc.world);
                    }
                    return;
                }
            }
            return;
        }

        // determine bounding box for ME bones and furniture
        Box cullBox;
        int checkId;
        if (isBone) {
            int rootId = meReg.getRootId(id);
            if (rootId < 0) return;
            Entity root = entity.getEntityWorld().getEntityById(rootId);
            if (root == null) return;
            cullBox = root.getBoundingBox();
            checkId = rootId;
        } else {
            cullBox = entity.getBoundingBox();
            checkId = id;
        }
        cullBox = cullBox.expand(BOX_EXPAND);

        Vec3d cam    = new Vec3d(camX, camY, camZ);
        double distSq = cullBox.getCenter().squaredDistanceTo(cam);

        // S2a: Frustum cull
        if (!frustum.isVisible(cullBox)) {
            if (isFurniture) furReg.incrementCulled();
            cir.setReturnValue(false);
            return;
        }

        // LOD: hard render distance cutoff
        double lodRenderDist = PhantasmConfig.get().lodRenderDistanceBlocks;
        if (distSq > lodRenderDist * lodRenderDist) {
            if (isFurniture) furReg.incrementCulled();
            cir.setReturnValue(false);
            return;
        }

        double closeDist = PhantasmConfig.get().closeDistanceBlocks;
        if (distSq < closeDist * closeDist) return;

        // S2b: Back-angle cull
        Vec3d toEntity = cullBox.getCenter().subtract(cam).normalize();
        if (isBehindCamera(toEntity)) {
            if (isFurniture) furReg.incrementCulled();
            cir.setReturnValue(false);
            return;
        }

        // S3: Async occlusion cull
        if (OcclusionCullCache.get().isOccluded(checkId)) {
            if (isFurniture) furReg.incrementCulled();
            cir.setReturnValue(false);
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null || mc.world == null) return;
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        AsyncOcclusionWorker.get().submit(checkId, cullBox.getCenter(), cameraPos, mc.world);
    }

    private boolean isBehindCamera(Vec3d toEntity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null) return false;
        Vec3d fwd = Vec3d.fromPolar(
            mc.gameRenderer.getCamera().getPitch(),
            mc.gameRenderer.getCamera().getYaw()
        );
        return fwd.dotProduct(toEntity) < BACK_ANGLE_COS;
    }
}
