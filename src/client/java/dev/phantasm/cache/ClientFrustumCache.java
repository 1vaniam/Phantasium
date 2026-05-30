package dev.phantasm.cache;

import net.minecraft.client.render.Frustum;

/*
 S1 - Per-frame frustum snapshot.
 Populated once per frame in WorldRenderEvents.START, consumed by S2 (shouldRender mixin)
 */
public final class ClientFrustumCache {

    private static final ClientFrustumCache INSTANCE = new ClientFrustumCache();

    private volatile Frustum frustum;

    private ClientFrustumCache() {}

    public static ClientFrustumCache get() {
        return INSTANCE;
    }

    /*Called once per frame from the WorldRenderEvents.START listener*/
    public void update(Frustum frustum) {
        this.frustum = frustum;
    }

    /*
     * Returns true if the given axis-aligned bounding box is inside (or touching)
     * the cached frustum. Always returns true if no frustum has been captured yet
     */
    public boolean isVisible(net.minecraft.util.math.Box box) {
        Frustum f = this.frustum;
        if (f == null) return true;
        return f.isVisible(box);
    }
}
