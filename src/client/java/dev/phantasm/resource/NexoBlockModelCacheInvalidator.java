package dev.phantasm.resource;

import dev.phantasm.cache.NexoBlockModelCache;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;


public class NexoBlockModelCacheInvalidator implements SimpleSynchronousResourceReloadListener {

    private static final Identifier ID = Identifier.of("phantasm", "nexo_block_model_cache");

    @Override
    public Identifier getFabricId() { return ID; }

    @Override
    public void reload(ResourceManager manager) {
        NexoBlockModelCache.get().invalidate();
    }
}
