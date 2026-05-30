package dev.phantasm.cache;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.BlockStateModel;

import java.util.concurrent.ConcurrentHashMap;

/*NX2 - Block state → BlockStateModel cache for Nexo's hijacked block types*/
public final class NexoBlockModelCache {

    private static final NexoBlockModelCache INSTANCE = new NexoBlockModelCache();

    /* Concurrent so render thread + chunk mesh threads can both read safely */
    private final ConcurrentHashMap<BlockState, BlockStateModel> cache = new ConcurrentHashMap<>();

    private NexoBlockModelCache() {}

    public static NexoBlockModelCache get() { return INSTANCE; }

    /* Returns a cached model, or null if not yet cached*/
    public BlockStateModel lookup(BlockState state) {
        if (!isCandidateBlock(state)) return null;
        return cache.get(state);
    }

    /* Store a resolved model in the cache */
    public void store(BlockState state, BlockStateModel model) {
        if (isCandidateBlock(state)) {
            cache.putIfAbsent(state, model);
        }
    }

    /*Only cache the three block types Nexo hijacks */
    public static boolean isCandidateBlock(BlockState state) {
        return state.isOf(Blocks.NOTE_BLOCK)
            || state.isOf(Blocks.TRIPWIRE)
            || state.isOf(Blocks.CHORUS_FLOWER);
    }

    /* Called on resource pack reload. models are rebaked so cache is stale */
    public void invalidate() {
        cache.clear();
    }

    public int size() { return cache.size(); }
}
