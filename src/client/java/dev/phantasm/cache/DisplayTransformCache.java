package dev.phantasm.cache;

import net.minecraft.entity.decoration.DisplayEntity;

import java.util.concurrent.ConcurrentHashMap;

/*ItemDisplay transform cache

 Caches the DisplayEntity.RenderState (an immutable record) per entity ID
 Invalidated by MixinNetworkHandlerTrackerUpdate on every SetEntityData packet,
 ensuring stale transforms are never used after an update*/
public final class DisplayTransformCache {

    private static final DisplayTransformCache INSTANCE = new DisplayTransformCache();

    private final ConcurrentHashMap<Integer, DisplayEntity.RenderState> cache = new ConcurrentHashMap<>();

    private DisplayTransformCache() {}

    public static DisplayTransformCache get() { return INSTANCE; }

    public DisplayEntity.RenderState get(int entityId) {
        return cache.get(entityId);
    }

    public void put(int entityId, DisplayEntity.RenderState state) {
        cache.put(entityId, state);
    }

    public void invalidate(int entityId) {
        cache.remove(entityId);
    }

    public void remove(int entityId) {
        cache.remove(entityId);
    }

    public void clear() {
        cache.clear();
    }
}
