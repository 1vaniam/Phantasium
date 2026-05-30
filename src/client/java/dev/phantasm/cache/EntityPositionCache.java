package dev.phantasm.cache;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.math.Vec3d;

/**
 Lightweight last-known-position cache for bone and furniture entities
 Updated by MixinNetworkHandlerEntities on spawn and position packets
 Used by MixinParticleSuppression to avoid iterating the entire entity list

 Only tracks entities already registered in ModelEngineRegistry or FurnitureRegistry
 All access on the main/network thread — no lock needed
 */
public final class EntityPositionCache {

    private static final EntityPositionCache INSTANCE = new EntityPositionCache();

    /* entityId = last known position */
    private final Int2ObjectOpenHashMap<Vec3d> positions = new Int2ObjectOpenHashMap<>();

    private EntityPositionCache() {}

    public static EntityPositionCache get() { return INSTANCE; }

    public void put(int entityId, double x, double y, double z) {
        positions.put(entityId, new Vec3d(x, y, z));
    }

    public void remove(int entityId) {
        positions.remove(entityId);
    }

    /*
     Returns true if any tracked entity is within toleranceSq (squared) of (px, py, pz).
     O(n) over only the registered entities, typically far fewer than all world entities.

     */
    public boolean anyWithin(double px, double py, double pz, double toleranceSq) {
        for (Vec3d pos : positions.values()) {
            double dx = pos.x - px, dy = pos.y - py, dz = pos.z - pz;
            if (dx * dx + dy * dy + dz * dz <= toleranceSq) return true;
        }
        return false;
    }

    public void clear() { positions.clear(); }
}
