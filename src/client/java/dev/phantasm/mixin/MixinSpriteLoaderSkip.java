package dev.phantasm.mixin;

import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.util.AtlasDiskCache;
import dev.phantasm.util.PackHashCache;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(SpriteLoader.class)
public abstract class MixinSpriteLoaderSkip {

    @Inject(
        method = "load(Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/Identifier;ILjava/util/concurrent/Executor;Ljava/util/Set;)Ljava/util/concurrent/CompletableFuture;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void phantasm_skipSpriteLoad(
            ResourceManager resourceManager,
            Identifier id,
            int mipLevel,
            Executor executor,
            Set<?> additionalMetadata,
            CallbackInfoReturnable<CompletableFuture<SpriteLoader.StitchResult>> cir) {
        // TX5 intentionally disabled — see class comment above.
    }
}
