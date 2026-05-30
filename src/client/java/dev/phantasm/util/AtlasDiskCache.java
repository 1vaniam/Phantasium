package dev.phantasm.util;

import dev.phantasm.PhantasmClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/*
 TX4 - Atlas disk cache
 */
public final class AtlasDiskCache {

    static final Path CACHE_ROOT =
        FabricLoader.getInstance().getConfigDir().resolve("phantasm_atlas_cache");

    static final int MAX_CACHED_ATLASES = 3;

    private static volatile boolean cacheHit = false;
    private static volatile String activeFingerprint = null;

    private AtlasDiskCache() {}

    /**
     * Checks whether a valid atlas cache exists for the given fingerprint
     * Called from PackHashCache after a fingerprint + file match
     */
    public static boolean checkCache(String fingerprint) {
        Path atlasDir = CACHE_ROOT.resolve(fingerprint);
        Path sentinel = atlasDir.resolve("layout.properties");

        if (Files.exists(sentinel)) {
            cacheHit = true;
            activeFingerprint = fingerprint;
            PhantasmClient.LOGGER.info(
                "[Phantasm] TX4 atlas disk cache hit for fingerprint {}",
                fingerprint.substring(0, 8));
            return true;
        }

        cacheHit = false;
        activeFingerprint = null;
        return false;
    }

    public static boolean isCacheHit() { return cacheHit; }
    public static String getActiveFingerprint() { return activeFingerprint; }

    public static void reset() {
        cacheHit = false;
        activeFingerprint = null;
    }

    public static void invalidate() {
        if (activeFingerprint != null) {
            deleteDir(CACHE_ROOT.resolve(activeFingerprint));
            PhantasmClient.LOGGER.info("[Phantasm] TX4 atlas cache invalidated");
        }
        reset();
    }

    /** Exposed package private for AtlasSaveQueue */
    static void pruneOldEntries() {
        try {
            if (!Files.exists(CACHE_ROOT)) return;
            try (var stream = Files.list(CACHE_ROOT)) {
                var entries = stream
                    .filter(Files::isDirectory)
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b)
                                        .compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) { return 0; }
                    })
                    .toList();
                for (int i = MAX_CACHED_ATLASES; i < entries.size(); i++) {
                    deleteDir(entries.get(i));
                    PhantasmClient.LOGGER.debug(
                        "[Phantasm] TX4 pruned old cache: {}", entries.get(i).getFileName());
                }
            }
        } catch (IOException e) {
            PhantasmClient.LOGGER.debug("[Phantasm] TX4 prune error: {}", e.getMessage());
        }
    }

    private static void deleteDir(Path dir) {
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        } catch (IOException e) {
            PhantasmClient.LOGGER.debug("[Phantasm] TX4 delete error: {}", e.getMessage());
        }
    }
}
