package dev.phantasm.mixin;

import dev.phantasm.cache.DisplayTransformCache;
import dev.phantasm.cache.EntityPositionCache;
import dev.phantasm.cache.OcclusionCullCache;
import dev.phantasm.registry.FurnitureRegistry;
import dev.phantasm.registry.ModelEngineRegistry;
import dev.phantasm.registry.PlayerArmorRegistry;
import dev.phantasm.registry.ServerPluginDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 IO1 - Stage furniture candidates from AddEntity packets
 IO1b - Upgrade source tag from equipment packets
 Cleanup - RemoveEntities clears all registries.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinNetworkHandlerEntities {

    @Inject(method = "onEntitySpawn", at = @At("TAIL"))
    private void phantasm_onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        EntityType<?> type = packet.getEntityType();
        if (type == EntityType.ITEM_DISPLAY || type == EntityType.ARMOR_STAND) {
            MinecraftClient mc = MinecraftClient.getInstance();
            long tick = mc.world != null ? mc.world.getTime() : 0L;
            FurnitureRegistry.get().stagePending(packet.getEntityId(), packet.getY(), tick);
            EntityPositionCache.get().put(packet.getEntityId(), packet.getX(), packet.getY(), packet.getZ());
        }
        if (ServerPluginDetector.get().hasNexo()) {
            if (type == EntityType.INTERACTION
                    || type == EntityType.SHULKER
                    || type == EntityType.GHAST) {
                FurnitureRegistry.get().registerDirect(
                        packet.getEntityId(), FurnitureRegistry.Source.NEXO);
            }
        }
    }

    @Inject(method = "onEntitiesDestroy", at = @At("TAIL"))
    private void phantasm_onEntitiesDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        for (int id : packet.getEntityIds()) {
            ModelEngineRegistry.get().removeEntity(id);
            FurnitureRegistry.get().removeEntity(id);
            PlayerArmorRegistry.get().removeEntity(id);
            OcclusionCullCache.get().remove(id);
            DisplayTransformCache.get().remove(id);
            EntityPositionCache.get().remove(id);
        }
    }

    /*
     IO1b Equipment packet hook for armor stand furniture
     IA/Oraxen send the custom model item in the HEAD equipment slot of armor stands
     This upgrades a GENERIC-tagged entity to the correct plugin source
     */
    @Inject(method = "onEntityEquipmentUpdate", at = @At("TAIL"))
    private void phantasm_onEntityEquipment(EntityEquipmentUpdateS2CPacket packet, CallbackInfo ci) {
        FurnitureRegistry furReg = FurnitureRegistry.get();
        int entityId = packet.getEntityId();
        if (!furReg.isFurniture(entityId) && !furReg.isPending(entityId)) return;

        for (var entry : packet.getEquipmentList()) {
            ItemStack stack = entry.getSecond();
            if (stack == null || stack.isEmpty()) continue;
            String namespace = Registries.ITEM.getId(stack.getItem()).getNamespace();
            FurnitureRegistry.Source src = FurnitureRegistry.inferSourcePublic(namespace);
            if (src != FurnitureRegistry.Source.GENERIC) {
                furReg.upgradeSource(entityId, src);
                break;
            }
        }
    }
}
