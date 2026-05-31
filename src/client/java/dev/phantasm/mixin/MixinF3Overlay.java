package dev.phantasm.mixin;

import dev.phantasm.config.PhantasmConfig;
import dev.phantasm.registry.FurnitureRegistry;
import dev.phantasm.registry.ModelEngineRegistry;
import dev.phantasm.registry.ServerPluginDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/*
F3 diagnostic overlay
*/
@Mixin(DebugHud.class)
public abstract class MixinF3Overlay {

    private static final String MARKER = "[Phantasium]";

    @Inject(method = "drawText", at = @At("HEAD"))
    private void phantasm_appendF3Stats(DrawContext context, List<String> text, boolean left,
                                         CallbackInfo ci) {
        if (left) return;
        if (!PhantasmConfig.get().enableF3Overlay) return;
        if (!MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud()) return;

        for (String line : text) {
            if (line != null && line.startsWith(MARKER)) return;
        }

        ServerPluginDetector det = ServerPluginDetector.get();
        ModelEngineRegistry me  = ModelEngineRegistry.get();
        FurnitureRegistry fur   = FurnitureRegistry.get();

        boolean hasME = det.isModelEngineLatched();
        boolean hasIA = det.isItemsAdderLatched();
        boolean hasOX = det.isOraxenLatched();
        boolean hasNX = det.isNexoLatched();

        StringBuilder plugins = new StringBuilder(MARKER + " plugins:");
        if (hasME) plugins.append(" ME");
        if (hasIA) plugins.append(" IA");
        if (hasOX) plugins.append(" OX");
        if (hasNX) plugins.append(" NX");
        if (!hasME && !hasIA && !hasOX && !hasNX) plugins.append(" none");

        text.add("");
        text.add(plugins.toString());
        int furnitureCulled = fur.getAndResetCulled();
        int totalCulled = me.getServerCulledCount() + furnitureCulled;
        int backfaceCulled = dev.phantasm.cache.BackfaceCullCounter.getAndReset();
        text.add(MARKER + " bones: " + me.getBoneCount()
                 + "  culled: " + totalCulled
                 + "  backface: " + backfaceCulled);
        text.add(MARKER + " furniture: " + fur.getFurnitureCount());
        text.add(MARKER + " close-dist: " + (int) dev.phantasm.config.PhantasmConfig.get().closeDistanceBlocks + "b");
    }
}
