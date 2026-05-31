package dev.phantasm;

import dev.phantasm.cache.DisplayTransformCache;
import dev.phantasm.cache.OcclusionCullCache;
import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.network.*;
import dev.phantasm.registry.*;
import dev.phantasm.registry.PlayerArmorRegistry;
import dev.phantasm.util.AtlasDiskCache;
import dev.phantasm.util.AtlasSaveQueue;
import dev.phantasm.util.PackHashCache;
import dev.phantasm.util.PuaCodepointRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhantasmClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("phantasm");

    @Override
    public void onInitializeClient() {

        // Load config from disk (creates defaults on first run)
        PhantasmConfig.load();

        // da ModelEngine bulk_data
        PayloadTypeRegistry.playS2C().register(BulkEntityDataPayload.ID, BulkEntityDataPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(BulkEntityDataPayload.ID, (payload, ctx) -> {
            ServerPluginDetector.get().detectModelEngine();
            BulkDataHandler.handle(payload);
        });

        // ItemsAdder, Oraxen, and Nexo does not send client-bound handshake
        // channel packets. Detection for those plugins is entity-based
        // ServerPluginDetector.hasOraxen() / hasItemsAdder() / hasNexo() which
        // check FurnitureRegistry for confirmed furniture with the matching source tag

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null) {
                FurnitureRegistry.get().evictStalePending(client.world.getTime());
            }
            dev.phantasm.util.AsyncOcclusionWorker.get().clearPending();
            ModelEngineRegistry.get().flushPending();
            // TX4 shi drain one pending atlas save per tick (render thread, GL context available)
            AtlasSaveQueue.drainOne();
        });

        // NX2 - invalidate block model cache on resource reload
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(
                net.minecraft.resource.ResourceType.CLIENT_RESOURCES)
            .registerReloadListener(new dev.phantasm.resource.NexoBlockModelCacheInvalidator());

        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(
                net.minecraft.resource.ResourceType.CLIENT_RESOURCES)
            .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                private static final net.minecraft.util.Identifier ID =
                    net.minecraft.util.Identifier.of("phantasm", "injection_guard");

                @Override
                public net.minecraft.util.Identifier getFabricId() { return ID; }

                @Override
                public void reload(net.minecraft.resource.ResourceManager manager) {

                    var ns = manager.getAllNamespaces();
                    if (ns.contains("oraxen"))      ServerPluginDetector.get().detectOraxen();
                    if (ns.contains("itemsadder"))  ServerPluginDetector.get().detectItemsAdder();
                    if (ns.contains("nexo"))        ServerPluginDetector.get().detectNexo();
                    if (ns.contains("modelengine")) ServerPluginDetector.get().detectModelEngine();

                    // PUA glyph set is rebuilt from the new pack, clear stale entries
                    PuaCodepointRegistry.clear();

                    // AtlasManager (vanilla) reloads before mod listeners
                    if (!dev.phantasm.util.AtlasInjectionGuard.INJECTION_FIRED.get()) {
                        LOGGER.warn("[Phantasm] TX1/TX2 injections did not fire - mipmap cap and parallel load inactive.");
                    }
                    dev.phantasm.util.AtlasInjectionGuard.INJECTION_FIRED.set(false);
                }
            });

        //reset all state
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerPluginDetector.get().reset();
            DisplayTransformCache.get().clear();
            ModelEngineRegistry.get().clear();
            FurnitureRegistry.get().clear();
            PlayerArmorRegistry.get().clear();
            LodThrottleRegistry.get().clear();
            OcclusionCullCache.get().clear();
            PackHashCache.reset();
            AtlasDiskCache.reset();
            PuaCodepointRegistry.clear();
            dev.phantasm.cache.EntityPositionCache.get().clear();
        });

        // Dimension change, flush bone + furniture registries
        // Uses the correct dimension-change event instead of join (which only fires once).
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {});
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // JOIN fires on initial server
            ModelEngineRegistry.get().clear();
            FurnitureRegistry.get().clear();
            PlayerArmorRegistry.get().clear();
            LodThrottleRegistry.get().clear();
            DisplayTransformCache.get().clear();
            OcclusionCullCache.get().clear();
            LOGGER.info("[Phantasm] Server join — registries flushed.");
        });

        LOGGER.info("[Phantasm] Initialized — culling + ME bulk packet optimization active.");
    }
}
