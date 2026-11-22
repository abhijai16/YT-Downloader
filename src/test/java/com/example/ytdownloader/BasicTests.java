package com.example.ytdownloader;

import com.example.ytdownloader.model.DownloadType;
import com.example.ytdownloader.service.HistoryService;
import com.example.ytdownloader.util.ProcessRunner;
import com.example.ytdownloader.util.UrlValidator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public final class BasicTests {
    public static void main(String[] args) throws Exception {
        verify(UrlValidator.isYouTubeUrl("https://www.youtube.com/watch?v=abc123"), "YouTube URL");
        verify(UrlValidator.isYouTubeUrl("https://youtu.be/abc123"), "Short URL");
        verify(UrlValidator.isYouTubeUrl("https://music.youtube.com/watch?v=abc"), "Subdomain URL");
        verify(!UrlValidator.isYouTubeUrl("https://youtube.com.evil.net/watch"), "Spoofed domain");
        verify(!UrlValidator.isYouTubeUrl("https://youtube.com@evil.net/watch"), "Userinfo spoofing");
        verify(!UrlValidator.isYouTubeUrl("file:///etc/passwd"), "File URL");
        verify(!UrlValidator.isYouTubeUrl("-x"), "Command flag");
        verify(!UrlValidator.isYouTubeUrl(null), "Null URL");

        Path dir = Files.createTempDirectory("ytdownloader-test-");
        HistoryService history = new HistoryService(dir.resolve("history.tsv"));
        verify(history.all().isEmpty(), "Initial history");
        history.record("https://youtu.be/abc", DownloadType.VIDEO, "720p", true);
        history.record("https://youtu.be/def", DownloadType.AUDIO, "MP3", false);
        List<String> rows = history.all();
        verify(rows.size() == 3, "History header and 2 rows");
        verify(rows.get(1).contains("SUCCESS"), "Successful history entry");
        verify(rows.get(2).contains("FAILED"), "Failed history entry");

        ProcessRunner.Result result = new ProcessRunner().capture(
                List.of(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-version"),
                Duration.ofSeconds(10));
        verify(result.succeeded(), "ProcessRunner exit status");
        verify(result.output().contains("version"), "ProcessRunner output");

        System.out.println("All 14 checks passed.");
    }

    private static void verify(boolean condition, String description) {
        if (!condition) throw new AssertionError("FAILED: " + description);
    }
}
