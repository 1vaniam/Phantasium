package dev.phantasm.cache;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.math.Vec3d;

public final class EntityPositionCache {

    private static final EntityPositionCache INSTANCE = new EntityPositionCache();

    /* entityId -> last known position */
    private final Int2ObjectOpenHashMap<Vec3d> positions = new Int2ObjectOpenHashMap<>();

    private EntityPositionCache() {}

    public static EntityPositionCache get() { return INSTANCE; }

    public synchronized void put(int entityId, double x, double y, double z) {
        positions.put(entityId, new Vec3d(x, y, z));
    }

    public synchronized void remove(int entityId) {
        positions.remove(entityId);
    }

    public synchronized boolean anyWithin(double px, double py, double pz, double toleranceSq) {
        for (Vec3d pos : positions.values()) {
            double dx = pos.x - px, dy = pos.y - py, dz = pos.z - pz;
            if (dx * dx + dy * dy + dz * dz <= toleranceSq) return true;
        }
        return false;
    }

    public synchronized void clear() { positions.clear(); }
}
