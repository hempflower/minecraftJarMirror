package com.mcmirror.service;

import com.google.gson.Gson;
import com.mcmirror.config.MirrorConfig;
import com.mcmirror.downloader.DownloadResult;
import com.mcmirror.downloader.HttpDownloader;
import com.mcmirror.model.VersionManifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Handles version manifest fetching, parsing, and listing.
 */
public class VersionService {

    private static final Logger log = LoggerFactory.getLogger(VersionService.class);

    private final MirrorConfig config;
    private final HttpDownloader downloader;
    private final Gson gson;

    public VersionService(MirrorConfig config) {
        this.config = config;
        this.downloader = new HttpDownloader(config);
        this.gson = new Gson();
    }

    /**
     * Fetch and parse the version manifest from Mojang.
     *
     * @return parsed manifest, or null on failure
     */
    public VersionManifest fetchManifest() {
        DownloadResult result = downloader.download(
                config.getVersionManifestUrl(),
                config.getDataDir(),
                "versions.json");

        if (!result.isSuccess()) {
            log.error("Failed to download version manifest: {}", result.getErrorMessage());
            return null;
        }

        try {
            String json = Files.readString(config.getVersionsJsonPath());
            VersionManifest manifest = gson.fromJson(json, VersionManifest.class);
            if (manifest == null || manifest.versions == null || manifest.versions.length == 0) {
                log.error("Version manifest is empty or invalid");
                return null;
            }
            return manifest;
        } catch (IOException e) {
            log.error("Failed to read versions.json", e);
            return null;
        }
    }

    /**
     * List all available versions from the remote manifest.
     * Downloads the manifest on-the-fly to show fresh data.
     */
    public void listVersions() {
        log.info("Fetching version manifest from Mojang...");
        VersionManifest manifest = fetchManifest();
        if (manifest == null) {
            System.out.println("Failed to retrieve version list.");
            return;
        }

        System.out.println();
        System.out.printf("Available versions: %d%n%n", manifest.versions.length);
        System.out.println("┌──────────────────────┬──────────────────────────────────────────────────┐");
        System.out.println("│ Version              │ URL                                              │");
        System.out.println("├──────────────────────┼──────────────────────────────────────────────────┤");
        for (VersionManifest.VersionEntry entry : manifest.versions) {
            System.out.printf("│ %-20s │ %-48s │%n", truncate(entry.id, 20), truncate(entry.url, 48));
        }
        System.out.println("└──────────────────────┴──────────────────────────────────────────────────┘");
        System.out.println();
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }
}
