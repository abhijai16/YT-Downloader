package com.example.ytdownloader;

import com.example.ytdownloader.service.DownloadService;
import com.example.ytdownloader.service.HistoryService;
import com.example.ytdownloader.util.ProcessRunner;
import com.example.ytdownloader.util.UrlValidator;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Scanner;

public final class App {
    private final Scanner scanner = new Scanner(System.in);
    private final ProcessRunner runner = new ProcessRunner();
    private final String ytDlpBinary = System.getenv().getOrDefault("YTDLP_BIN", "yt-dlp");
    private final HistoryService history;
    private final DownloadService downloads;

    private App() throws IOException {
        Path root = Path.of("downloads").toAbsolutePath();
        history = new HistoryService(root.resolve("history.tsv"));
        downloads = new DownloadService(runner, history, ytDlpBinary, root);
    }

    public static void main(String[] args) {
        try {
            new App().start();
        } catch (IOException e) {
            System.err.println("Startup error: " + e.getMessage());
        }
    }

    private void start() {
        if (!available(ytDlpBinary, "--version")) {
            System.out.println("yt-dlp was not found. Install it and make sure it is on PATH.");
            System.out.println("See README.md for setup. (YTDLP_BIN can override its location.)");
            return;
        }
        if (!available("ffmpeg", "-version")) {
            System.out.println("Note: FFmpeg was not found. Merging video and MP3 conversion need it.");
        }
        System.out.println("""
                
                 █████ █████ ███████████            ██████████      ███████    █████   ███   █████ ██████   █████ █████          ███████      █████████   ██████████   ██████████ ███████████ \s
                ▒▒███ ▒▒███ ▒█▒▒▒███▒▒▒█           ▒▒███▒▒▒▒███   ███▒▒▒▒▒███ ▒▒███   ▒███  ▒▒███ ▒▒██████ ▒▒███ ▒▒███         ███▒▒▒▒▒███   ███▒▒▒▒▒███ ▒▒███▒▒▒▒███ ▒▒███▒▒▒▒▒█▒▒███▒▒▒▒▒███\s
                 ▒▒███ ███  ▒   ▒███  ▒             ▒███   ▒▒███ ███     ▒▒███ ▒███   ▒███   ▒███  ▒███▒███ ▒███  ▒███        ███     ▒▒███ ▒███    ▒███  ▒███   ▒▒███ ▒███  █ ▒  ▒███    ▒███\s
                  ▒▒█████       ▒███     ██████████ ▒███    ▒███▒███      ▒███ ▒███   ▒███   ▒███  ▒███▒▒███▒███  ▒███       ▒███      ▒███ ▒███████████  ▒███    ▒███ ▒██████    ▒██████████ \s
                   ▒▒███        ▒███    ▒▒▒▒▒▒▒▒▒▒  ▒███    ▒███▒███      ▒███ ▒▒███  █████  ███   ▒███ ▒▒██████  ▒███       ▒███      ▒███ ▒███▒▒▒▒▒███  ▒███    ▒███ ▒███▒▒█    ▒███▒▒▒▒▒███\s
                    ▒███        ▒███                ▒███    ███ ▒▒███     ███   ▒▒▒█████▒█████▒    ▒███  ▒▒█████  ▒███      █▒▒███     ███  ▒███    ▒███  ▒███    ███  ▒███ ▒   █ ▒███    ▒███\s
                    █████       █████               ██████████   ▒▒▒███████▒      ▒▒███ ▒▒███      █████  ▒▒█████ ███████████ ▒▒▒███████▒   █████   █████ ██████████   ██████████ █████   █████
                   ▒▒▒▒▒       ▒▒▒▒▒               ▒▒▒▒▒▒▒▒▒▒      ▒▒▒▒▒▒▒         ▒▒▒   ▒▒▒      ▒▒▒▒▒    ▒▒▒▒▒ ▒▒▒▒▒▒▒▒▒▒▒    ▒▒▒▒▒▒▒    ▒▒▒▒▒   ▒▒▒▒▒ ▒▒▒▒▒▒▒▒▒▒   ▒▒▒▒▒▒▒▒▒▒ ▒▒▒▒▒   ▒▒▒▒▒\s
                
                
                """);
        while (true) {
            System.out.println("\n1. Download video\n2. Download MP3 audio");
            System.out.println("3. Download playlist\n4. Show download history\n0. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> downloads.downloadVideo(readUrl(), readQuality());
                    case "2" -> downloads.downloadAudio(readUrl());
                    case "3" -> downloads.downloadPlaylist(readUrl(), readQuality());
                    case "4" -> showHistory();
                    case "0" -> { System.out.println("Goodbye!"); return; }
                    default -> System.out.println("Choose one of the listed options.");
                }
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Download was interrupted.");
                return;
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid input: " + e.getMessage());
            }
        }
    }

    private String readUrl() {
        System.out.print("Paste the YouTube URL: ");
        String url = scanner.nextLine().trim();
        if (!UrlValidator.isYouTubeUrl(url)) throw new IllegalArgumentException("Use a valid YouTube URL.");
        return url;
    }

    private int readQuality() {
        System.out.print("Maximum quality (360 / 480 / 720 / 1080): ");
        String raw = scanner.nextLine().trim();
        try {
            int quality = Integer.parseInt(raw);
            if (List.of(360, 480, 720, 1080).contains(quality)) return quality;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Choose 360, 480, 720, or 1080.");
    }


    private void showHistory() throws IOException {
        List<String> entries = history.all();
        if (entries.isEmpty()) {
            System.out.println("No downloads recorded yet.");
        } else {
            System.out.println("\n" + String.join("\n", entries));
        }
    }

    private boolean available(String tool, String versionFlag) {
        try {
            return runner.capture(List.of(tool, versionFlag), Duration.ofSeconds(10)).succeeded();
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
        }
    }
}
