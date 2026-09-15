package com.heirs.desktop.model;

import javafx.scene.control.Dialog;
import javafx.stage.Window;
import com.heirs.desktop.service.RecordService;

/** Compatibility entry point for the existing screenshot smoke sequence. */
public final class AddRecordDialog {
    private static Dialog<Record> lastDialog;
    private AddRecordDialog() {}
    public static Dialog<Record> build(Window owner) {
        lastDialog = RecordFormDialog.build(owner, null, new RecordService(), r -> {}, () -> {}, s -> {});
        return lastDialog;
    }
    public static void closeLast() {
        if (lastDialog != null) lastDialog.close();
        lastDialog = null;
    }
}
