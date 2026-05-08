package com.mcmirror.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Central configuration for the mirror tool.
 * Loads from application.properties on classpath, then overrides via system properties.
 */
public class MirrorConfig {

    private final Path baseDir;
    private final int maxRetries;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int threadCount;
    private final boolean verifyHash;
    private final boolean withAssets;
    private final String userAgent;
    private final String versionManifestUrl;
    private final String legacyAssetUrl;
    private final String versionType;
    private final String includePattern;
    private final String excludePattern;

    private static final String DEFAULT_USER_AGENT = "MCMirror/2.0 (Java)";

    // Mojang API endpoints
    private static final String DEFAULT_VERSION_MANIFEST_URL =
            "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String DEFAULT_LEGACY_ASSET_URL =
            "https://launchermeta.mojang.com/mc/assets/legacy/c0fd82e8ce9fbc93119e40d96d5a4e62cfa3f729/legacy.json";

    // Properties key prefix
    private static final String PREFIX = "mcmirror.";

    private final Properties props;

    public MirrorConfig() {
        this.props = loadProperties();

        this.baseDir = Path.of(getString("baseDir", System.getProperty("user.dir")));
        this.maxRetries = getInt("maxRetries", 3);
        this.connectTimeoutMs = getInt("connectTimeoutMs", 10_000);
        this.readTimeoutMs = getInt("readTimeoutMs", 30_000);
        this.threadCount = getInt("threads", 4);
        this.verifyHash = getBool("verifyHash", true);
        this.withAssets = getBool("withAssets", false);
        this.userAgent = getString("userAgent", DEFAULT_USER_AGENT);
        this.versionManifestUrl = getString("versionManifestUrl", DEFAULT_VERSION_MANIFEST_URL);
        this.legacyAssetUrl = getString("legacyAssetUrl", DEFAULT_LEGACY_ASSET_URL);
        this.versionType = getString("versionType", null);
        this.includePattern = getString("include", null);
        this.excludePattern = getString("exclude", null);
    }

    private Properties loadProperties() {
        Properties p = new Properties();
        try (InputStream in = MirrorConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                p.load(in);
            }
        } catch (IOException e) {
            System.err.println("[WARN] Failed to load application.properties: " + e.getMessage());
        }
        return p;
    }

    private String getString(String suffix, String defaultValue) {
        // System property takes precedence over file
        String sysVal = System.getProperty(PREFIX + suffix);
        if (sysVal != null && !sysVal.isEmpty()) return sysVal;
        String fileVal = props.getProperty(PREFIX + suffix);
        return (fileVal != null && !fileVal.isEmpty()) ? fileVal : defaultValue;
    }

    private int getInt(String suffix, int defaultValue) {
        String val = getString(suffix, null);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private boolean getBool(String suffix, boolean defaultValue) {
        String val = getString(suffix, null);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return defaultValue;
    }

    // --- Paths ---

    public Path getBaseDir() {
        return baseDir;
    }

    public Path getDataDir() {
        return baseDir.resolve("mcdl");
    }

    public Path getVersionsJsonPath() {
        return getDataDir().resolve("versions.json");
    }

    public Path getLegacyJsonPath() {
        return getAssetsIndexesDir().resolve("legacy.json");
    }

    public Path getAssetsIndexesDir() {
        return getDataDir().resolve("assets").resolve("indexes");
    }

    public Path getAssetsObjectsDir() {
        return getDataDir().resolve("assets").resolve("objects");
    }

    public Path getVersionDir(String versionId) {
        return getDataDir().resolve(versionId);
    }

    // --- URLs ---

    public String getVersionManifestUrl() {
        return versionManifestUrl;
    }

    public String getLegacyAssetUrl() {
        return legacyAssetUrl;
    }

    // --- Network ---

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public String getUserAgent() {
        return userAgent;
    }

    // --- Concurrency ---

    public int getThreadCount() {
        return threadCount;
    }

    // --- Verification ---

    public boolean isVerifyHash() {
        return verifyHash;
    }

    // --- Assets ---

    public boolean isWithAssets() {
        return withAssets;
    }

    public String getVersionType() {
        return versionType;
    }

    public String getIncludePattern() {
        return includePattern;
    }

    public String getExcludePattern() {
        return excludePattern;
    }

    // --- Constants ---

    public static final String LEGACY_ASSET_ID = "legacy";

    /** Base URL for downloading individual asset files. */
    public static final String ASSETS_BASE_URL = "https://resources.download.minecraft.net";
}
