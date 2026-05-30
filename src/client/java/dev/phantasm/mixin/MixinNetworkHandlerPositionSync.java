package dev.phantasm.mixin;

import dev.phantasm.cache.EntityPositionCache;
import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.registry.FurnitureRegistry;
import dev.phantasm.registry.ModelEngineRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
S5  -Drop distant position packets for tracked entity IDs
IO2 - Drop position packets for static furniture
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinNetworkHandlerPositionSync {

    private static final double DISTANCE_THRESHOLD_SQ = 48.0 * 48.0;

    @Inject(method = "onEntityPosition", at = @At("HEAD"), cancellable = true)
    private void phantasm_onEntityPosition(EntityPositionS2CPacket packet, CallbackInfo ci) {
        if (!PhantasmConfig.get().enablePacketDrops) return;

        int id = packet.entityId();
        ModelEngineRegistry meReg = ModelEngineRegistry.get();
        FurnitureRegistry furReg  = FurnitureRegistry.get();

        boolean isMeBone    = meReg.isBone(id);
        boolean isFurniture = furReg.isFurniture(id);
        if (!isMeBone && !isFurniture) return;

        // Update position cache, packet only carries deltas, so read from the entity if available
        MinecraftClient mc = MinecraftClient.getInstance();
        long tick = mc.world != null ? mc.world.getTime() : 0L;
        if (mc.world != null) {
            net.minecraft.entity.Entity e = mc.world.getEntityById(id);
            if (e != null) EntityPositionCache.get().put(id, e.getX(), e.getY(), e.getZ());
        }

        // IO2: drop position packet for static furniture
        if (isFurniture) {
            if (furReg.isStatic(id, tick)) {
                ci.cancel();
                return;
            }
            furReg.recordMovement(id, tick);
        }

        // S5: drop beyond distance threshold
        if (mc.player != null && mc.world != null) {
            net.minecraft.entity.Entity entity = mc.world.getEntityById(id);
            if (entity != null) {
                double dx = entity.getX() - mc.player.getX();
                double dy = entity.getY() - mc.player.getY();
                double dz = entity.getZ() - mc.player.getZ();
                if (dx * dx + dy * dy + dz * dz > DISTANCE_THRESHOLD_SQ) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void phantasm_onEntityVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        if (!PhantasmConfig.get().enablePacketDrops) return;

        int id = packet.getEntityId();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        net.minecraft.entity.Entity entity = mc.world.getEntityById(id);
        if (entity == null) return;

        boolean isMeBone    = ModelEngineRegistry.get().isBone(id);
        boolean isFurniture = FurnitureRegistry.get().isFurniture(id);
        if (!isMeBone && !isFurniture) return;

        double dx = entity.getX() - mc.player.getX();
        double dy = entity.getY() - mc.player.getY();
        double dz = entity.getZ() - mc.player.getZ();

        // For ME bones: drop only if beyond distance threshold
        if (isMeBone && dx * dx + dy * dy + dz * dz > DISTANCE_THRESHOLD_SQ) {
            ci.cancel();
            return;
        }

        // For confirmed furniture: always drop velocity, furniture is static
        if (isFurniture) {
            ci.cancel();
        }
    }
}
