package dev.phantasm.mixin;

import dev.phantasm.cache.DisplayTransformCache;
import dev.phantasm.registry.FurnitureRegistry;
import dev.phantasm.registry.PlayerArmorRegistry;
import net.minecraft.client.render.entity.state.DisplayEntityRenderState;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(targets = "net.minecraft.client.render.entity.DisplayEntityRenderer")
public abstract class MixinItemDisplayTransform {

    @Inject(
        method = "updateRenderState(Lnet/minecraft/entity/decoration/DisplayEntity;Lnet/minecraft/client/render/entity/state/DisplayEntityRenderState;F)V",
        at = @At("RETURN")
    )
    private void phantasm_cacheDisplayRenderState(
            DisplayEntity entity,
            DisplayEntityRenderState renderState,
            float tickDelta,
            CallbackInfo ci) {

        int id = entity.getId();

        // Only cache for static furniture and player-armor, not ME bones
        if (!FurnitureRegistry.get().isFurniture(id)
                && !PlayerArmorRegistry.get().isPlayerArmor(id)) {
            return;
        }

        DisplayTransformCache cache = DisplayTransformCache.get();
        DisplayEntity.RenderState cached = cache.get(id);

        if (cached != null) {
            // Cache hit = restore the cached RenderState directly
            // RenderState is an immutable record so this is always safe
            renderState.displayRenderState = cached;
        } else if (renderState.displayRenderState != null) {
            // Cache miss = store the freshly populated RenderState for next frame
            cache.put(id, renderState.displayRenderState);
        }
    }
}
