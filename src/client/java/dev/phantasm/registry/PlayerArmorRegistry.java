package dev.phantasm.registry;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public final class PlayerArmorRegistry {

    private static final PlayerArmorRegistry INSTANCE = new PlayerArmorRegistry();

    /*passengerEntityId = player entity ID */
    private volatile Int2IntOpenHashMap armorToPlayer = emptyMap();

    /*playerEntityId = set of armor entity IDs riding it */
    private final java.util.concurrent.ConcurrentHashMap<Integer, IntOpenHashSet> playerToArmor =
            new java.util.concurrent.ConcurrentHashMap<>();

    private PlayerArmorRegistry() {}

    public static PlayerArmorRegistry get() { return INSTANCE; }

    private static Int2IntOpenHashMap emptyMap() {
        Int2IntOpenHashMap m = new Int2IntOpenHashMap();
        m.defaultReturnValue(-1);
        return m;
    }


    /*Called when a player entity has armor-stand or item-display passengers set.*/
    public synchronized void registerPlayerArmor(int playerId, int[] passengerIds) {
        Int2IntOpenHashMap next = new Int2IntOpenHashMap(armorToPlayer);
        IntOpenHashSet armor = playerToArmor.computeIfAbsent(playerId, k -> new IntOpenHashSet());
        for (int pid : passengerIds) {
            next.put(pid, playerId);
            armor.add(pid);
        }
        armorToPlayer = next;
    }

    public synchronized void removePlayer(int playerId) {
        IntOpenHashSet armor = playerToArmor.remove(playerId);
        if (armor == null) return;
        Int2IntOpenHashMap next = new Int2IntOpenHashMap(armorToPlayer);
        for (int armorId : armor) next.remove(armorId);
        armorToPlayer = next;
    }

    public synchronized void removeEntity(int entityId) {
        Int2IntOpenHashMap snapshot = armorToPlayer;
        if (!snapshot.containsKey(entityId)) return;
        int playerId = snapshot.get(entityId);
        Int2IntOpenHashMap next = new Int2IntOpenHashMap(snapshot);
        next.remove(entityId);
        armorToPlayer = next;
        IntOpenHashSet set = playerToArmor.get(playerId);
        if (set != null) set.remove(entityId);
    }

    public synchronized void clear() {
        armorToPlayer = emptyMap();
        playerToArmor.clear();
    }


    /*True if this entity is a custom-armor stand/display riding a player*/
    public boolean isPlayerArmor(int entityId) {
        return armorToPlayer.containsKey(entityId);
    }

    /*Returns the player entity ID this armor rides, or -1 if not registered*/
    public int getPlayerId(int entityId) {
        return armorToPlayer.get(entityId);
    }

    public int getCount() { return armorToPlayer.size(); }
}
