package dev.phantasm.mixin;

/*
 TX6 Parallel apply-executor
*/

import net.minecraft.client.texture.SpriteAtlasTexture;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpriteAtlasTexture.class)
public abstract class MixinAtlasReloadParallel {
    // Intentionally empty, see MixinMipmapSkip for TX6-A/TX6-B logic
}
