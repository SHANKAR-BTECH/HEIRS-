package com.heirs.desktop.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Minimal, safe CSV writer. Centralizes RFC-4180-style escaping, UTF-8 output
 * and row writing so no controller duplicates CSV logic.
 *
 * <p>Escaping rule: a field is quoted when it contains a comma, quote,
 * carriage return or newline; embedded quotes are doubled.
 */
public final class CsvExporter {

    private CsvExporter() {
    }

    /** Returns the RFC-4180-safe cell text for a raw value. */
    public static String escape(Object raw) {
        String value = raw == null ? "" : String.valueOf(raw);
        boolean needsQuoting = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0;
        if (!needsQuoting) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** Builds one escaped CSV line from the given cell values. */
    public static String row(List<?> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(values.get(i)));
        }
        return sb.toString();
    }

    /** Builds one escaped CSV line (varargs convenience). */
    public static String row(Object... values) {
        return row(List.of(values));
    }

    /**
     * Writes the given rows (including an optional header) to {@code target}
     * as UTF-8 text with CRLF line terminators.
     */
    public static void write(Path target, List<List<String>> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (List<String> cells : rows) {
            sb.append(row(cells)).append("\r\n");
        }
        Files.write(target, sb.toString().getBytes(StandardCharsets.UTF_8));
    }
}