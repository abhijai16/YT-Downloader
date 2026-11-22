package com.example.ytdownloader.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public final class UrlValidator {
    private static final Set<String> EXACT_HOSTS = Set.of("youtube.com", "youtu.be", "youtube-nocookie.com");

    private UrlValidator() { }

    public static boolean isYouTubeUrl(String input) {
        if (input == null || input.isBlank()) return false;
        try {
            URI uri = new URI(input.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || uri.getUserInfo() != null) return false;
            if (!scheme.equalsIgnoreCase("https") && !scheme.equalsIgnoreCase("http")) return false;
            host = host.toLowerCase(Locale.ROOT);
            return EXACT_HOSTS.contains(host)
                    || host.endsWith(".youtube.com")
                    || host.endsWith(".youtube-nocookie.com");
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
