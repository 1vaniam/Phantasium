package dev.phantasm.mixin;

import dev.phantasm.cache.DisplayTransformCache;
import dev.phantasm.registry.FurnitureRegistry;
import dev.phantasm.registry.ModelEngineRegistry;
import dev.phantasm.registry.ServerPluginDetector;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 ME3 - Detect server-side bone visibility cull
 IO1 (Stage 2) - Confirm furniture with cosmetic false-positive checks
 NX1 - Debounce rapid metadata packets from Nexo furniture
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinNetworkHandlerTrackerUpdate {

    private static final int ENTITY_FLAGS_INDEX      = 0;
    private static final int CUSTOM_NAME_INDEX       = 2;
    private static final int CUSTOM_MODEL_DATA_INDEX = 8;
    private static final int ARMOR_STAND_FLAGS_INDEX = 15;
    private static final int ITEM_DISPLAY_SLOT_INDEX = 23;
    private static final byte INVISIBLE_FLAG         = 0x20;
    private static final byte ARMOR_STAND_MARKER_FLAG = 0x10; // marker = no hitbox, no collision

    @Unique
    private final Int2LongOpenHashMap phantasm_nexoDebounce = new Int2LongOpenHashMap();

    /* Prune debounce entries when entities are removed to prevent unbounded growth*/
    @Inject(method = "onEntitiesDestroy", at = @At("TAIL"))
    private void phantasm_pruneDebounceOnDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        if (phantasm_nexoDebounce == null) return;
        for (int id : packet.getEntityIds()) {
            phantasm_nexoDebounce.remove(id);
        }
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At("HEAD"), cancellable = true)
    private void phantasm_onEntityTrackerUpdate(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci) {
        if (packet.trackedValues() == null) return;
        int entityId = packet.id();

        // NX1: debounce rapid Nexo metadata bursts
        if (phantasm_nexoDebounce != null
                && ServerPluginDetector.get().hasNexo()
                && FurnitureRegistry.get().isFurniture(entityId)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            long currentTick = mc.world != null ? mc.world.getTime() : -1L;
            long lastTick = phantasm_nexoDebounce.getOrDefault(entityId, -2L);
            if (currentTick >= 0 && currentTick == lastTick) {
                ci.cancel();
                return;
            }
            phantasm_nexoDebounce.put(entityId, currentTick);
        }

        ModelEngineRegistry meReg = ModelEngineRegistry.get();
        if (meReg.isServerCulled(entityId)) {
            ci.cancel();
            return;
        }

        boolean isMeBone           = meReg.isBone(entityId);
        boolean hasCustomModelData = false;
        boolean hasCustomName      = false;
        boolean isMarkerArmorStand = false;
        String  itemNamespace      = null;   // from item display slot
        String  nameNamespace      = null;   // from custom name text component

        for (var entry : packet.trackedValues()) {
            int index = entry.id();

            // ME3: invisible flag
            if (isMeBone && index == ENTITY_FLAGS_INDEX
                    && entry.handler() == TrackedDataHandlerRegistry.BYTE) {
                byte flags = (Byte) entry.value();
                if ((flags & INVISIBLE_FLAG) != 0) meReg.markInvisible(entityId);
                else                                meReg.markVisible(entityId);
            }

            // Custom name, check for plugin namespace prefix e.g. "ia:chair"
            if (index == CUSTOM_NAME_INDEX && entry.value() instanceof java.util.Optional<?> opt
                    && opt.isPresent() && opt.get() instanceof net.minecraft.text.Text text) {
                String raw = text.getString();
                hasCustomName = true;
                // Some plugins embed their namespace in the custom name as an identifier
                nameNamespace = extractNamespace(raw);
            }

            // Custom model data
            if (index == CUSTOM_MODEL_DATA_INDEX) {
                Object val = entry.value();
                hasCustomModelData = (val instanceof Integer i && i != 0)
                        || (val instanceof java.util.OptionalInt oi
                            && oi.isPresent() && oi.getAsInt() != 0);
            }

            // Armor stand flags - detect marker stands (IA/Oraxen use these for hitbox-less furniture)
            if (index == ARMOR_STAND_FLAGS_INDEX
                    && entry.handler() == TrackedDataHandlerRegistry.BYTE) {
                byte asFlags = (Byte) entry.value();
                isMarkerArmorStand = (asFlags & ARMOR_STAND_MARKER_FLAG) != 0;
                // Marker armor stands are strongly indicative of plugin furniture
                if (isMarkerArmorStand) hasCustomModelData = true;
            }

            // Item display slot - direct namespace read
            if (index == ITEM_DISPLAY_SLOT_INDEX
                    && entry.value() instanceof ItemStack stack && !stack.isEmpty()) {
                itemNamespace = Registries.ITEM.getId(stack.getItem()).getNamespace();
                if (!hasCustomModelData) {
                    hasCustomModelData = stack.getComponents()
                            .contains(net.minecraft.component.DataComponentTypes.CUSTOM_MODEL_DATA);
                }
            }
        }

        // Confirm furniture
        if (hasCustomModelData) {
            MinecraftClient mc = MinecraftClient.getInstance();
            FurnitureRegistry furReg = FurnitureRegistry.get();

            // Resolve namespace: item display wins, then name, then plugin detection fallback
            String resolvedNamespace = itemNamespace != null ? itemNamespace
                    : nameNamespace != null ? nameNamespace
                    : inferFromActivePlugins();

            if (mc.world != null) {
                Entity entity = mc.world.getEntityById(entityId);
                boolean isPassengerOfPlayer = false;
                double  currentY = Double.NaN;

                if (entity != null) {
                    currentY = entity.getY();
                    if (entity.getVehicle() instanceof net.minecraft.entity.player.PlayerEntity) {
                        isPassengerOfPlayer = true;
                    }
                }

                // Marker armor stands: skip the custom name rejection. plugins use them
                // with names for internal tracking, not for display
                boolean effectiveHasCustomName = hasCustomName && !isMarkerArmorStand;

                furReg.confirmFurniture(entityId, true, isPassengerOfPlayer,
                        effectiveHasCustomName, currentY, mc.world.getTime(), resolvedNamespace);
            } else {
                furReg.confirmFurniture(entityId, true);
            }
        }

        DisplayTransformCache.get().invalidate(entityId);
    }

    /*
     Try to extract a plugin namespace from a raw name string.
     e.g. "ia:chair_wood" → "ia", "oraxen:table" → "oraxen"
     */
    private static String extractNamespace(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        int colon = raw.indexOf(':');
        if (colon > 0 && colon < raw.length() - 1) {
            String ns = raw.substring(0, colon).toLowerCase().trim();
            // Only return if it looks like a known plugin namespace
            return switch (ns) {
                case "ia", "itemsadder", "oraxen", "o", "nexo" -> ns;
                default -> null;
            };
        }
        return null;
    }

    /*
     Fallback for armor stands with no item namespace and no name namespace
     If exactly one plugin is active, attribute to it Otherwise null = GENERIC
    */
    private static String inferFromActivePlugins() {
        ServerPluginDetector det = ServerPluginDetector.get();
        boolean hasIA  = det.isItemsAdderLatched();
        boolean hasOX  = det.isOraxenLatched();
        boolean hasNX  = det.isNexoLatched();
        int count = (hasIA ? 1 : 0) + (hasOX ? 1 : 0) + (hasNX ? 1 : 0);
        if (count != 1) return null;
        if (hasIA)  return "ia";
        if (hasOX)  return "oraxen";
        return "nexo";
    }
}
