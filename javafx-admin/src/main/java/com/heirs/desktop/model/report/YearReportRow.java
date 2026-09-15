package com.heirs.desktop.model.report;

import java.util.Locale;

/**
 * One distribution row for the chronological (publication year) dimension.
 */
public record YearReportRow(int year, long count, double percentage) {

    public String yearText() {
        return year > 0 ? String.valueOf(year) : "Unknown";
    }

    public String percentageText() {
        return String.format(Locale.ROOT, "%.1f%%", percentage);
    }
}