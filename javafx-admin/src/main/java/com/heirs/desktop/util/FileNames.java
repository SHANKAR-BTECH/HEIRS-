package com.heirs.desktop.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Filename and size helpers shared by the document API layer and UI.
 */
public final class FileNames {

    private FileNames() {
    }

    /**
     * Derives a safe basename for local saving/opening. Strips any directory
     * components and Windows drive prefixes so a backend-provided value can
     * never write outside the chosen destination.
     */
    public static String safeFileName(String raw) {
        if (raw == null) {
            return "document.pdf";
        }
        String name = raw.trim();
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        name = name.replace(":", "");
        name = name.replace("\"", "").replace("\r", "").replace("\n", "");
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            return "document.pdf";
        }
        return name;
    }

    /**
     * Extracts the safe filename from a Content-Disposition header.
     * Supports both {@code filename="value"} and the extended
     * {@code filename*=UTF-8''<url-encoded>} form.
     *
     * @return the extracted filename, or {@code null} when absent
     */
    public static String parseContentDispositionFilename(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        for (String part : header.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("filename*=")) {
                String value = trimmed.substring("filename*=".length()).trim();
                int firstApostrophe = value.indexOf('\'');
                int lastApostrophe = value.lastIndexOf('\'');
                if (firstApostrophe >= 0 && lastApostrophe > firstApostrophe) {
                    String encoded = value.substring(lastApostrophe + 1);
                    try {
                        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
                    } catch (IllegalArgumentException ignored) {
                        // fall through to the plain filename parameter
                    }
                }
            }
        }
        for (String part : header.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("filename=")) {
                String value = trimmed.substring("filename=".length()).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        }
        return null;
    }

    /** Human-readable byte count: {@code 153 B}, {@code 153 KB}, {@code 1.8 MB}. */
    public static String formatSize(long bytes) {
        if (bytes < 0) {
            bytes = 0;
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            double kb = bytes / 1024.0;
            return kb == Math.floor(kb)
                    ? (long) kb + " KB"
                    : String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = bytes / (1024.0 * 1024.0);
        return mb == Math.floor(mb)
                ? (long) mb + " MB"
                : String.format(Locale.ROOT, "%.1f MB", mb);
    }
}