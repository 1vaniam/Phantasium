package dev.phantasm.mixin;

import dev.phantasm.registry.FurnitureRegistry;
import dev.phantasm.registry.ModelEngineRegistry;
import net.minecraft.entity.decoration.ArmorStandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/*
S4 -  Skip collision for ME bones and IA/Oraxen furniture ArmorStands.
DisplayEntity already returns false for isPushable, so we only need ArmorStand
 */
@Mixin(ArmorStandEntity.class)
public abstract class MixinIsPushable {

    @Inject(method = "isPushable()Z", at = @At("HEAD"), cancellable = true)
    private void phantasm_isPushable(CallbackInfoReturnable<Boolean> cir) {
        int id = ((net.minecraft.entity.Entity) (Object) this).getId();
        if (ModelEngineRegistry.get().isBone(id) || FurnitureRegistry.get().isFurniture(id)) {
            cir.setReturnValue(false);
        }
    }
}
