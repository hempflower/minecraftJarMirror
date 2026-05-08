package com.mcmirror.service;

import com.mcmirror.config.MirrorConfig;
import com.mcmirror.model.VersionManifest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class VersionServiceTest {

    private HttpServer server;
    private int port;
    private MirrorConfig config;
    private VersionService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        port = server.getAddress().getPort();

        System.setProperty("mcmirror.baseDir", tempDir.toString());
        System.setProperty("mcmirror.verifyHash", "false"); // skip hash for simplicity
        config = new MirrorConfig();
        service = new VersionService(config);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        System.clearProperty("mcmirror.baseDir");
        System.clearProperty("mcmirror.verifyHash");
    }

    private String manifestUrl() {
        return "http://localhost:" + port + "/version_manifest.json";
    }

    private void serveManifest(String json) {
        System.setProperty("mcmirror.versionManifestUrl", manifestUrl());
        // Re-create config to pick up the new URL
        config = new MirrorConfig();
        service = new VersionService(config);

        server.createContext("/version_manifest.json", exchange -> {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
    }

    @Test
    void shouldFetchAndParseManifest() {
        serveManifest("""
                {
                  "versions": [
                    { "id": "1.21", "type": "release", "url": "https://example.com/1.21.json" },
                    { "id": "24w14a", "type": "snapshot", "url": "https://example.com/24w14a.json" }
                  ]
                }
                """);

        VersionManifest manifest = service.fetchManifest();

        assertThat(manifest).isNotNull();
        assertThat(manifest.versions).hasSize(2);
        assertThat(manifest.versions[0].id).isEqualTo("1.21");
        assertThat(manifest.versions[0].type).isEqualTo("release");
        assertThat(manifest.versions[1].id).isEqualTo("24w14a");
        assertThat(manifest.versions[1].type).isEqualTo("snapshot");
    }

    @Test
    void shouldReturnNullWhenManifestIsEmpty() {
        serveManifest("""
                { "versions": [] }
                """);

        VersionManifest manifest = service.fetchManifest();

        assertThat(manifest).isNull();
    }

    @Test
    void shouldReturnNullWhenManifestHasNoVersionsField() {
        serveManifest("""
                { "latest": { "release": "1.21" } }
                """);

        VersionManifest manifest = service.fetchManifest();

        assertThat(manifest).isNull();
    }

    @Test
    void shouldReturnNullOnServerError() {
        System.setProperty("mcmirror.versionManifestUrl",
                "http://localhost:" + port + "/nonexistent");
        config = new MirrorConfig();
        service = new VersionService(config);

        VersionManifest manifest = service.fetchManifest();

        assertThat(manifest).isNull();
    }
}
