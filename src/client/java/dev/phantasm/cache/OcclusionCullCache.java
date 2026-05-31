package dev.phantasm.cache;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.concurrent.locks.StampedLock;

/*S3 - Async occlusion cull results*/
public final class OcclusionCullCache {

    private static final OcclusionCullCache INSTANCE = new OcclusionCullCache();

    private final IntOpenHashSet occludedIds = new IntOpenHashSet();
    private final StampedLock lock = new StampedLock();

    private OcclusionCullCache() {}

    public static OcclusionCullCache get() { return INSTANCE; }

    public void markOccluded(int entityId) {
        long stamp = lock.writeLock();
        try { occludedIds.add(entityId); }
        finally { lock.unlockWrite(stamp); }
    }

    public void markVisible(int entityId) {
        long stamp = lock.writeLock();
        try { occludedIds.remove(entityId); }
        finally { lock.unlockWrite(stamp); }
    }

    public boolean isOccluded(int entityId) {
        // Optimistic read first - avoids acquiring the lock on the common (uncontended) path
        long stamp = lock.tryOptimisticRead();
        boolean result = occludedIds.contains(entityId);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try { result = occludedIds.contains(entityId); }
            finally { lock.unlockRead(stamp); }
        }
        return result;
    }

    public void remove(int entityId) {
        long stamp = lock.writeLock();
        try { occludedIds.remove(entityId); }
        finally { lock.unlockWrite(stamp); }
    }

    public void clear() {
        long stamp = lock.writeLock();
        try { occludedIds.clear(); }
        finally { lock.unlockWrite(stamp); }
    }
}
