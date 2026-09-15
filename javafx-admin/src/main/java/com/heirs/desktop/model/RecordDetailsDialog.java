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
        show(owner, recordId, onEdit, navigator, null, -1);
    }

    public static void show(Window owner, Record record) {
        if (record == null) return;
        if (record.getId() != null) {
            show(owner, record.getId());
        } else {
            displayDialog(owner, record, List.of(), null, null, null, -1);
        }
    }

    /**
     * Open the details dialog for the given record, with Previous / Next
     * navigation stepping through {@code order}. {@code index} is the position
     * of the record inside {@code order}; when {@code order} is {@code null} or
     * contains no ID-bearing records, the plain dialog is shown.
     */
    public static void showInOrder(Window owner, java.util.List<Record> order, Record record,
                                   java.util.function.Consumer<Record> onEdit, Navigator navigator) {
        if (record == null || record.getId() == null) return;
        int index = order == null ? -1 : order.indexOf(record);
        show(owner, record.getId(), onEdit, navigator, order, index);
    }

    private static void show(Window owner, Long recordId, java.util.function.Consumer<Record> onEdit,
                             Navigator navigator, java.util.List<Record> order, int index) {
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
                    displayDialog(owner, result.record, result.docs, onEdit, navigator, order, index);
                },
                error -> {
                    UiBuilders.placeholderAlert(owner, "Record could not be loaded: " + error.getMessage());
                }
        );
    }

    private record RecordAndDocs(Record record, List<DocumentResponse> docs) {}

    private static void displayDialog(Window owner, Record record, List<DocumentResponse> docs,
                                      java.util.function.Consumer<Record> onEdit, Navigator navigator,
                                      java.util.List<Record> order, int index) {
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

            boolean navigable = order != null && order.size() > 1 && index >= 0 && index < order.size();

            ButtonType prevType = new ButtonType("Previous", ButtonBar.ButtonData.LEFT);
            ButtonType nextType = new ButtonType("Next", ButtonBar.ButtonData.LEFT);
            ButtonType editType = new ButtonType("Edit Record...", ButtonBar.ButtonData.LEFT);
            ButtonType docType = new ButtonType("Documents", ButtonBar.ButtonData.LEFT);
            ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

            if (navigable) {
                pane.getButtonTypes().setAll(prevType, nextType, editType, docType, closeType);
            } else {
                pane.getButtonTypes().setAll(editType, docType, closeType);
            }

            if (navigable) {
                var prevBtn = pane.lookupButton(prevType);
                var nextBtn = pane.lookupButton(nextType);
                if (prevBtn != null) {
                    prevBtn.getStyleClass().add("dialog-button");
                    prevBtn.setDisable(index <= 0);
                }
                if (nextBtn != null) {
                    nextBtn.getStyleClass().add("dialog-button");
                    nextBtn.setDisable(index >= order.size() - 1);
                }
            }

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
                if (navigable && result == prevType && index > 0) {
                    Record previous = order.get(index - 1);
                    dialog.close();
                    showInOrder(owner, order, previous, onEdit, navigator);
                } else if (navigable && result == nextType && index < order.size() - 1) {
                    Record next = order.get(index + 1);
                    dialog.close();
                    showInOrder(owner, order, next, onEdit, navigator);
                } else if (result == editType) {
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