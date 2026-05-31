package dev.phantasm.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.phantasm.PhantasmClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/*simple JSON config saved to config/phantasm.json*/
public final class PhantasmConfig {

    private static final PhantasmConfig INSTANCE = new PhantasmConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "phantasm.json";

    public volatile boolean enableFrustumCulling      = true;
    public volatile boolean enableOcclusionCulling    = true;
    public volatile boolean enablePacketDrops         = true;
    public volatile boolean enableMipmapCap           = true;
    public volatile boolean enableLabelSkip           = true;
    public volatile boolean enableShadowSkip          = true;
    public volatile boolean enableHitboxSkip          = true;
    public volatile boolean enableLodThrottling       = true;
    public volatile boolean enableF3Overlay           = true;
    public volatile boolean enableParticleSuppression = true;
    public volatile boolean enableParallelSpriteLoad  = true;
    public volatile boolean enablePackHashSkip        = true;
    public volatile boolean enableAtlasDiskCache      = true;

    public volatile boolean enableBackFaceCulling     = true;

    //Cull distance
    public volatile double closeDistanceBlocks = 16.0;

    //LOD settings
    public volatile double lodDistanceBlocks       = 32.0;
    public volatile int    lodSkipTicks            = 3;
    public volatile double lodRenderDistanceBlocks = 96.0;

    private PhantasmConfig() {}

    public static PhantasmConfig get() { return INSTANCE; }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        if (Files.exists(path)) {
            try {
                String json = Files.readString(path);
                PhantasmConfig loaded = GSON.fromJson(json, PhantasmConfig.class);
                if (loaded != null) copyFrom(loaded, INSTANCE);
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                PhantasmClient.LOGGER.warn("[Phantasm] Failed to load config, using defaults: {}", e.getMessage());
            }
        }
        // Clamp to valid preset values
        INSTANCE.closeDistanceBlocks = nearestPreset(INSTANCE.closeDistanceBlocks);
        save();
    }

    public static void save() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            Files.writeString(path, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            PhantasmClient.LOGGER.warn("[Phantasm] Failed to save config: {}", e.getMessage());
        }
    }

    /*Snaps an arbitrary value to the nearest valid preset*/
    private static double nearestPreset(double value) {
        double[] presets = { 8, 16, 32, 64, 128 };
        double best = presets[0];
        double bestDist = Math.abs(value - best);
        for (double p : presets) {
            double d = Math.abs(value - p);
            if (d < bestDist) { best = p; bestDist = d; }
        }
        return best;
    }

    private static void copyFrom(PhantasmConfig src, PhantasmConfig dst) {
        dst.enableFrustumCulling      = src.enableFrustumCulling;
        dst.enableOcclusionCulling    = src.enableOcclusionCulling;
        dst.enablePacketDrops         = src.enablePacketDrops;
        dst.enableMipmapCap           = src.enableMipmapCap;
        dst.enableLabelSkip           = src.enableLabelSkip;
        dst.enableShadowSkip          = src.enableShadowSkip;
        dst.enableHitboxSkip          = src.enableHitboxSkip;
        dst.enableLodThrottling       = src.enableLodThrottling;
        dst.enableF3Overlay           = src.enableF3Overlay;
        dst.enableParticleSuppression = src.enableParticleSuppression;
        dst.enableParallelSpriteLoad  = src.enableParallelSpriteLoad;
        dst.enablePackHashSkip        = src.enablePackHashSkip;
        dst.enableAtlasDiskCache      = src.enableAtlasDiskCache;
        dst.enableBackFaceCulling     = src.enableBackFaceCulling;
        dst.closeDistanceBlocks       = src.closeDistanceBlocks;
        dst.lodDistanceBlocks         = src.lodDistanceBlocks;
        dst.lodSkipTicks              = src.lodSkipTicks;
        dst.lodRenderDistanceBlocks   = src.lodRenderDistanceBlocks;
    }
}
