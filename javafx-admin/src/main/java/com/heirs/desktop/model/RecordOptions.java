package com.heirs.desktop.model;

import java.util.List;

/** Backend enum labels (accepted JSON values); category endpoint is preferred when online. */
public final class RecordOptions {
    public static final List<String> CATEGORIES = List.of("Policy", "Scheme", "Regulation", "Project", "Rules");
    public static final List<String> STATUSES = List.of("Active", "Draft", "Completed", "Archived");
    private RecordOptions() {}
}
