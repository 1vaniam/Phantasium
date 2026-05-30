package dev.phantasm.mixin;

import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.util.PackHashCache;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 TX3 -  Server resource pack skip cache.

 Intercepts the ResourcePackSend packet to record the pack's UUID + URL
 fingerprint
 */
@Mixin(ClientCommonNetworkHandler.class)
public abstract class MixinResourcePackApply {

    @Inject(method = "onResourcePackSend", at = @At("HEAD"))
    private void phantasm_checkPackHash(ResourcePackSendS2CPacket packet, CallbackInfo ci) {
        if (!PhantasmConfig.get().enablePackHashSkip) return;
        PackHashCache.checkAndUpdate(packet.id(), packet.url(), packet.hash());
    }
}
