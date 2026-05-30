package dev.phantasm.mixin;

import dev.phantasm.registry.ServerPluginDetector;
import dev.phantasm.util.PuaCodepointRegistry;
import net.minecraft.client.font.Glyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 NX4 support - detects zero-width PUA glyphs from BitmapFont and registers
 them in PuaCodepointRegistry so MixinTextRenderer can skip them
 */
@Mixin(targets = "net.minecraft.client.font.BitmapFont")
public abstract class MixinBitmapFont {

    @Inject(method = "getGlyph(I)Lnet/minecraft/client/font/Glyph;",
            at = @At("RETURN"))
    private void phantasm_getGlyph(int codepoint, CallbackInfoReturnable<Glyph> cir) {
        if (!ServerPluginDetector.get().hasNexo()) return;
        if (codepoint < PuaCodepointRegistry.PUA_START || codepoint > PuaCodepointRegistry.PUA_END) return;

        Glyph glyph = cir.getReturnValue();
        if (glyph != null && glyph.getMetrics().getAdvance() == 0f) {
            PuaCodepointRegistry.registerZeroWidth(codepoint);
        }
    }
}
