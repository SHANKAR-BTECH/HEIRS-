package com.heirs.desktop.model;

import java.util.List;

/**
 * Deterministic placeholder documents for the Phase J1 UI shell.
 */
public final class DemoDocuments {

    private DemoDocuments() {
    }

    private static final List<DemoDocument> DOCUMENTS = List.of(
            new DemoDocument("National_Digital_Learning_Policy_2026.pdf", "PDF", "2.4 MB", "2026-02-15", "HEIRS/POL/2026/014", "Active"),
            new DemoDocument("Technical_Institutions_Quality_Guidelines.pdf", "PDF", "1.1 MB", "2026-01-10", "HEIRS/REG/2026/009", "Active"),
            new DemoDocument("Postgraduate_Scholarship_Scheme_Brochure.docx", "DOCX", "480 KB", "2025-11-20", "HEIRS/SCH/2025/007", "Active"),
            new DemoDocument("University_Research_Grant_Rules_v3.pdf", "PDF", "3.2 MB", "2025-08-14", "HEIRS/RUL/2025/003", "Active"),
            new DemoDocument("Central_Ecosystem_Digital_Platform_Blueprint.pdf", "PDF", "5.6 MB", "2025-05-02", "HEIRS/PRJ/2025/012", "Active"),
            new DemoDocument("Higher_Education_Affiliation_Norms_2024.pdf", "PDF", "890 KB", "2024-12-01", "HEIRS/REG/2024/022", "Completed"),
            new DemoDocument("Faculty_Development_Initiative_Framework.docx", "DOCX", "720 KB", "2024-09-18", "HEIRS/PRJ/2024/005", "Completed"),
            new DemoDocument("Inter_University_Credit_Transfer_Draft.pdf", "PDF", "1.5 MB", "2026-03-01", "HEIRS/POL/2026/018", "Draft")
    );

    public static List<DemoDocument> all() {
        return DOCUMENTS;
    }
}
