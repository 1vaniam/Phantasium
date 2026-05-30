package dev.phantasm.registry;

import dev.phantasm.config.PhantasmConfig;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;

/* LOD-based bone update throttling */
public final class LodThrottleRegistry {

    private static final LodThrottleRegistry INSTANCE = new LodThrottleRegistry();

    /* rootId = game tick of last applied update */
    private final Int2LongOpenHashMap lastUpdateTick = new Int2LongOpenHashMap();

    private LodThrottleRegistry() {
        lastUpdateTick.defaultReturnValue(-1L);
    }

    public static LodThrottleRegistry get() { return INSTANCE; }

    /**
     Returns true if this bone group should receive a transform update this tick

     @param rootId      root / pivot entity ID
     @param distanceSq  squared distance from camera to root entity
     @param currentTick current world tick
     */
    public boolean shouldUpdate(int rootId, double distanceSq, long currentTick) {
        PhantasmConfig cfg = PhantasmConfig.get();
        if (!cfg.enableLodThrottling) return true;

        double threshold = cfg.lodDistanceBlocks;
        if (distanceSq <= threshold * threshold) return true; // within LOD range → always update

        long last = lastUpdateTick.get(rootId);
        if (last < 0 || (currentTick - last) >= cfg.lodSkipTicks) {
            lastUpdateTick.put(rootId, currentTick);
            return true;
        }
        return false;
    }

    public void remove(int rootId) {
        lastUpdateTick.remove(rootId);
    }

    public void clear() {
        lastUpdateTick.clear();
    }
}
