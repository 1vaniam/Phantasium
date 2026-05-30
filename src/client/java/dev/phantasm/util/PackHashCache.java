package dev.phantasm.util;

import dev.phantasm.PhantasmClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * (TX3) Server resource pack skip cache.
 * File: config/phantasm_pack_cache.txt
 * Format: single line "<fingerprintHex>"
 */
public final class PackHashCache {

    private static final Path CACHE_FILE =
        FabricLoader.getInstance().getConfigDir().resolve("phantasm_pack_cache.txt");

    private static volatile boolean currentPackUnchanged = false;

    private static volatile String lastWrittenFingerprint = null;

    private PackHashCache() {}

    /*
     Call when the server sends a resource pack packet.
     Only marks unchanged=true if fingerprint matches AND the server provided
     */
    public static boolean checkAndUpdate(UUID packId, String url, String hash) {
        // If the server provides no hash, we can't safely skip — always reload
        if (hash == null || hash.isBlank()) {
            currentPackUnchanged = false;
            return false;
        }

        String fingerprint = buildFingerprint(packId, url, hash);
        String cached = readCached();

        if (fingerprint.equals(cached)) {

            Path downloadedPack = FabricLoader.getInstance()
                .getGameDir()
                .resolve("downloads")
                .resolve(packId.toString())
                .resolve(hash);
            if (!Files.exists(downloadedPack)) {
                PhantasmClient.LOGGER.info(
                    "[Phantasm] TX3 fingerprint matched but cached pack file is missing — forcing reload");
                currentPackUnchanged = false;
                return false;
            }

            currentPackUnchanged = true;

            // TX4, check if we also have a stitched atlas on disk for this fingerprint.
            AtlasDiskCache.checkCache(fingerprint);

            PhantasmClient.LOGGER.info(
                "[Phantasm] TX3 server pack unchanged — atlas restitch skipped");
            return true;
        }

        // Pack changed or first time, write new fingerprint and do full reload
        currentPackUnchanged = false;
        lastWrittenFingerprint = fingerprint;
        writeCached(fingerprint);
        return false;
    }

    public static boolean isCurrentPackUnchanged() {
        return currentPackUnchanged;
    }

    public static String getLastWrittenFingerprint() {
        return lastWrittenFingerprint;
    }

    /* Reset on disconnect so stale state doesn't carry uber */
    public static void reset() {
        currentPackUnchanged = false;
        AtlasDiskCache.reset();
        // Dont clear lastWrittenFingerprint. the save queue may still be draining
    }

    static String buildFingerprint(UUID id, String url, String hash) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String input = id.toString() + "|" + url + "|" + hash;
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return id.toString() + ":" + hash;
        }
    }

    private static String readCached() {
        try {
            if (Files.exists(CACHE_FILE)) {
                return Files.readString(CACHE_FILE).strip();
            }
        } catch (IOException e) {
            PhantasmClient.LOGGER.debug("[Phantasm] TX3 cache read failed: {}", e.getMessage());
        }
        return "";
    }

    private static void writeCached(String fingerprint) {
        try {
            Files.writeString(CACHE_FILE, fingerprint);
        } catch (IOException e) {
            PhantasmClient.LOGGER.debug("[Phantasm] TX3 cache write failed: {}", e.getMessage());
        }
    }
}
