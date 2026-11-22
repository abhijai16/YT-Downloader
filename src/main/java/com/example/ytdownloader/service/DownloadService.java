package com.example.ytdownloader.service;

import com.example.ytdownloader.model.DownloadType;
import com.example.ytdownloader.util.ProcessRunner;
import com.example.ytdownloader.util.UrlValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DownloadService {
    private static final String OUTPUT_NAME = "%(title).150B [%(id)s].%(ext)s";
    private final ProcessRunner runner;
    private final HistoryService history;
    private final String ytDlpBinary;
    private final Path root;

    public DownloadService(ProcessRunner runner, HistoryService history, String ytDlpBinary, Path root)
            throws IOException {
        this.runner = runner;
        this.history = history;
        this.ytDlpBinary = ytDlpBinary;
        this.root = root.toAbsolutePath().normalize();
        Files.createDirectories(this.root.resolve("videos"));
        Files.createDirectories(this.root.resolve("audio"));
        Files.createDirectories(this.root.resolve("thumbnails"));
        Files.createDirectories(this.root.resolve("metadata"));
    }

    public boolean downloadVideo(String url, int maximumHeight) throws IOException, InterruptedException {
        if (!List.of(360, 480, 720, 1080).contains(maximumHeight)) {
            throw new IllegalArgumentException("Quality must be 360, 480, 720 or 1080.");
        }
        List<String> args = baseCommand(root.resolve("videos"));
        args.add("--no-playlist");
        args.add("-f");
        args.add("bv*[height<=" + maximumHeight + "]+ba/b[height<=" + maximumHeight + "]");
        args.add("--merge-output-format");
        args.add("mp4");
        return run(args, url, DownloadType.VIDEO, maximumHeight + "p");
    }

    public boolean downloadAudio(String url) throws IOException, InterruptedException {
        List<String> args = baseCommand(root.resolve("audio"));
        args.addAll(List.of("--no-playlist", "-x", "--audio-format", "mp3", "--audio-quality", "0"));
        return run(args, url, DownloadType.AUDIO, "MP3");
    }

    public boolean downloadPlaylist(String url, int maximumHeight) throws IOException, InterruptedException {
        if (!List.of(360, 480, 720, 1080).contains(maximumHeight)) {
            throw new IllegalArgumentException("Quality must be 360, 480, 720 or 1080.");
        }
        List<String> args = baseCommand(root.resolve("videos"), "%(playlist_title)s/" + OUTPUT_NAME);
        args.addAll(List.of("--yes-playlist", "-f",
                "bv*[height<=" + maximumHeight + "]+ba/b[height<=" + maximumHeight + "]",
                "--merge-output-format", "mp4"));
        return run(args, url, DownloadType.PLAYLIST, maximumHeight + "p");
    }

    private List<String> baseCommand(Path outputDirectory) {
        return baseCommand(outputDirectory, OUTPUT_NAME);
    }

    private List<String> baseCommand(Path outputDirectory, String outputTemplate) {
        List<String> args = new ArrayList<>(List.of(
                ytDlpBinary, "--newline", "--no-overwrites", "--windows-filenames",
                "--write-info-json", "--write-thumbnail",
                "--no-write-playlist-metafiles",
                "-P", "home:" + outputDirectory,
                "-P", "thumbnail:" + root.resolve("thumbnails"),
                "-P", "infojson:" + root.resolve("metadata"),
                "-o", outputTemplate));

        String browser = System.getenv("YTDLP_BROWSER");

        if(browser != null && !browser.isBlank()){
            args.add("--cookies-from-browser");
            args.add(browser.trim());
        }

        return args;

    }

    private boolean run(List<String> args, String url, DownloadType type, String setting)
            throws IOException, InterruptedException {
        if (!UrlValidator.isYouTubeUrl(url)) throw new IllegalArgumentException("Not a YouTube URL.");
        args.add("--");
        args.add(url.trim());
        System.out.println("\nStarting " + type + " download...");
        int exit = runner.stream(args, System.out::println);
        boolean successful = exit == 0;
        history.record(url, type, setting, successful);
        System.out.println(successful ? "\nDownload completed." : "\nDownload failed (exit code " + exit + ").");
        return successful;
    }
}
