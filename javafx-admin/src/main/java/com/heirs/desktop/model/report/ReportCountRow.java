package com.heirs.desktop.model.report;

import java.util.Locale;

/**
 * One distribution row for a nominal dimension (category / status / department).
 */
public record ReportCountRow(String name, long count, double percentage) {

    public String percentageText() {
        return String.format(Locale.ROOT, "%.1f%%", percentage);
    }

    public String countText() {
        return String.valueOf(count);
    }
}