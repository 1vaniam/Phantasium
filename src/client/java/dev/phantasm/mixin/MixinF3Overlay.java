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
 * F3 diagnostic overlay.
 * Guards against duplicate appends (VulkanMod calls drawText twice per frame)
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

        // Guard don't append if already present (VulkanMod double-calls drawText)
        for (String line : text) {
            if (line != null && line.startsWith(MARKER)) return;
        }

        ServerPluginDetector det = ServerPluginDetector.get();
        ModelEngineRegistry me  = ModelEngineRegistry.get();
        FurnitureRegistry fur   = FurnitureRegistry.get();

        // Use raw latched flags - the has*() methods have entity-detection side effects
        // and also read non-thread-safe registry state (getBoneCount on a fastutil map)
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
        text.add(MARKER + " bones: " + me.getBoneCount()
                 + "  culled: " + totalCulled);
        text.add(MARKER + " furniture: " + fur.getFurnitureCount());
        text.add(MARKER + " close-dist: " + (int) dev.phantasm.config.PhantasmConfig.get().closeDistanceBlocks + "b");
    }
}
