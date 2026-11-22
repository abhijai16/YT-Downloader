package com.example.ytdownloader.service;

import com.example.ytdownloader.model.DownloadType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

public final class HistoryService {
    private final Path historyFile;

    public HistoryService(Path historyFile) { this.historyFile = historyFile; }

    public synchronized void record(String url, DownloadType type, String setting, boolean success)
            throws IOException {
        Files.createDirectories(historyFile.getParent());
        if (Files.notExists(historyFile)) {
            Files.writeString(historyFile, "timestamp\ttype\tsetting\tstatus\turl\n",
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        String safeUrl = url.replaceAll("[\\t\\n\\r]", " ");
        String line = Instant.now() + "\t" + type + "\t" + setting + "\t"
                + (success ? "SUCCESS" : "FAILED") + "\t" + safeUrl + "\n";
        Files.writeString(historyFile, line, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    public List<String> all() throws IOException {
        if (Files.notExists(historyFile)) return List.of();
        return Files.readAllLines(historyFile, StandardCharsets.UTF_8);
    }
}
