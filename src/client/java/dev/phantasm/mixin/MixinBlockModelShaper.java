package dev.phantasm.mixin;

import dev.phantasm.cache.NexoBlockModelCache;
import dev.phantasm.registry.ServerPluginDetector;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
NX2 — Block state model lookup cache for Nexo's hijacked blocks.
 */
@Mixin(BlockRenderManager.class)
public abstract class MixinBlockModelShaper {

    @Inject(method = "getModel(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/model/BlockStateModel;",
            at = @At("HEAD"), cancellable = true)
    private void phantasm_getModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        if (!ServerPluginDetector.get().hasNexo()) return;
        BlockStateModel cached = NexoBlockModelCache.get().lookup(state);
        if (cached != null) cir.setReturnValue(cached);
    }

    @Inject(method = "getModel(Lnet/minecraft/block/BlockState;)Lnet/minecraft/client/render/model/BlockStateModel;",
            at = @At("RETURN"))
    private void phantasm_cacheModel(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        if (!ServerPluginDetector.get().hasNexo()) return;
        BlockStateModel result = cir.getReturnValue();
        if (result != null) NexoBlockModelCache.get().store(state, result);
    }
}
