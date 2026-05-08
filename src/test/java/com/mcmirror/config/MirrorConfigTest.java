package com.mcmirror.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MirrorConfigTest {

    private MirrorConfig config;

    @BeforeEach
    void setUp() {
        config = new MirrorConfig();
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
        System.setProperty("mcmirror.maxRetries", "5");
        System.setProperty("mcmirror.withAssets", "true");

        MirrorConfig c2 = new MirrorConfig();
        assertThat(c2.getMaxRetries()).isEqualTo(5);
        assertThat(c2.isWithAssets()).isTrue();

        // Clean up
        System.clearProperty("mcmirror.maxRetries");
        System.clearProperty("mcmirror.withAssets");
    }
}
