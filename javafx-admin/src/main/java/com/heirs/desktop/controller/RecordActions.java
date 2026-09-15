package com.heirs.desktop.controller;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.stage.Window;
import com.heirs.desktop.api.ApiException;
import com.heirs.desktop.model.Record;
import com.heirs.desktop.model.RecordFormDialog;
import com.heirs.desktop.model.RecordDetailsDialog;
import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.service.RecordService;
import com.heirs.desktop.state.AppState;
import com.heirs.desktop.util.FxAsync;

/** One set of actions for menu, toolbar, table buttons and row context menus. */
public final class RecordActions {
    private final RecordService service = new RecordService();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final Navigator navigator;
    private final Supplier<Window> owner;
    private final Consumer<String> status;
    public RecordActions(Navigator navigator, Supplier<Window> owner, Consumer<String> status) {
        this.navigator = navigator; this.owner = owner; this.status = status;
    }
    public ReadOnlyBooleanProperty busyProperty() { return busy; }
    public void create() {
        if (busy.get()) return;
        openForm(null);
    }
    public void edit(Record record) {
        if (record == null || busy.get()) return;
        busy.set(true);
        status.accept("Loading record for editing...");
        FxAsync.run(() -> service.fetchById(record.getId()), current -> {
            AppState.getInstance().setBackendConnected(true);
            openForm(current);
        }, error -> { busy.set(false); showFailure(error); });
    }
    private void openForm(Record record) {
        busy.set(true);
        try {
            Dialog<Record> dialog = RecordFormDialog.build(owner.get(), record, service, saved -> {
                navigator.refreshRecordsAfterWrite(saved == null ? null : saved.getId(), record == null);
                status.accept(record == null ? "Record created successfully." : "Record updated successfully.");
            }, () -> navigator.refreshRecordsAfterWrite(null, false), status);
            dialog.setOnHidden(event -> busy.set(false));
            dialog.show();
        } catch (RuntimeException error) { busy.set(false); throw error; }
    }
    public void view(Record record) {
        if (record != null && !busy.get()) RecordDetailsDialog.show(owner.get(), record.getId(), this::edit, navigator);
    }
    public void delete(Record record) {
        if (record == null || busy.get()) return;
        busy.set(true);
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(owner.get());
        confirm.setTitle("Delete Record");
        confirm.setHeaderText("Delete Record");
        confirm.setContentText("Are you sure you want to delete:\n\n" + record.getReference() + "\n" + record.getTitle()
                + "\n\nThis action cannot be undone.");
        ButtonType delete = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        confirm.getButtonTypes().setAll(delete, ButtonType.CANCEL);
        Button deleteButton = (Button)confirm.getDialogPane().lookupButton(delete);
        confirm.getDialogPane().getStylesheets().add(RecordActions.class.getResource("/com/heirs/desktop/css/base.css").toExternalForm());
        deleteButton.getStyleClass().add("button-desktop-danger");
        deleteButton.setDefaultButton(false);
        ((Button)confirm.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        confirm.setOnHidden(event -> {
            if (confirm.getResult() != delete) { busy.set(false); return; }
            status.accept("Deleting record...");
            FxAsync.run(() -> { service.delete(record.getId()); return true; }, result -> {
                busy.set(false);
                AppState.getInstance().setBackendConnected(true);
                navigator.refreshRecordsAfterWrite(null, false);
                status.accept("Record deleted successfully.");
            }, error -> { busy.set(false); showFailure(error); });
        });
        confirm.show();
    }
    private void showFailure(Throwable error) {
        String message = "The record operation failed. Please retry.";
        if (error instanceof ApiException api) {
            AppState.getInstance().setBackendConnected(!api.isOffline());
            message = api.isOffline() ? "Backend Offline. Reconnect and retry. No success was confirmed." : api.getUserMessage();
            if (api.getStatusCode() == 404) {
                message = "This record no longer exists. The list will be refreshed.";
                navigator.refreshRecordsAfterWrite(null, false);
            }
        }
        status.accept(message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.initOwner(owner.get());
        alert.setTitle("Record Operation");
        alert.setHeaderText(null);
        alert.show();
    }
}
