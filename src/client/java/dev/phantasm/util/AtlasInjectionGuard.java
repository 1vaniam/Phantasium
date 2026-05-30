package dev.phantasm.util;

import java.util.concurrent.atomic.AtomicBoolean;


public final class AtlasInjectionGuard {
    private AtlasInjectionGuard() {}

    public static final AtomicBoolean INJECTION_FIRED = new AtomicBoolean(false);
}
