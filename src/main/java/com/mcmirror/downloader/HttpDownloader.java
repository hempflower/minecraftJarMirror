package com.mcmirror.downloader;

import com.mcmirror.config.MirrorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * HTTP downloader with retry, progress reporting, and optional SHA-1 verification.
 */
public class HttpDownloader {

    private static final Logger log = LoggerFactory.getLogger(HttpDownloader.class);

    private final MirrorConfig config;

    public HttpDownloader(MirrorConfig config) {
        this.config = config;
    }

    /**
     * Download a file. Skips if target exists with matching size.
     *
     * @param url       source URL
     * @param targetDir directory to save into
     * @param fileName  output file name
     * @return download result
     */
    public DownloadResult download(String url, Path targetDir, String fileName) {
        return download(url, targetDir, fileName, null, -1);
    }

    /**
     * Download a file with expected SHA-1 verification.
     *
     * @param url         source URL
     * @param targetDir   directory to save into
     * @param fileName    output file name
     * @param expectedSha1 expected SHA-1 hex (null to skip verification)
     * @param expectedSize expected file size in bytes (-1 to skip check)
     * @return download result
     */
    public DownloadResult download(String url, Path targetDir, String fileName,
                                   String expectedSha1, long expectedSize) {
        Path targetFile = targetDir.resolve(fileName);

        // Check if the file already exists and looks complete
        if (Files.exists(targetFile)) {
            try {
                long existingSize = Files.size(targetFile);
                if (existingSize == 0) {
                    Files.delete(targetFile);
                } else if (expectedSize < 0 || existingSize == expectedSize) {
                    // If hash verification is enabled and we have an expected hash,
                    // verify the file's integrity before skipping
                    if (config.isVerifyHash() && expectedSha1 != null && !expectedSha1.isEmpty()) {
                        String actualSha1 = computeSha1(targetFile);
                        if (expectedSha1.equalsIgnoreCase(actualSha1)) {
                            log.debug("File already exists with valid hash, skipping: {}", targetFile);
                            return DownloadResult.skipped(targetFile);
                        } else {
                            log.warn("Existing file has wrong hash (expected {}, got {}), re-downloading: {}",
                                    expectedSha1, actualSha1, targetFile);
                            Files.delete(targetFile);
                        }
                    } else {
                        log.debug("File already exists, skipping: {}", targetFile);
                        return DownloadResult.skipped(targetFile);
                    }
                }
            } catch (IOException e) {
                log.warn("Could not check existing file: {}", targetFile, e);
            }
        }

        // Ensure target directory exists
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("Failed to create directory: {}", targetDir, e);
            return DownloadResult.failure("Cannot create directory: " + e.getMessage(), 0);
        }

        int maxRetries = config.getMaxRetries();
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return doDownload(url, targetFile, expectedSha1, attempt);
            } catch (IOException e) {
                lastException = e;
                if (attempt <= maxRetries) {
                    long backoffMs = (long) Math.pow(2, attempt) * 500L;
                    log.warn("Download failed (attempt {}/{}), retrying in {}ms: {}",
                            attempt, maxRetries + 1, backoffMs, url);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // Clean up partial file
                    try { Files.deleteIfExists(targetFile); } catch (IOException ignored) {}
                }
            }
        }

        return DownloadResult.failure(lastException != null ? lastException.getMessage() : "Unknown error", maxRetries + 1);
    }

    private DownloadResult doDownload(String urlStr, Path targetFile, String expectedSha1, int attempt)
            throws IOException {

        URI uri = URI.create(urlStr);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setConnectTimeout(config.getConnectTimeoutMs());
        conn.setReadTimeout(config.getReadTimeoutMs());
        conn.setRequestProperty("User-Agent", config.getUserAgent());
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new IOException("HTTP " + status + " for " + urlStr);
        }

        long contentLength = conn.getContentLengthLong();
        log.info("Downloading: {} ({})", targetFile.getFileName(),
                contentLength > 0 ? formatSize(contentLength) : "unknown size");

        MessageDigest digest = null;
        if (config.isVerifyHash()) {
            try {
                digest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                log.warn("SHA-1 not available, hash verification disabled");
            }
        }

        long bytesRead;
        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(targetFile)) {

            byte[] buffer = new byte[8192];
            bytesRead = 0;
            int read;
            long lastReportTime = System.currentTimeMillis();
            long lastReportBytes = 0;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                if (digest != null) {
                    digest.update(buffer, 0, read);
                }
                bytesRead += read;

                // Report progress every 500ms
                long now = System.currentTimeMillis();
                if (now - lastReportTime > 500 && contentLength > 0) {
                    long bytesSinceLast = bytesRead - lastReportBytes;
                    double speed = bytesSinceLast / ((now - lastReportTime) / 1000.0);
                    int pct = (int) (bytesRead * 100 / contentLength);
                    log.info("  {} {}% ({} / {}) @ {}/s",
                            targetFile.getFileName(), pct,
                            formatSize(bytesRead), formatSize(contentLength),
                            formatSize((long) speed));
                    lastReportTime = now;
                    lastReportBytes = bytesRead;
                }
            }
        } finally {
            conn.disconnect();
        }

        // Verify content length if known
        if (contentLength > 0 && bytesRead != contentLength) {
            try { Files.delete(targetFile); } catch (IOException ignored) {}
            throw new IOException("Incomplete download: got " + bytesRead + " bytes, expected " + contentLength);
        }

        // Verify SHA-1 if we have it
        String actualSha1 = null;
        if (digest != null) {
            actualSha1 = HexFormat.of().formatHex(digest.digest());
            if (expectedSha1 != null && !expectedSha1.isEmpty()
                    && !actualSha1.equalsIgnoreCase(expectedSha1)) {
                try { Files.delete(targetFile); } catch (IOException ignored) {}
                throw new IOException("SHA-1 mismatch: expected " + expectedSha1 + ", got " + actualSha1);
            }
        }

        log.info("Downloaded: {} ({}) [SHA-1: {}] on attempt {}",
                targetFile.getFileName(), formatSize(bytesRead),
                actualSha1 != null ? actualSha1 : "unverified", attempt);

        return DownloadResult.success(targetFile, bytesRead, actualSha1, attempt);
    }

    /**
     * Compute the SHA-1 hex digest of a local file.
     */
    private static String computeSha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 not available", e);
        }
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
