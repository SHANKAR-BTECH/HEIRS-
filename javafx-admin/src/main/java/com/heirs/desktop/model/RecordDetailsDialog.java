package com.heirs.desktop.model;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Modality;
import javafx.stage.Window;

import com.heirs.desktop.api.DocumentsApi;
import com.heirs.desktop.controller.DocumentsController;
import com.heirs.desktop.controller.RecordDetailsController;
import com.heirs.desktop.dto.DocumentResponse;
import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.ViewType;
import com.heirs.desktop.service.RecordService;
import com.heirs.desktop.util.FxAsync;
import com.heirs.desktop.util.UiBuilders;

public final class RecordDetailsDialog {

    private static final String FXML = "/com/heirs/desktop/fxml/record-details.fxml";
    private static final RecordService recordService = new RecordService();
    private static final DocumentsApi documentsApi = new DocumentsApi();

    private RecordDetailsDialog() {
    }

    public static void show(Window owner, Long recordId) {
        show(owner, recordId, null, null);
    }

    public static void show(Window owner, Long recordId, java.util.function.Consumer<Record> onEdit) {
        show(owner, recordId, onEdit, null);
    }

    public static void show(Window owner, Long recordId, java.util.function.Consumer<Record> onEdit, Navigator navigator) {
        if (recordId == null) return;

        FxAsync.run(
                () -> {
                    Record record = recordService.fetchById(recordId);
                    List<DocumentResponse> docs = documentsApi.getDocumentsForRecord(recordId);
                    return new RecordAndDocs(record, docs);
                },
                result -> {
                    if (result.record == null) {
                        UiBuilders.placeholderAlert(owner, "Record #" + recordId + " could not be loaded from backend.");
                        return;
                    }
                    displayDialog(owner, result.record, result.docs, onEdit, navigator);
                },
                error -> {
                    UiBuilders.placeholderAlert(owner, "Record could not be loaded: " + error.getMessage());
                }
        );
    }

    public static void show(Window owner, Record record) {
        if (record == null) return;
        if (record.getId() != null) {
            show(owner, record.getId());
        } else {
            displayDialog(owner, record, List.of(), null, null);
        }
    }

    private record RecordAndDocs(Record record, List<DocumentResponse> docs) {}

    private static void displayDialog(Window owner, Record record, List<DocumentResponse> docs,
                                      java.util.function.Consumer<Record> onEdit, Navigator navigator) {
        try {
            FXMLLoader loader = new FXMLLoader(RecordDetailsDialog.class.getResource(FXML));
            DialogPane pane = loader.load();

            RecordDetailsController controller = loader.getController();
            controller.setRecord(record);
            if (docs != null && !docs.isEmpty()) {
                controller.setDocuments(docs);
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Record Details \u2014 " + record.getReference());
            dialog.setHeaderText(null);
            dialog.setGraphic(null);

            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.initModality(Modality.APPLICATION_MODAL);

            ButtonType editType = new ButtonType("Edit Record...", ButtonBar.ButtonData.LEFT);
            ButtonType docType = new ButtonType("Documents", ButtonBar.ButtonData.LEFT);
            ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

            pane.getButtonTypes().setAll(editType, docType, closeType);

            var editBtn = pane.lookupButton(editType);
            if (editBtn != null) {
                editBtn.getStyleClass().add("dialog-button");
                editBtn.setDisable(onEdit == null);
            }
            var docBtn = pane.lookupButton(docType);
            if (docBtn != null) docBtn.getStyleClass().add("dialog-button");
            var closeBtn = pane.lookupButton(closeType);
            if (closeBtn != null) closeBtn.getStyleClass().addAll("dialog-button", "button-desktop-secondary");

            for (String sheet : List.of(
                    "/com/heirs/desktop/css/base.css",
                    "/com/heirs/desktop/css/forms.css",
                    "/com/heirs/desktop/css/dialogs.css")) {
                pane.getStylesheets().add(RecordDetailsDialog.class.getResource(sheet).toExternalForm());
            }

            dialog.showAndWait().ifPresent(result -> {
                if (result == editType) {
                    if (onEdit != null) onEdit.accept(record);
                } else if (result == docType) {
                    if (navigator != null) {
                        dialog.close();
                        navigator.navigateTo(ViewType.DOCUMENTS);
                        if (navigator.currentController() instanceof DocumentsController documentsController) {
                            documentsController.preselectRecord(record.getId());
                        }
                    } else {
                        showDocumentsInfo(owner, docs);
                    }
                }
            });

        } catch (IOException e) {
            throw new IllegalStateException("Unable to load Record Details dialog", e);
        }
    }

    private static void showDocumentsInfo(Window owner, List<DocumentResponse> docs) {
        if (docs != null && !docs.isEmpty()) {
            StringBuilder sb = new StringBuilder("Attached Documents (" + docs.size() + "):\n\n");
            for (DocumentResponse d : docs) {
                sb.append("• ").append(d.originalFileName() != null ? d.originalFileName() : "unnamed")
                  .append(" (").append(com.heirs.desktop.util.FileNames.formatSize(
                          d.fileSize() != null ? d.fileSize() : 0L)).append(")\n");
            }
            Alert info = new Alert(Alert.AlertType.INFORMATION, sb.toString());
            info.setTitle("Supporting Documents");
            info.setHeaderText(null);
            if (owner != null) info.initOwner(owner);
            info.show();
        } else {
            UiBuilders.placeholderAlert(owner, "No documents currently attached to this record.");
        }
    }
}