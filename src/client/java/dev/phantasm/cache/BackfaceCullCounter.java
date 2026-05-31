package dev.phantasm.cache;

import java.util.concurrent.atomic.AtomicInteger;

public final class BackfaceCullCounter {
    private static final AtomicInteger COUNT = new AtomicInteger();
    private BackfaceCullCounter() {}
    public static void increment() { COUNT.incrementAndGet(); }
    public static int getAndReset() { return COUNT.getAndSet(0); }
}
