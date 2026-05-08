package com.mcmirror.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MirrorConfigTest {

    private MirrorConfig config;
    private final Set<String> testProperties = new java.util.HashSet<>();

    @BeforeEach
    void setUp() {
        config = new MirrorConfig();
    }

    @AfterEach
    void cleanUpSystemProperties() {
        for (String key : testProperties) {
            System.clearProperty(key);
        }
        testProperties.clear();
    }

    void setTestProperty(String key, String value) {
        System.setProperty(key, value);
        testProperties.add(key);
    }

    @Test
    void shouldProvideDefaultValues() {
        assertThat(config.getMaxRetries()).isEqualTo(3);
        assertThat(config.getThreadCount()).isEqualTo(4);
        assertThat(config.isVerifyHash()).isTrue();
        assertThat(config.isWithAssets()).isFalse();
        assertThat(config.getConnectTimeoutMs()).isEqualTo(10_000);
        assertThat(config.getReadTimeoutMs()).isEqualTo(30_000);
        assertThat(config.getUserAgent()).isEqualTo("MCMirror/2.0 (Java)");
    }

    @Test
    void shouldHaveCorrectVersionManifestUrl() {
        assertThat(config.getVersionManifestUrl())
                .isEqualTo("https://launchermeta.mojang.com/mc/game/version_manifest.json");
    }

    @Test
    void shouldResolveDataDir() {
        assertThat(config.getDataDir())
                .isEqualTo(Path.of(System.getProperty("user.dir"), "mcdl"));
    }

    @Test
    void shouldResolveAssetsIndexesDir() {
        assertThat(config.getAssetsIndexesDir())
                .isEqualTo(Path.of(System.getProperty("user.dir"), "mcdl", "assets", "indexes"));
    }

    @Test
    void shouldResolveAssetsObjectsDir() {
        assertThat(config.getAssetsObjectsDir())
                .isEqualTo(Path.of(System.getProperty("user.dir"), "mcdl", "assets", "objects"));
    }

    @Test
    void shouldResolveVersionDir() {
        assertThat(config.getVersionDir("1.12"))
                .isEqualTo(Path.of(System.getProperty("user.dir"), "mcdl", "1.12"));
    }

    @Test
    void shouldOverrideFromSystemProperty() {
        setTestProperty("mcmirror.maxRetries", "5");
        setTestProperty("mcmirror.withAssets", "true");

        MirrorConfig c2 = new MirrorConfig();
        assertThat(c2.getMaxRetries()).isEqualTo(5);
        assertThat(c2.isWithAssets()).isTrue();
    }

    @Test
    void shouldHaveCorrectLegacyAssetUrl() {
        assertThat(config.getLegacyAssetUrl())
                .isEqualTo("https://launchermeta.mojang.com/mc/assets/legacy/c0fd82e8ce9fbc93119e40d96d5a4e62cfa3f729/legacy.json");
    }

    @Test
    void shouldResolveVersionsJsonPath() {
        assertThat(config.getVersionsJsonPath())
                .isEqualTo(Path.of(System.getProperty("user.dir"), "mcdl", "versions.json"));
    }

    @Test
    void shouldResolveLegacyJsonPath() {
        assertThat(config.getLegacyJsonPath())
                .isEqualTo(Path.of(System.getProperty("user.dir"), "mcdl", "assets", "indexes", "legacy.json"));
    }

    @Test
    void shouldHaveDefaultVersionFilterAsNull() {
        assertThat(config.getVersionType()).isNull();
        assertThat(config.getIncludePattern()).isNull();
        assertThat(config.getExcludePattern()).isNull();
    }

    @Test
    void shouldOverrideBaseDirFromSystemProperty() {
        setTestProperty("mcmirror.baseDir", "/custom/mirror");
        MirrorConfig c2 = new MirrorConfig();
        assertThat(c2.getBaseDir()).isEqualTo(Path.of("/custom/mirror"));
        assertThat(c2.getDataDir()).isEqualTo(Path.of("/custom/mirror", "mcdl"));
    }

    @Test
    void shouldOverrideVersionFilterFromSystemProperty() {
        // Use simple patterns without regex backslash escaping to avoid confusion
        setTestProperty("mcmirror.versionType", "snapshot");
        setTestProperty("mcmirror.include", "1[.]16.*");
        setTestProperty("mcmirror.exclude", ".*pre.*");

        MirrorConfig c2 = new MirrorConfig();
        assertThat(c2.getVersionType()).isEqualTo("snapshot");
        assertThat(c2.getIncludePattern()).isEqualTo("1[.]16.*");
        assertThat(c2.getExcludePattern()).isEqualTo(".*pre.*");
    }

    @Test
    void shouldParseBoolVariousCases() {
        setTestProperty("mcmirror.withAssets", "TRUE");
        MirrorConfig c2 = new MirrorConfig();
        assertThat(c2.isWithAssets()).isTrue();
    }

    @Test
    void shouldIgnoreBogusThreadCount() {
        setTestProperty("mcmirror.threads", "not-a-number");
        MirrorConfig c2 = new MirrorConfig();
        assertThat(c2.getThreadCount()).isEqualTo(4); // stays at default
    }
}
