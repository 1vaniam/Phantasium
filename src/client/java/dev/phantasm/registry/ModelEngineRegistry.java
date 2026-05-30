package dev.phantasm.registry;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.concurrent.ConcurrentHashMap;

/*ME1- Bone group registry built from SetPassengers packets.*/
public final class ModelEngineRegistry {

    private static final ModelEngineRegistry INSTANCE = new ModelEngineRegistry();

    private volatile Int2IntOpenHashMap boneToRoot = emptyBoneToRoot();

    private final ConcurrentHashMap<Integer, IntOpenHashSet> rootToBones = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, Boolean> invisibleBones = new ConcurrentHashMap<>();

    private final java.util.ArrayDeque<int[]> pendingGroups = new java.util.ArrayDeque<>();

    private ModelEngineRegistry() {}

    private static Int2IntOpenHashMap emptyBoneToRoot() {
        Int2IntOpenHashMap m = new Int2IntOpenHashMap();
        m.defaultReturnValue(-1);
        return m;
    }

    public static ModelEngineRegistry get() { return INSTANCE; }

    public synchronized void registerGroup(int rootId, int[] passengerIds) {
        int[] entry = new int[passengerIds.length + 1];
        entry[0] = rootId;
        System.arraycopy(passengerIds, 0, entry, 1, passengerIds.length);
        pendingGroups.add(entry);
    }

    public synchronized void flushPending() {
        if (pendingGroups.isEmpty()) return;
        Int2IntOpenHashMap next = new Int2IntOpenHashMap(boneToRoot);
        while (!pendingGroups.isEmpty()) {
            int[] entry = pendingGroups.poll();
            int rootId = entry[0];
            IntOpenHashSet bones = rootToBones.computeIfAbsent(rootId, k -> new IntOpenHashSet());
            for (int i = 1; i < entry.length; i++) {
                next.put(entry[i], rootId);
                bones.add(entry[i]);
            }
        }
        boneToRoot = next;
    }

    public synchronized void removeEntity(int entityId) {
        Int2IntOpenHashMap next = new Int2IntOpenHashMap(boneToRoot);
        IntOpenHashSet bones = rootToBones.remove(entityId);
        if (bones != null) {
            for (int boneId : bones) {
                next.remove(boneId);
                invisibleBones.remove(boneId);
            }
        }
        next.remove(entityId);
        invisibleBones.remove(entityId);
        boneToRoot = next;
    }

    public synchronized void clear() {
        boneToRoot = emptyBoneToRoot();
        rootToBones.clear();
        invisibleBones.clear();
    }

    public boolean isBone(int entityId)  { return boneToRoot.containsKey(entityId); }
    public int getRootId(int boneId)     { return boneToRoot.get(boneId); }
    public int getBoneCount()            { return boneToRoot.size(); }

    public void markInvisible(int boneId)  { invisibleBones.put(boneId, Boolean.TRUE); }
    public void markVisible(int boneId)    { invisibleBones.remove(boneId); }
    public boolean isServerCulled(int id)  { return invisibleBones.containsKey(id); }
    public int getServerCulledCount()      { return invisibleBones.size(); }
}
