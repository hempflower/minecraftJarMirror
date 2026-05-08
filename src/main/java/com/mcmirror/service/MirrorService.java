package com.mcmirror.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mcmirror.Main;
import com.mcmirror.config.MirrorConfig;
import com.mcmirror.downloader.DownloadResult;
import com.mcmirror.downloader.HttpDownloader;
import com.mcmirror.model.VersionDetail;
import com.mcmirror.model.VersionManifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Core mirroring logic — downloads version manifest, version details,
 * client JARs, asset indexes, and optionally full asset files from Mojang.
 */
public class MirrorService {

    private static final Logger log = LoggerFactory.getLogger(MirrorService.class);

    private final MirrorConfig config;
    private final HttpDownloader downloader;
    private final VersionService versionService;
    private final Gson gson;

    // TypeToken for parsing asset index objects map
    private static final Type ASSET_OBJECTS_TYPE = new TypeToken<Map<String, JsonObject>>() {}.getType();

    public MirrorService(MirrorConfig config) {
        this.config = config;
        this.downloader = new HttpDownloader(config);
        this.versionService = new VersionService(config);
        this.gson = new Gson();
    }

    /**
     * Execute the full mirror update.
     */
    public void execute() {
        log.info("=== {} {} ===", Main.NAME, Main.VERSION);
        log.info("Data directory: {}", config.getDataDir().toAbsolutePath());
        log.info("Threads: {}, Max retries: {}, With assets: {}",
                config.getThreadCount(), config.getMaxRetries(), config.isWithAssets());

        // 1. Download legacy asset index (idempotent — skip logic handles already-downloaded)
        log.info("--- Downloading legacy asset index ---");
        DownloadResult legacyResult = downloader.download(
                config.getLegacyAssetUrl(),
                config.getAssetsIndexesDir(),
                MirrorConfig.LEGACY_ASSET_ID + ".json");
        log.info("Legacy: {}", legacyResult.isSuccess() ? "OK" : "FAILED - " + legacyResult.getErrorMessage());

        // 2. Fetch and parse version manifest
        log.info("--- Downloading version manifest ---");
        VersionManifest manifest = versionService.fetchManifest();
        if (manifest == null) {
            log.error("Cannot proceed without version manifest");
            return;
        }

        // Filter versions by type, include, and exclude patterns
        List<VersionManifest.VersionEntry> versions = new java.util.ArrayList<>();
        Pattern includePattern = config.getIncludePattern() != null
                ? Pattern.compile(config.getIncludePattern()) : null;
        Pattern excludePattern = config.getExcludePattern() != null
                ? Pattern.compile(config.getExcludePattern()) : null;

        for (VersionManifest.VersionEntry entry : manifest.versions) {
            // Filter by version type (release, snapshot, old_beta, old_alpha)
            if (config.getVersionType() != null
                    && !config.getVersionType().equalsIgnoreCase(entry.type)) {
                continue;
            }
            // Filter by include regex
            if (includePattern != null && !includePattern.matcher(entry.id).matches()) {
                continue;
            }
            // Filter by exclude regex
            if (excludePattern != null && excludePattern.matcher(entry.id).matches()) {
                continue;
            }
            versions.add(entry);
        }
        log.info("Found {} versions to mirror (filtered from {})", versions.size(), manifest.versions.length);

        // 3. Mirror each version (parallel)
        int threads = Math.min(config.getThreadCount(), versions.size());
        ExecutorService versionExecutor = Executors.newFixedThreadPool(Math.max(1, threads));
        ExecutorService assetExecutor = config.isWithAssets()
                ? Executors.newFixedThreadPool(Math.max(1, threads))
                : null;
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        try {
            for (VersionManifest.VersionEntry entry : versions) {
                versionExecutor.submit(() -> {
                    boolean ok = mirrorVersion(entry, assetExecutor);
                    if (ok) {
                        succeeded.incrementAndGet();
                    } else {
                        failed.incrementAndGet();
                    }
                });
            }

            versionExecutor.shutdown();
            versionExecutor.awaitTermination(60, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            log.warn("Mirror interrupted, shutting down executors");
            versionExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            if (assetExecutor != null) {
                assetExecutor.shutdown();
                try {
                    assetExecutor.awaitTermination(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    assetExecutor.shutdownNow();
                }
            }
        }

        log.info("=== Mirror complete: {} succeeded, {} failed ===",
                succeeded.get(), failed.get());
    }

    /**
     * Mirror a single version: download its detail JSON, client JAR, and asset index.
     * If withAssets is enabled, also downloads the individual asset files.
     */
    private boolean mirrorVersion(VersionManifest.VersionEntry entry, ExecutorService assetExecutor) {
        log.info("=== Processing version: {} ===", entry.id);

        try {
            // Ensure version directory exists
            Path versionDir = config.getVersionDir(entry.id);
            Files.createDirectories(versionDir);

            // a. Download version detail JSON
            DownloadResult jsonResult = downloader.download(
                    entry.url, versionDir, entry.id + ".json");
            if (!jsonResult.isSuccess()) {
                log.error("  [{}] Failed to download version JSON: {}", entry.id, jsonResult.getErrorMessage());
                return false;
            }

            // b. Parse version detail
            String versionJson = Files.readString(versionDir.resolve(entry.id + ".json"));
            VersionDetail detail = gson.fromJson(versionJson, VersionDetail.class);
            if (detail == null) {
                log.error("  [{}] Failed to parse version JSON", entry.id);
                return false;
            }

            // c. Download client JAR
            if (detail.downloads != null && detail.downloads.client != null) {
                DownloadResult jarResult = downloader.download(
                        detail.downloads.client.url, versionDir, entry.id + ".jar",
                        detail.downloads.client.sha1, detail.downloads.client.size);
                if (jarResult.isSuccess()) {
                    log.info("  [{}] Client JAR: {}", entry.id,
                            jarResult.getBytesDownloaded() > 0
                                    ? HttpDownloader.formatSize(jarResult.getBytesDownloaded())
                                    : "skipped");
                } else {
                    log.warn("  [{}] Client JAR failed: {}", entry.id, jarResult.getErrorMessage());
                }
            }

            // d. Download server JAR
            if (detail.downloads != null && detail.downloads.server != null) {
                DownloadResult srvResult = downloader.download(
                        detail.downloads.server.url, versionDir, "minecraft_server." + entry.id + ".jar",
                        detail.downloads.server.sha1, detail.downloads.server.size);
                log.info("  [{}] Server JAR: {}", entry.id,
                        srvResult.isSuccess() ? (srvResult.getBytesDownloaded() > 0
                                ? HttpDownloader.formatSize(srvResult.getBytesDownloaded()) : "skipped")
                                : "FAILED - " + srvResult.getErrorMessage());
            }

            // e. Download asset index (skip legacy — already downloaded, and skip if null)
            if (detail.assetIndex != null && detail.assetIndex.url != null
                    && !MirrorConfig.LEGACY_ASSET_ID.equals(detail.assetIndex.id)) {
                DownloadResult indexResult = downloader.download(
                        detail.assetIndex.url,
                        config.getAssetsIndexesDir(),
                        detail.assetIndex.id + ".json",
                        detail.assetIndex.sha1,
                        -1);
                log.info("  [{}] Asset index '{}': {}", entry.id, detail.assetIndex.id,
                        indexResult.isSuccess() ? "OK" : "FAILED - " + indexResult.getErrorMessage());
            }

            // f. Download actual asset files (if enabled AND asset index is available)
            if (config.isWithAssets() && detail.assetIndex != null && assetExecutor != null) {
                downloadAssetFiles(entry.id, detail.assetIndex.id, assetExecutor);
            }

            log.info("  [{}] Done", entry.id);
            return true;

        } catch (IOException e) {
            log.error("  [{}] Error: {}", entry.id, e.getMessage());
            return false;
        }
    }

    // ── Asset file download ──────────────────────────────────────────

    /**
     * Reads the asset index JSON for the given version and downloads every
     * referenced asset file from Mojang's resources server.
     *
     * Asset files are stored as: assets/objects/{hash[0..1]}/{hash}
     */
    private void downloadAssetFiles(String versionId, String assetIndexId, ExecutorService assetExecutor) {
        Path indexesDir = config.getAssetsIndexesDir();

        // Find the asset index file. The file is named by assetIndex.id
        // (which may differ from versionId when versions share an index).
        Path indexFile = indexesDir.resolve(assetIndexId + ".json");
        if (!Files.exists(indexFile)) {
            log.warn("  [{}] No asset index file '{}' found", versionId, assetIndexId);
            return;
        }

        log.info("  [{}] Downloading asset files from '{}'...", versionId, indexFile.getFileName());
        long start = System.currentTimeMillis();

        try {
            String json = Files.readString(indexFile);
            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root == null || !root.has("objects")) {
                log.warn("  [{}] Asset index has no 'objects' field", versionId);
                return;
            }

            Map<String, JsonObject> objects = gson.fromJson(root.get("objects"), ASSET_OBJECTS_TYPE);
            if (objects == null || objects.isEmpty()) {
                log.warn("  [{}] Asset index 'objects' is empty", versionId);
                return;
            }

            log.info("  [{}] Found {} asset files to download", versionId, objects.size());

            AtomicInteger assetOk = new AtomicInteger(0);
            AtomicInteger assetFail = new AtomicInteger(0);
            AtomicInteger assetSkipped = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(objects.size());

            for (Map.Entry<String, JsonObject> entry : objects.entrySet()) {
                JsonObject obj = entry.getValue();
                String hash = obj.has("hash") ? obj.get("hash").getAsString() : null;
                long size = obj.has("size") ? obj.get("size").getAsLong() : -1;

                if (hash == null) {
                    latch.countDown();
                    continue;
                }

                assetExecutor.submit(() -> {
                    try {
                        String assetUrl = String.format("%s/%s/%s",
                                MirrorConfig.ASSETS_BASE_URL, hash.substring(0, 2), hash);
                        Path assetDir = config.getAssetsObjectsDir().resolve(hash.substring(0, 2));
                        DownloadResult result = downloader.download(assetUrl, assetDir, hash, hash, size);
                        if (result.getBytesDownloaded() > 0) {
                            assetOk.incrementAndGet();
                        } else if (result.isSuccess()) {
                            assetSkipped.incrementAndGet();
                        } else {
                            assetFail.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all asset downloads for this version to complete
            boolean completed = latch.await(30, TimeUnit.MINUTES);
            long elapsed = System.currentTimeMillis() - start;

            if (!completed) {
                log.warn("  [{}] Asset download timed out after {} ({} succeeded, {} failed, {} skipped)",
                        versionId, HttpDownloader.formatTime(elapsed),
                        assetOk.get(), assetFail.get(), assetSkipped.get());
            } else {
                log.info("  [{}] Assets complete in {}: {} downloaded, {} failed, {} skipped",
                        versionId, HttpDownloader.formatTime(elapsed),
                        assetOk.get(), assetFail.get(), assetSkipped.get());
            }

        } catch (IOException e) {
            log.error("  [{}] Failed to read asset index: {}", versionId, e.getMessage());
        } catch (InterruptedException e) {
            log.warn("  [{}] Asset download interrupted", versionId);
            Thread.currentThread().interrupt();
        }
    }

    // ── Status ────────────────────────────────────────────────────────

    /**
     * Check local mirror status — reports which versions have their
     * JSON, client JAR, and asset index downloaded locally.
     */
    public void status() {
        System.out.println();
        System.out.println("Scanning local mirror at: " + config.getDataDir().toAbsolutePath());
        System.out.println();

        Path dataDir = config.getDataDir();
        if (!Files.exists(dataDir)) {
            System.out.println("No mirror data found. Run 'update' first.");
            return;
        }

        // Count version directories (those containing a .json + .jar)
        int versionCount = 0;
        int jarsFound = 0;
        int indexesFound = 0;

        try (Stream<Path> dirs = Files.list(dataDir)) {
            List<Path> versionDirs = dirs
                    .filter(Files::isDirectory)
                    .filter(d -> !d.getFileName().toString().equals("assets"))
                    .toList();

            versionCount = versionDirs.size();

            System.out.println("Versions found locally: " + versionCount);
            System.out.println();

            for (Path vd : versionDirs) {
                String vid = vd.getFileName().toString();
                boolean hasJar = Files.exists(vd.resolve(vid + ".jar"));
                boolean hasJson = Files.exists(vd.resolve(vid + ".json"));
                if (hasJar) jarsFound++;
                String flags = (hasJar ? " [JAR]" : "") + (hasJson ? " [JSON]" : "");
                System.out.printf("  %-20s%s%n", vid, flags.isEmpty() ? " (empty)" : flags);
            }

            System.out.println();
        } catch (IOException e) {
            log.error("Failed to scan data directory", e);
        }

        // Count asset indexes
        Path indexesDir = config.getAssetsIndexesDir();
        if (Files.exists(indexesDir)) {
            try (Stream<Path> indexFiles = Files.list(indexesDir)) {
                indexesFound = (int) indexFiles.filter(Files::isRegularFile).count();
            } catch (IOException ignored) {}
        }

        // Count asset objects
        int assetObjectCount = 0;
        Path objectsDir = config.getAssetsObjectsDir();
        if (Files.exists(objectsDir)) {
            try (Stream<Path> objDirs = Files.walk(objectsDir, 2)) {
                assetObjectCount = (int) objDirs.filter(Files::isRegularFile).count();
            } catch (IOException ignored) {}
        }

        System.out.printf("Asset indexes: %d%n", indexesFound);
        System.out.printf("Asset objects: %d%n", assetObjectCount);
        System.out.printf("Client JARs:   %d / %d versions%n", jarsFound, versionCount);
        System.out.println();
    }
}
