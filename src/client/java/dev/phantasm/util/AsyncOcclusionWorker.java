package dev.phantasm.util;

import dev.phantasm.cache.OcclusionCullCache;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/* S3 - Async occlusion cull worker */
public final class AsyncOcclusionWorker {

    private static final AsyncOcclusionWorker INSTANCE = new AsyncOcclusionWorker();

    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "phantasm-occlusion");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });


    private final IntOpenHashSet pendingThisTick = new IntOpenHashSet();
    private final ReentrantLock pendingLock = new ReentrantLock();

    private AsyncOcclusionWorker() {}

    public static AsyncOcclusionWorker get() { return INSTANCE; }

    /*
     Submit an occlusion check
    */
    public void submit(int entityId, Vec3d boxCenter, Vec3d cameraPos, World world) {
        pendingLock.lock();
        try {
            if (!pendingThisTick.add(entityId)) return; // already queued this tick
        } finally {
            pendingLock.unlock();
        }
        executor.execute(() -> checkEntity(entityId, boxCenter, cameraPos, world));
    }

    public void clearPending() {
        pendingLock.lock();
        try { pendingThisTick.clear(); }
        finally { pendingLock.unlock(); }
    }

    private void checkEntity(int entityId, Vec3d boxCenter, Vec3d cameraPos, World world) {
        RaycastContext ctx = new RaycastContext(
            cameraPos, boxCenter,
            RaycastContext.ShapeType.VISUAL,
            RaycastContext.FluidHandling.NONE,
            net.minecraft.block.ShapeContext.absent()
        );
        HitResult hit = world.raycast(ctx);
        OcclusionCullCache cache = OcclusionCullCache.get();
        if (hit.getType() == HitResult.Type.MISS) cache.markVisible(entityId);
        else                                       cache.markOccluded(entityId);
    }

    public void shutdown() {
        executor.shutdown();
        try { executor.awaitTermination(2, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
