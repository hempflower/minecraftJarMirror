package com.mcmirror.downloader;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DownloadResultTest {

    @Test
    void successShouldHaveCorrectValues() {
        Path file = Path.of("/tmp/test.jar");
        DownloadResult r = DownloadResult.success(file, 12345L, "abc123", 1);

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getOutputFile()).isEqualTo(file);
        assertThat(r.getBytesDownloaded()).isEqualTo(12345L);
        assertThat(r.getSha1()).isEqualTo("abc123");
        assertThat(r.getAttempts()).isEqualTo(1);
        assertThat(r.getErrorMessage()).isNull();
    }

    @Test
    void failureShouldHaveErrorMessage() {
        DownloadResult r = DownloadResult.failure("Connection refused", 3);

        assertThat(r.isSuccess()).isFalse();
        assertThat(r.getErrorMessage()).isEqualTo("Connection refused");
        assertThat(r.getAttempts()).isEqualTo(3);
        assertThat(r.getOutputFile()).isNull();
        assertThat(r.getBytesDownloaded()).isZero();
        assertThat(r.getSha1()).isNull();
    }

    @Test
    void skippedShouldHaveZeroBytes() {
        Path file = Path.of("/tmp/cached.jar");
        DownloadResult r = DownloadResult.skipped(file);

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getBytesDownloaded()).isZero();
        assertThat(r.getOutputFile()).isEqualTo(file);
        assertThat(r.getSha1()).isNull();
        assertThat(r.getAttempts()).isZero();
        assertThat(r.getErrorMessage()).isNull();
    }
}
