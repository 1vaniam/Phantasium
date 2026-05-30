package dev.phantasm.mixin;

import dev.phantasm.PhantasmClient;
import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.registry.ServerPluginDetector;
import dev.phantasm.util.AtlasInjectionGuard;
import dev.phantasm.util.AtlasSaveQueue;
import dev.phantasm.util.PackHashCache;
import net.minecraft.client.texture.AtlasManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/*
 TX1 = Mipmap cap
 TX2 = Parallel prepareExecutor
 *X3 = Skip when pack fingerprint unchanged
 TX6-A = Parallel applyExecutor
 TX6-B = Queue atlas sentinel write after full reload
 */
@Mixin(AtlasManager.class)
public abstract class MixinMipmapSkip {

    @Shadow private int mipmapLevels;

    private static final ForkJoinPool SPRITE_LOAD_POOL = new ForkJoinPool(
        Math.max(2, Runtime.getRuntime().availableProcessors())
    );

    @Inject(
        method = "reload(Lnet/minecraft/resource/ResourceReloader$Store;Ljava/util/concurrent/Executor;Lnet/minecraft/resource/ResourceReloader$Synchronizer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("HEAD")
    )
    private void phantasm_onReloadHead(
            net.minecraft.resource.ResourceReloader.Store store,
            Executor prepareExecutor,
            net.minecraft.resource.ResourceReloader.Synchronizer synchronizer,
            Executor applyExecutor,
            CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        AtlasInjectionGuard.INJECTION_FIRED.set(true);

        if (PhantasmConfig.get().enableMipmapCap
                && !PackHashCache.isCurrentPackUnchanged()
                && ServerPluginDetector.get().hasAnyPlugin()
                && this.mipmapLevels > 1) {
            PhantasmClient.LOGGER.debug("[Phantasm] TX1 mipmap cap: {} → 1", this.mipmapLevels);
            this.mipmapLevels = 1;
        }
    }

    /*
     TX2 - Replace prepareExecutor (index 2) with burst pool
     */
    @ModifyVariable(
        method = "reload(Lnet/minecraft/resource/ResourceReloader$Store;Ljava/util/concurrent/Executor;Lnet/minecraft/resource/ResourceReloader$Synchronizer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("HEAD"),
        argsOnly = true,
        index = 2
    )
    private Executor phantasm_replaceExecutor(Executor prepareExecutor) {
        if (!PhantasmConfig.get().enableParallelSpriteLoad) return prepareExecutor;
        if (PackHashCache.isCurrentPackUnchanged())         return prepareExecutor;
        if (!ServerPluginDetector.get().hasAnyPlugin())     return prepareExecutor;
        PhantasmClient.LOGGER.debug(
            "[Phantasm] TX2 burst pool ({} threads) applied for atlas reload",
            SPRITE_LOAD_POOL.getParallelism());
        return SPRITE_LOAD_POOL;
    }

    /*
     TX6-A - Replace applyExecutor (index 4) with burst pool
     */
    @ModifyVariable(
        method = "reload(Lnet/minecraft/resource/ResourceReloader$Store;Ljava/util/concurrent/Executor;Lnet/minecraft/resource/ResourceReloader$Synchronizer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("HEAD"),
        argsOnly = true,
        index = 4
    )
    private Executor phantasm_replaceApplyExecutor(Executor applyExecutor) {
        if (!PhantasmConfig.get().enableParallelSpriteLoad) return applyExecutor;
        if (PackHashCache.isCurrentPackUnchanged())         return applyExecutor;
        if (!ServerPluginDetector.get().hasAnyPlugin())     return applyExecutor;
        PhantasmClient.LOGGER.debug(
            "[Phantasm] TX6-A apply burst pool ({} threads) applied",
            SPRITE_LOAD_POOL.getParallelism());
        return SPRITE_LOAD_POOL;
    }

    /*
     TX6-B - After full reload, write TX4 sentinel for next join cache hit
     */
    @Inject(
        method = "reload(Lnet/minecraft/resource/ResourceReloader$Store;Ljava/util/concurrent/Executor;Lnet/minecraft/resource/ResourceReloader$Synchronizer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("RETURN")
    )
    private void phantasm_queueAtlasSave(
            net.minecraft.resource.ResourceReloader.Store store,
            Executor prepareExecutor,
            net.minecraft.resource.ResourceReloader.Synchronizer synchronizer,
            Executor applyExecutor,
            CallbackInfoReturnable<CompletableFuture<Void>> cir) {

        if (!PhantasmConfig.get().enableAtlasDiskCache) return;
        if (PackHashCache.isCurrentPackUnchanged())     return;

        String fingerprint = PackHashCache.getLastWrittenFingerprint();
        if (fingerprint == null) return;

        CompletableFuture<Void> future = cir.getReturnValue();
        if (future == null) return;

        future.thenRunAsync(() ->
            AtlasSaveQueue.enqueue(fingerprint, null),
        Runnable::run);
    }
}
