package com.heirs.desktop.model;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Window;
import com.heirs.desktop.api.ApiException;
import com.heirs.desktop.controller.RecordFormController;
import com.heirs.desktop.dto.RecordRequest;
import com.heirs.desktop.service.RecordService;
import com.heirs.desktop.state.AppState;
import com.heirs.desktop.util.FxAsync;

/** Shared CREATE/EDIT form, using the existing traditional dialog layout. */
public final class RecordFormDialog {
    public enum Mode { CREATE, EDIT }
    private RecordFormDialog() {}

    public static Dialog<Record> build(Window owner, Record original, RecordService service,
            Consumer<Record> saved, Runnable missing, Consumer<String> status) {
        try {
            Mode mode = original == null ? Mode.CREATE : Mode.EDIT;
            FXMLLoader loader = new FXMLLoader(RecordFormDialog.class.getResource("/com/heirs/desktop/fxml/add-record.fxml"));
            DialogPane pane = loader.load();
            RecordFormController form = loader.getController();
            if (original != null) form.populate(RecordRequest.from(original));
            Dialog<Record> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle(mode == Mode.CREATE ? "Add Record" : "Edit Record");
            if (owner != null) dialog.initOwner(owner);
            dialog.initModality(Modality.APPLICATION_MODAL);
            ButtonType saveType = new ButtonType(mode == Mode.CREATE ? "Save Record" : "Save Changes", ButtonBar.ButtonData.OK_DONE);
            pane.getButtonTypes().setAll(saveType, ButtonType.CANCEL);
            Button save = (Button)pane.lookupButton(saveType);
            save.setId("saveRecord");
            save.getStyleClass().add("button-save");
            pane.lookupButton(ButtonType.CANCEL).getStyleClass().add("button-desktop-secondary");
            for (String css : new String[]{"base", "forms", "dialogs"})
                pane.getStylesheets().add(RecordFormDialog.class.getResource("/com/heirs/desktop/css/" + css + ".css").toExternalForm());
            boolean[] saving = {false};
            dialog.setResultConverter(button -> null);
            dialog.setOnCloseRequest(event -> { if (saving[0]) event.consume(); });
            save.addEventFilter(ActionEvent.ACTION, event -> {
                event.consume();
                if (saving[0]) return;
                RecordRequest request = form.validatedRequest();
                if (request == null) { dialog.getDialogPane().getScene().getWindow().sizeToScene(); return; }
                saving[0] = true;
                save.setDisable(true);
                pane.lookupButton(ButtonType.CANCEL).setDisable(true);
                form.setSaving(true);
                FxAsync.run(() -> mode == Mode.CREATE ? service.create(request) : service.update(original.getId(), request), result -> {
                    saving[0] = false;
                    AppState.getInstance().setBackendConnected(true);
                    dialog.setResult(result);
                    dialog.close();
                    saved.accept(result);
                }, error -> {
                    saving[0] = false;
                    save.setDisable(false);
                    pane.lookupButton(ButtonType.CANCEL).setDisable(false);
                    form.setSaving(false);
                    if (error instanceof ApiException api) {
                        AppState.getInstance().setBackendConnected(!api.isOffline());
                        if (api.getStatusCode() == 404 && mode == Mode.EDIT) {
                            form.showErrors(Map.of(), "This record no longer exists. The list will be refreshed.");
                            save.setDisable(true);
                            missing.run();
                        } else {
                            String message = api.isOffline()
                                    ? "Backend Offline. Your entries are preserved. Reconnect and click Save to retry."
                                    : api.getUserMessage();
                            form.showErrors(RecordValidation.backendFields(api), message);
                        }
                        status.accept(api.isOffline() ? "Backend Offline." : "Record could not be saved.");
                    } else {
                        form.showErrors(Map.of(), "Record could not be saved. Please retry.");
                        status.accept("Record could not be saved.");
                    }
                    dialog.getDialogPane().getScene().getWindow().sizeToScene();
                });
            });
            return dialog;
        } catch (IOException e) { throw new IllegalStateException("Unable to load record form", e); }
    }
}
