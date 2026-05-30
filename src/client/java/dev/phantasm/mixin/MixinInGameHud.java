package dev.phantasm.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 O3 - ActionBar glyph deduplication.
 Caches the last overlay message string and skips redundant updates
 */
@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

    private String phantasm_lastOverlayString = null;

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void phantasm_setOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        if (message == null) {
            phantasm_lastOverlayString = null;
            return;
        }

        // Use the plain string content for comparison - fast and allocation-light.
        // For PUA glyph checks this is sufficient since the character sequence is what changes.
        String str = message.getString();
        if (str.equals(phantasm_lastOverlayString)) {
            ci.cancel();
            return;
        }
        phantasm_lastOverlayString = str;
    }
}
