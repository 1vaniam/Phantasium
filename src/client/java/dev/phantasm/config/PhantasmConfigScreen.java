package dev.phantasm.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class PhantasmConfigScreen {

    private PhantasmConfigScreen() {}

    public static Screen build(Screen parent) {
        PhantasmConfig cfg = PhantasmConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Phantasium Config"))
                .setSavingRunnable(PhantasmConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        //Culling
        ConfigCategory culling = builder.getOrCreateCategory(Text.literal("Culling"));

        culling.addEntry(eb.startBooleanToggle(Text.literal("Frustum Culling"), cfg.enableFrustumCulling)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Skip rendering bones/furniture outside the camera frustum."))
                .setSaveConsumer(v -> cfg.enableFrustumCulling = v)
                .build());

        culling.addEntry(eb.startBooleanToggle(Text.literal("Occlusion Culling"), cfg.enableOcclusionCulling)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Skip rendering entities hidden behind solid blocks."))
                .setSaveConsumer(v -> cfg.enableOcclusionCulling = v)
                .build());

        culling.addEntry(eb.startBooleanToggle(Text.literal("Back-Face Culling"), cfg.enableBackFaceCulling)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Skip rendering Model Engine bones facing away from the camera."))
                .setSaveConsumer(v -> cfg.enableBackFaceCulling = v)
                .build());

        culling.addEntry(eb.startIntSlider(Text.literal("Close Distance (blocks)"), (int) cfg.closeDistanceBlocks, 8, 128)
                .setDefaultValue(16)
                .setTooltip(Text.literal("Entities closer than this skip back-face and occlusion culling.\n8=Low  16=Medium  32=High  64=Ultra  128=Off"))
                .setSaveConsumer(v -> cfg.closeDistanceBlocks = v)
                .build());

        //Packets
        ConfigCategory packets = builder.getOrCreateCategory(Text.literal("Packets"));

        packets.addEntry(eb.startBooleanToggle(Text.literal("Packet Drops"), cfg.enablePacketDrops)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Drop redundant position/velocity packets for tracked entities."))
                .setSaveConsumer(v -> cfg.enablePacketDrops = v)
                .build());

        packets.addEntry(eb.startBooleanToggle(Text.literal("Particle Suppression"), cfg.enableParticleSuppression)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Suppress particle packets originating from bone/furniture positions."))
                .setSaveConsumer(v -> cfg.enableParticleSuppression = v)
                .build());

        //LOD
        ConfigCategory lod = builder.getOrCreateCategory(Text.literal("LOD"));

        lod.addEntry(eb.startBooleanToggle(Text.literal("LOD Throttling"), cfg.enableLodThrottling)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Throttle render updates for distant entities."))
                .setSaveConsumer(v -> cfg.enableLodThrottling = v)
                .build());

        lod.addEntry(eb.startIntSlider(Text.literal("LOD Distance (blocks)"), (int) cfg.lodDistanceBlocks, 8, 128)
                .setDefaultValue(32)
                .setTooltip(Text.literal("Entities beyond this distance are LOD-throttled."))
                .setSaveConsumer(v -> cfg.lodDistanceBlocks = v)
                .build());

        lod.addEntry(eb.startIntSlider(Text.literal("LOD Skip Ticks"), cfg.lodSkipTicks, 1, 20)
                .setDefaultValue(3)
                .setTooltip(Text.literal("How many ticks to skip between updates for LOD entities."))
                .setSaveConsumer(v -> cfg.lodSkipTicks = v)
                .build());

        lod.addEntry(eb.startIntSlider(Text.literal("LOD Render Distance (blocks)"), (int) cfg.lodRenderDistanceBlocks, 32, 256)
                .setDefaultValue(96)
                .setTooltip(Text.literal("Entities beyond this distance are not rendered at all."))
                .setSaveConsumer(v -> cfg.lodRenderDistanceBlocks = v)
                .build());

        //Rendering
        ConfigCategory rendering = builder.getOrCreateCategory(Text.literal("Rendering"));

        rendering.addEntry(eb.startBooleanToggle(Text.literal("Label Skip"), cfg.enableLabelSkip)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Skip name-tag rendering for bone entities."))
                .setSaveConsumer(v -> cfg.enableLabelSkip = v)
                .build());

        rendering.addEntry(eb.startBooleanToggle(Text.literal("Shadow Skip"), cfg.enableShadowSkip)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Skip shadow rendering for bone entities."))
                .setSaveConsumer(v -> cfg.enableShadowSkip = v)
                .build());

        rendering.addEntry(eb.startBooleanToggle(Text.literal("Hitbox Skip"), cfg.enableHitboxSkip)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Skip hitbox rendering for bone entities in F3+B mode."))
                .setSaveConsumer(v -> cfg.enableHitboxSkip = v)
                .build());

        //Textures
        ConfigCategory textures = builder.getOrCreateCategory(Text.literal("Textures"));

        textures.addEntry(eb.startBooleanToggle(Text.literal("Mipmap Cap"), cfg.enableMipmapCap)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Cap mipmap levels on atlas sprites to reduce VRAM usage."))
                .setSaveConsumer(v -> cfg.enableMipmapCap = v)
                .build());

        textures.addEntry(eb.startBooleanToggle(Text.literal("Parallel Sprite Load"), cfg.enableParallelSpriteLoad)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Load atlas sprites in parallel to speed up resource pack loading."))
                .setSaveConsumer(v -> cfg.enableParallelSpriteLoad = v)
                .build());

        textures.addEntry(eb.startBooleanToggle(Text.literal("Pack Hash Skip"), cfg.enablePackHashSkip)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Skip redundant resource pack hash checks."))
                .setSaveConsumer(v -> cfg.enablePackHashSkip = v)
                .build());

        textures.addEntry(eb.startBooleanToggle(Text.literal("Atlas Disk Cache"), cfg.enableAtlasDiskCache)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Cache stitched atlas to disk to speed up subsequent loads."))
                .setSaveConsumer(v -> cfg.enableAtlasDiskCache = v)
                .build());

        //Misc
        ConfigCategory misc = builder.getOrCreateCategory(Text.literal("Misc"));

        misc.addEntry(eb.startBooleanToggle(Text.literal("F3 Overlay"), cfg.enableF3Overlay)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Show Phantasium stats in the F3 debug screen."))
                .setSaveConsumer(v -> cfg.enableF3Overlay = v)
                .build());

        return builder.build();
    }
}
