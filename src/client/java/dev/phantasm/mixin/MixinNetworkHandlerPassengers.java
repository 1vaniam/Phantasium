package dev.phantasm.mixin;

import dev.phantasm.registry.ModelEngineRegistry;
import dev.phantasm.registry.PlayerArmorRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 ME1 - Intercept SetPassengers packets to build the bone group registry
 PA1 - Detect ArmorStand/ItemDisplay passengers riding a player (custom armor)
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinNetworkHandlerPassengers {

    @Inject(method = "onEntityPassengersSet", at = @At("TAIL"))
    private void phantasm_onEntityPassengersSet(EntityPassengersSetS2CPacket packet, CallbackInfo ci) {
        int rootId = packet.getEntityId();
        int[] passengerIds = packet.getPassengerIds();
        if (passengerIds.length == 0) return;

        ModelEngineRegistry.get().registerGroup(rootId, passengerIds);
        dev.phantasm.registry.ServerPluginDetector.get().detectModelEngine();

        // PA1 if the vehicle is a player, register as player armor
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        Entity vehicle = mc.world.getEntityById(rootId);
        if (!(vehicle instanceof PlayerEntity)) return;

        int[] armorPassengers = java.util.Arrays.stream(passengerIds)
                .filter(pid -> {
                    Entity e = mc.world.getEntityById(pid);
                    return e != null && (e.getType() == EntityType.ARMOR_STAND
                            || e.getType() == EntityType.ITEM_DISPLAY);
                })
                .toArray();

        if (armorPassengers.length > 0) {
            PlayerArmorRegistry.get().registerPlayerArmor(rootId, armorPassengers);
        }
    }
}
