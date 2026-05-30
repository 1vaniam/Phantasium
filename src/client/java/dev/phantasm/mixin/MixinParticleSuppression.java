package dev.phantasm.mixin;

import dev.phantasm.cache.EntityPositionCache;
import dev.phantasm.config.PhantasmConfig;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 Drop paricle packets whose XYZ originates from a bone or furniture entity's position
 Tolerance = 0.5 blocks.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinParticleSuppression {

    private static final double PROXIMITY_SQ = 0.5 * 0.5;

    @Inject(method = "onParticle", at = @At("HEAD"), cancellable = true)
    private void phantasm_suppressBoneParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!PhantasmConfig.get().enableParticleSuppression) return;
        if (EntityPositionCache.get().anyWithin(packet.getX(), packet.getY(), packet.getZ(), PROXIMITY_SQ)) {
            ci.cancel();
        }
    }
}
