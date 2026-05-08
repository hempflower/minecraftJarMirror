package com.mcmirror.downloader;

import java.nio.file.Path;

/**
 * Represents the result of a download operation.
 */
public class DownloadResult {

    private final boolean success;
    private final Path outputFile;
    private final long bytesDownloaded;
    private final String sha1;
    private final String errorMessage;
    private final int attempts;

    private DownloadResult(boolean success, Path outputFile, long bytesDownloaded,
                           String sha1, String errorMessage, int attempts) {
        this.success = success;
        this.outputFile = outputFile;
        this.bytesDownloaded = bytesDownloaded;
        this.sha1 = sha1;
        this.errorMessage = errorMessage;
        this.attempts = attempts;
    }

    public static DownloadResult success(Path outputFile, long bytesDownloaded, String sha1, int attempts) {
        return new DownloadResult(true, outputFile, bytesDownloaded, sha1, null, attempts);
    }

    public static DownloadResult failure(String errorMessage, int attempts) {
        return new DownloadResult(false, null, 0, null, errorMessage, attempts);
    }

    public static DownloadResult skipped(Path outputFile) {
        return new DownloadResult(true, outputFile, 0, null, null, 0);
    }

    public boolean isSuccess() { return success; }
    public Path getOutputFile() { return outputFile; }
    public long getBytesDownloaded() { return bytesDownloaded; }
    public String getSha1() { return sha1; }
    public String getErrorMessage() { return errorMessage; }
    public int getAttempts() { return attempts; }
}
