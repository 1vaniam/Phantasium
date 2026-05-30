package dev.phantasm.registry;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/*IO1 — Furniture entity registry for ItemsAdder, Oraxen, Nexo, and generic custom-model entities*/
public final class FurnitureRegistry {

    public enum Source { ORAXEN, ITEMS_ADDER, NEXO, GENERIC }

    /** Holds all per-entity state in a single object to cut map lookups from 5 - 1 */
    private static final class FurnitureEntry {
        Source source;          // null = pending (not yet confirmed)
        double spawnY;
        long   spawnTick;
        long   lastMoveTick;

        FurnitureEntry(double spawnY, long spawnTick) {
            this.source      = null; // pending
            this.spawnY      = spawnY;
            this.spawnTick   = spawnTick;
            this.lastMoveTick = -1L;
        }

        FurnitureEntry(Source source) {
            this.source       = source;
            this.spawnY       = Double.NaN;
            this.spawnTick    = -1L;
            this.lastMoveTick = -1L;
        }

        boolean isPending()   { return source == null; }
        boolean isConfirmed() { return source != null; }
    }

    private static final FurnitureRegistry INSTANCE = new FurnitureRegistry();

    private static final long MAX_PENDING_TICKS = 200;

    /** Single map for all entity state — pending and confirmed */
    private final Int2ObjectOpenHashMap<FurnitureEntry> entries = new Int2ObjectOpenHashMap<>();

    private volatile boolean hasOraxen     = false;
    private volatile boolean hasItemsAdder = false;
    private volatile boolean hasNexo       = false;

    public static final int STATIC_THRESHOLD = 60;

    private FurnitureRegistry() {}

    public static FurnitureRegistry get() { return INSTANCE; }

    /** Stage 1: AddEntity packet seen for a candidate type */
    public void stagePending(int entityId, double spawnY, long currentTick) {
        entries.put(entityId, new FurnitureEntry(spawnY, currentTick));
    }

    public void confirmFurniture(int entityId, boolean hasCustomModelData,
                                  boolean isPassengerOfPlayer,
                                  boolean hasCustomName,
                                  double currentY,
                                  long currentTick,
                                  String itemNamespace) {
        FurnitureEntry e = entries.get(entityId);
        if (e == null || !e.isPending() || !hasCustomModelData) { entries.remove(entityId); return; }
        if (isPassengerOfPlayer || hasCustomName)                { entries.remove(entityId); return; }
        if (!Double.isNaN(e.spawnY) && Math.abs(currentY - e.spawnY) > 0.1) { entries.remove(entityId); return; }

        Source src = inferSource(itemNamespace);
        e.source = src;
        markSource(src);
    }

    public void confirmFurniture(int entityId, boolean hasCustomModelData) {
        FurnitureEntry e = entries.get(entityId);
        if (e == null || !hasCustomModelData) { entries.remove(entityId); return; }
        e.source = Source.GENERIC;
        markSource(Source.GENERIC);
    }

    public void registerDirect(int entityId, Source source) {
        entries.put(entityId, new FurnitureEntry(source));
        markSource(source);
    }

    public void removeEntity(int entityId) {
        entries.remove(entityId);
    }

    public void evictStalePending(long currentTick) {
        entries.int2ObjectEntrySet().removeIf(entry -> {
            FurnitureEntry e = entry.getValue();
            return e.isPending() && e.spawnTick >= 0 && (currentTick - e.spawnTick) > MAX_PENDING_TICKS;
        });
    }

    public void clear() {
        entries.clear();
        hasOraxen     = false;
        hasItemsAdder = false;
        hasNexo       = false;
    }

    private static Source inferSource(String namespace) {
        if (namespace == null) return Source.GENERIC;
        return switch (namespace) {
            case "ia", "itemsadder" -> Source.ITEMS_ADDER;
            case "oraxen", "o"      -> Source.ORAXEN;
            case "nexo"             -> Source.NEXO;
            default                 -> Source.GENERIC;
        };
    }

    //Queries
    public boolean isFurniture(int entityId) {
        FurnitureEntry e = entries.get(entityId);
        return e != null && e.isConfirmed();
    }

    public boolean isPending(int entityId) {
        FurnitureEntry e = entries.get(entityId);
        return e != null && e.isPending();
    }

    private final java.util.concurrent.atomic.AtomicInteger frameCulledCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public int getFurnitureCount() {
        int count = 0;
        for (FurnitureEntry e : entries.values()) if (e.isConfirmed()) count++;
        return count;
    }
    public void incrementCulled()    { frameCulledCount.incrementAndGet(); }
    public int  getAndResetCulled()  { return frameCulledCount.getAndSet(0); }

    public Source getSource(int entityId) {
        FurnitureEntry e = entries.get(entityId);
        return e != null ? e.source : null;
    }
    public boolean isOraxen(int entityId)     { return getSource(entityId) == Source.ORAXEN; }
    public boolean isItemsAdder(int entityId) { return getSource(entityId) == Source.ITEMS_ADDER; }
    public boolean isNexo(int entityId)       { return getSource(entityId) == Source.NEXO; }

    public boolean hasOraxenFurniture()     { return hasOraxen; }
    public boolean hasItemsAdderFurniture() { return hasItemsAdder; }
    public boolean hasNexoFurniture()       { return hasNexo; }

    private void markSource(Source src) {
        switch (src) {
            case ORAXEN      -> { hasOraxen     = true; ServerPluginDetector.get().detectOraxen(); }
            case ITEMS_ADDER -> { hasItemsAdder = true; ServerPluginDetector.get().detectItemsAdder(); }
            case NEXO        -> { hasNexo       = true; ServerPluginDetector.get().detectNexo(); }
            default          -> {}
        }
    }

    //IO2 static detection
    public void recordMovement(int entityId, long currentTick) {
        FurnitureEntry e = entries.get(entityId);
        if (e != null) e.lastMoveTick = currentTick;
    }

    public boolean isStatic(int entityId, long currentTick) {
        FurnitureEntry e = entries.get(entityId);
        if (e == null || e.lastMoveTick < 0) return false;
        return (currentTick - e.lastMoveTick) >= STATIC_THRESHOLD;
    }

    public void upgradeSource(int entityId, Source newSource) {
        FurnitureEntry e = entries.get(entityId);
        if (e != null && e.isConfirmed()) {
            e.source = newSource;
            markSource(newSource);
        }
    }

    public static Source inferSourcePublic(String namespace) { return inferSource(namespace); }
}
