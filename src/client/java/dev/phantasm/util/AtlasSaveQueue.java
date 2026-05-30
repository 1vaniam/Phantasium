package dev.phantasm.util;

import dev.phantasm.PhantasmClient;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public final class AtlasSaveQueue {

    public record Entry(String fingerprint) {}

    private static final BlockingQueue<Entry> QUEUE = new ArrayBlockingQueue<>(4);

    private AtlasSaveQueue() {}

    public static void enqueue(String fingerprint, Object ignored) {
        if (!QUEUE.offer(new Entry(fingerprint))) {
            PhantasmClient.LOGGER.debug("[Phantasm] TX4 save queue full — skipping sentinel write");
        }
    }

    /*
     Drains one entry and writes the sentinel file to disk
     Safe to call every tick - returns false immediately if queue is empty
    */
    public static boolean drainOne() {
        Entry entry = QUEUE.poll();
        if (entry == null) return false;

        try {
            Path atlasDir = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getConfigDir()
                .resolve("phantasm_atlas_cache")
                .resolve(entry.fingerprint());
            Files.createDirectories(atlasDir);

            Files.writeString(
                atlasDir.resolve("layout.properties"),
                "fingerprint=" + entry.fingerprint() + "\n"
            );

            PhantasmClient.LOGGER.info(
                "[Phantasm] TX4 atlas cache sentinel written for fingerprint {}",
                entry.fingerprint().substring(0, 8));

            AtlasDiskCache.pruneOldEntries();

        } catch (IOException e) {
            PhantasmClient.LOGGER.warn("[Phantasm] TX4 sentinel write failed: {}", e.getMessage());
        }
        return true;
    }

    public static boolean hasPending() { return !QUEUE.isEmpty(); }
}
