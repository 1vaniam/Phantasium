package dev.phantasm.mixin;

import dev.phantasm.util.PuaCodepointRegistry;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
 IO4 PUA codepoint cache warmup
 NX4 Skip accept() for zero-width PUA shift codepoints
*/
@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
public abstract class MixinTextRenderer {

    @Inject(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At("HEAD"), cancellable = true)
    private void phantasm_accept(int index, Style style, int codepoint,
                                  CallbackInfoReturnable<Boolean> cir) {

        if (codepoint >= PuaCodepointRegistry.PUA_START && codepoint <= PuaCodepointRegistry.PUA_END) {
            // IO4: warm up PUA set
            PuaCodepointRegistry.markKnown(codepoint);

            // NX4: skip zero-width shift characters entirely
            if (PuaCodepointRegistry.isZeroWidth(codepoint)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
