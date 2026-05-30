package dev.phantasm.mixin;

import dev.phantasm.duck.EntityRenderStateEntityId;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class MixinEntityRenderState implements EntityRenderStateEntityId {

    @Unique
    private int phantasm_entityId = -1;

    @Override
    public int phantasm_getEntityId() { return phantasm_entityId; }

    @Override
    public void phantasm_setEntityId(int id) { phantasm_entityId = id; }
}
