package com.mcmirror.downloader;

import com.mcmirror.config.MirrorConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class HttpDownloaderTest {

    private HttpServer server;
    private int port;
    private MirrorConfig config;
    private HttpDownloader downloader;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        port = server.getAddress().getPort();

        // Create config pointing at temp dir
        System.setProperty("mcmirror.baseDir", tempDir.toString());
        System.setProperty("mcmirror.verifyHash", "true");
        System.setProperty("mcmirror.maxRetries", "1");
        config = new MirrorConfig();
        downloader = new HttpDownloader(config);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        System.clearProperty("mcmirror.baseDir");
        System.clearProperty("mcmirror.verifyHash");
        System.clearProperty("mcmirror.maxRetries");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ── helpers ──────────────────────────────────────────────────────

    private void serve(String path, int status, byte[] body, String sha1) {
        server.createContext(path, exchange -> {
            if (sha1 != null) {
                exchange.getResponseHeaders().set("X-SHA1", sha1);
            }
            exchange.sendResponseHeaders(status, body != null ? body.length : -1);
            if (body != null) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }
        });
    }

    private void serve(String path, byte[] body) {
        serve(path, 200, body, null);
    }

    private static byte[] bytes(String s) {
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── download tests ───────────────────────────────────────────────

    @Test
    void shouldDownloadFileSuccessfully() throws IOException {
        String content = "hello world";
        serve("/test.txt", bytes(content));

        Path target = tempDir.resolve("dl");
        DownloadResult result = downloader.download(url("/test.txt"), target, "test.txt",
                sha1(bytes(content)), content.length());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBytesDownloaded()).isEqualTo(content.length());
        Path output = target.resolve("test.txt");
        assertThat(output).exists();
        assertThat(Files.readString(output)).isEqualTo(content);
    }

    @Test
    void shouldSkipWhenFileExistsWithMatchingHash() throws IOException {
        String content = "cached content";
        String hash = sha1(bytes(content));
        Path target = tempDir.resolve("dl");
        Files.createDirectories(target);
        Files.writeString(target.resolve("test.txt"), content);

        DownloadResult result = downloader.download(url("/never-called"), target, "test.txt",
                hash, content.length());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBytesDownloaded()).isZero(); // skipped
    }

    @Test
    void shouldReDownloadWhenHashMismatches() throws IOException {
        String oldContent = "old";
        String newContent = "new content here";
        Path target = tempDir.resolve("dl");
        Files.createDirectories(target);
        Files.writeString(target.resolve("test.txt"), oldContent);

        serve("/new.txt", bytes(newContent));

        DownloadResult result = downloader.download(url("/new.txt"), target, "test.txt",
                sha1(bytes(newContent)), newContent.length());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBytesDownloaded()).isEqualTo(newContent.length());
        assertThat(Files.readString(target.resolve("test.txt"))).isEqualTo(newContent);
    }

    @Test
    void shouldDeleteEmptyExistingFileAndDownload() throws IOException {
        String content = "fresh";
        Path target = tempDir.resolve("dl");
        Files.createDirectories(target);
        Files.writeString(target.resolve("test.txt"), ""); // empty file

        serve("/fresh.txt", bytes(content));

        DownloadResult result = downloader.download(url("/fresh.txt"), target, "test.txt",
                sha1(bytes(content)), content.length());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBytesDownloaded()).isEqualTo(content.length());
    }

    @Test
    void shouldFailOnHttp404() {
        serve("/missing.txt", 404, bytes("Not Found"), null);

        Path target = tempDir.resolve("dl");
        DownloadResult result = downloader.download(url("/missing.txt"), target, "missing.txt");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("HTTP 404");
    }

    @Test
    void shouldRetryOnServerError() {
        AtomicInteger callCount = new AtomicInteger(0);
        server.createContext("/flaky", exchange -> {
            int count = callCount.incrementAndGet();
            if (count < 2) {
                exchange.sendResponseHeaders(503, -1);
            } else {
                byte[] body = bytes("ok on retry");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }
        });

        Path target = tempDir.resolve("dl");
        DownloadResult result = downloader.download(url("/flaky"), target, "retry.txt",
                sha1(bytes("ok on retry")), "ok on retry".length());

        assertThat(result.isSuccess()).isTrue();
        assertThat(callCount.get()).isEqualTo(2);
        assertThat(result.getAttempts()).isEqualTo(2);
    }

    @Test
    void shouldPassThroughLargeFile() throws IOException {
        // Generate 64KB of content
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1024; i++) sb.append("0123456789ABCDEF-");
        String content = sb.toString();
        serve("/large.txt", bytes(content));

        Path target = tempDir.resolve("dl");
        DownloadResult result = downloader.download(url("/large.txt"), target, "large.txt",
                sha1(bytes(content)), content.length());

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBytesDownloaded()).isEqualTo(content.length());
    }

    // ── formatSize / formatTime ──────────────────────────────────────

    @Test
    void shouldFormatBytes() {
        assertThat(HttpDownloader.formatSize(0)).isEqualTo("0 B");
        assertThat(HttpDownloader.formatSize(512)).isEqualTo("512 B");
        assertThat(HttpDownloader.formatSize(2048)).isEqualTo("2.0 KB");
        assertThat(HttpDownloader.formatSize(2 * 1024 * 1024)).isEqualTo("2.0 MB");
        assertThat(HttpDownloader.formatSize(1536L * 1024 * 1024)).isEqualTo("1.50 GB");
    }

    @Test
    void shouldFormatTime() {
        assertThat(HttpDownloader.formatTime(0)).isEqualTo("0ms");
        assertThat(HttpDownloader.formatTime(500)).isEqualTo("500ms");
        assertThat(HttpDownloader.formatTime(1500)).isEqualTo("1.5s");
        assertThat(HttpDownloader.formatTime(90_000)).isEqualTo("1m30s");
    }
}
