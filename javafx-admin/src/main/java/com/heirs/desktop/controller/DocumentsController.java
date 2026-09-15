package com.heirs.desktop.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import com.heirs.desktop.api.ApiException;
import com.heirs.desktop.model.DocumentInfo;
import com.heirs.desktop.model.Record;
import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.NavigatorAware;
import com.heirs.desktop.service.DocumentService;
import com.heirs.desktop.service.RecordService;
import com.heirs.desktop.state.AppState;
import com.heirs.desktop.util.DesktopSupport;
import com.heirs.desktop.util.FileNames;
import com.heirs.desktop.util.FxAsync;

/**
 * Traditional document-administration screen backed by the real Spring Boot
 * document REST API. All HTTP operations run asynchronously; the table never
 * shows fabricated rows.
 */
public class DocumentsController implements NavigatorAware {

    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

    @FXML private ComboBox<Record> recordPicker;
    @FXML private TextField filterField;
    @FXML private Button btnAddDoc;
    @FXML private Button btnOpenDoc;
    @FXML private Button btnDownloadDoc;
    @FXML private Button btnReplaceDoc;
    @FXML private Button btnRemoveDoc;
    @FXML private Button btnRefreshDoc;

    @FXML private TableView<DocumentInfo> documentsTable;
    @FXML private Label lblDocStatus;

    private Navigator navigator;
    private final RecordService recordService = new RecordService();
    private final DocumentService documentService = new DocumentService();

    private final ObservableList<DocumentInfo> masterData = FXCollections.observableArrayList();
    private final FilteredList<DocumentInfo> filtered = new FilteredList<>(masterData, p -> true);
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    private long loadGeneration;
    private Long pendingPreselectRecordId;

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    public javafx.beans.property.ReadOnlyBooleanProperty busyProperty() {
        return busy;
    }

    @FXML
    void initialize() {
        setupTable();
        setupRecordPicker();
        setupActions();
        loadRecords();
    }

    /** Requests loading of a specific record's documents (used by Record Details navigation). */
    public void preselectRecord(Long recordId) {
        this.pendingPreselectRecordId = recordId;
        applyPendingPreselect();
    }

    private void applyPendingPreselect() {
        if (pendingPreselectRecordId == null) {
            return;
        }
        recordPicker.getItems().stream()
                .filter(r -> Objects.equals(r.getId(), pendingPreselectRecordId))
                .findFirst()
                .ifPresent(r -> recordPicker.setValue(r));
    }

    /* ------------------------------------------------------------------ */
    /* Record picker and document list loading                             */
    /* ------------------------------------------------------------------ */

    private void setupRecordPicker() {
        recordPicker.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Record r) {
                return r == null ? "" : r.getReference() + " — " + r.getTitle();
            }

            @Override
            public Record fromString(String s) {
                return null;
            }
        });

        recordPicker.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                loadDocuments(newV.getId(), newV.getReference());
            } else {
                clearDocuments();
            }
        });
    }

    private void loadRecords() {
        FxAsync.run(
                recordService::fetchCatalog,
                paged -> {
                    AppState.getInstance().setBackendConnected(true);
                    recordPicker.getItems().setAll(paged.safeContent());
                    if (pendingPreselectRecordId != null) {
                        applyPendingPreselect();
                    }
                    if (recordPicker.getValue() == null && !recordPicker.getItems().isEmpty()) {
                        recordPicker.getSelectionModel().selectFirst();
                    } else if (recordPicker.getValue() == null) {
                        setStatus("Select a record to view its supporting documents.");
                        lblDocStatus.setText("No records available — select a record to view its documents");
                    }
                    if (recordPicker.getValue() == null && pendingPreselectRecordId != null) {
                        loadDocuments(pendingPreselectRecordId, "Record #" + pendingPreselectRecordId);
                    }
                },
                error -> {
                    if (error instanceof ApiException api) {
                        AppState.getInstance().setBackendConnected(!api.isOffline());
                    }
                    setStatus("Backend Offline. Reconnect and retry.");
                }
        );
    }

    private Long currentRecordId;

    private void loadDocuments(Long recordId, String relatedReference) {
        long generation = ++loadGeneration;
        setBusyUi("Loading documents...");
        documentsTable.getSelectionModel().clearSelection();

        FxAsync.run(
                () -> documentService.listDocuments(recordId, relatedReference != null ? relatedReference : "Record #" + recordId),
                docs -> {
                    if (generation != loadGeneration) return;
                    busy.set(false);
                    refreshBusyUi();
                    AppState.getInstance().setBackendConnected(true);
                    masterData.setAll(docs);
                    applyFilter();
                    lblDocStatus.setText(docs.size() + " document(s) loaded for " + safeReference(relatedReference));
                    setStatus(docs.size() + " document(s) loaded.");
                },
                error -> {
                    if (generation != loadGeneration) return;
                    busy.set(false);
                    refreshBusyUi();
                    handleDocumentLoadError(error);
                }
        );
    }

    private void clearDocuments() {
        masterData.clear();
        applyFilter();
        lblDocStatus.setText("No record selected — no documents shown.");
    }

    private void handleDocumentLoadError(Throwable error) {
        if (error instanceof ApiException api) {
            AppState.getInstance().setBackendConnected(!api.isOffline());
            if (api.isOffline()) {
                setTablePlaceholder("Backend Offline", "Start the Spring Boot service and use Refresh to reload documents.");
                setStatus("Backend Offline. Reconnect and retry. No success was confirmed.");
                lblDocStatus.setText("Backend offline — documents unavailable.");
                return;
            }
            if (api.getStatusCode() == 404) {
                masterData.clear();
                applyFilter();
                setTablePlaceholder("Record not found", "This record no longer exists in the repository.");
                setStatus("This record no longer exists. Return to Manage Records.");
                lblDocStatus.setText("Record not found — no documents.");
                return;
            }
        }
        setTablePlaceholder("Unable to load documents", messageOf(error));
        setStatus("Document list could not be loaded.");
        lblDocStatus.setText("Document list could not be loaded.");
    }

    private void setTablePlaceholder(String title, String body) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("table-placeholder");
        Label bodyLabel = new Label(body);
        bodyLabel.getStyleClass().add("form-hint");
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(6, titleLabel, bodyLabel);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        documentsTable.setPlaceholder(box);
    }

    /* ------------------------------------------------------------------ */
    /* Table                                                               */
    /* ------------------------------------------------------------------ */

    private void setupTable() {
        TableColumn<DocumentInfo, String> colName = new TableColumn<>("File Name");
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFileName()));
        colName.setPrefWidth(320);

        TableColumn<DocumentInfo, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFileType()));
        colType.setPrefWidth(80);

        TableColumn<DocumentInfo, String> colSize = new TableColumn<>("Size");
        colSize.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFileSize()));
        colSize.setPrefWidth(90);

        TableColumn<DocumentInfo, String> colUpdated = new TableColumn<>("Uploaded/Updated");
        colUpdated.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUpdatedDate()));
        colUpdated.setPrefWidth(110);

        TableColumn<DocumentInfo, String> colRef = new TableColumn<>("Related Record");
        colRef.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRelatedReference()));
        colRef.setPrefWidth(160);

        documentsTable.getColumns().setAll(colName, colType, colSize, colUpdated, colRef);

        SortedList<DocumentInfo> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(documentsTable.comparatorProperty());
        documentsTable.setItems(sorted);

        documentsTable.setRowFactory(tv -> {
            TableRow<DocumentInfo> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();
            MenuItem openItem = new MenuItem("Open Document");
            openItem.setOnAction(e -> handleOpenDoc(row.getItem()));
            MenuItem dlItem = new MenuItem("Download File...");
            dlItem.setOnAction(e -> handleDownloadDoc(row.getItem()));
            MenuItem replaceItem = new MenuItem("Replace File...");
            replaceItem.setOnAction(e -> handleReplaceDoc(row.getItem()));
            MenuItem removeItem = new MenuItem("Remove Document");
            removeItem.setOnAction(e -> handleRemoveDoc(row.getItem()));

            cm.getItems().addAll(openItem, dlItem, replaceItem, removeItem);
            cm.setOnShowing(e -> {
                documentsTable.getSelectionModel().select(row.getItem());
                boolean disabled = busy.get();
                openItem.setDisable(disabled);
                dlItem.setDisable(disabled);
                replaceItem.setDisable(disabled);
                removeItem.setDisable(disabled);
            });

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(cm)
            );

            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    handleOpenDoc(row.getItem());
                }
            });

            return row;
        });
    }

    /* ------------------------------------------------------------------ */
    /* Actions                                                             */
    /* ------------------------------------------------------------------ */

    private void setupActions() {
        var noSelection = documentsTable.getSelectionModel().selectedItemProperty().isNull();

        btnOpenDoc.disableProperty().bind(noSelection.or(busy));
        btnDownloadDoc.disableProperty().bind(noSelection.or(busy));
        btnReplaceDoc.disableProperty().bind(noSelection.or(busy));
        btnRemoveDoc.disableProperty().bind(noSelection.or(busy));
        btnAddDoc.disableProperty().bind(recordPicker.valueProperty().isNull().or(busy));
        btnRefreshDoc.disableProperty().bind(busy);

        btnAddDoc.setOnAction(e -> handleAddDocuments());
        btnOpenDoc.setOnAction(e -> handleOpenDoc(documentsTable.getSelectionModel().getSelectedItem()));
        btnDownloadDoc.setOnAction(e -> handleDownloadDoc(documentsTable.getSelectionModel().getSelectedItem()));
        btnReplaceDoc.setOnAction(e -> handleReplaceDoc(documentsTable.getSelectionModel().getSelectedItem()));
        btnRemoveDoc.setOnAction(e -> handleRemoveDoc(documentsTable.getSelectionModel().getSelectedItem()));
        btnRefreshDoc.setOnAction(e -> handleRefresh());

        filterField.textProperty().addListener((obs, oldV, newV) -> applyFilter());
    }

    private void handleRefresh() {
        Record record = recordPicker.getValue();
        if (record != null) {
            loadDocuments(record.getId(), record.getReference());
        } else if (pendingPreselectRecordId != null) {
            loadDocuments(pendingPreselectRecordId, "Record #" + pendingPreselectRecordId);
        } else {
            loadRecords();
        }
        if (navigator != null && navigator.mainController() != null) {
            navigator.mainController().checkBackendConnection();
        }
        setStatus("Document list refreshed.");
    }

    private void handleAddDocuments() {
        if (busy.get()) {
            return;
        }
        Record record = recordPicker.getValue();
        if (record == null) {
            showInfo("Upload Documents", "Select a record above before adding documents.");
            return;
        }

        Window owner = documentsTable.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Select PDF Documents to Upload");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        List<File> selected = fc.showOpenMultipleDialog(owner);
        if (selected == null || selected.isEmpty()) {
            return;
        }

        List<Path> files = new ArrayList<>();
        for (File file : selected) {
            String problem = validateUploadFile(file.toPath());
            if (problem == null) {
                files.add(file.toPath());
            } else {
                showError("Upload Documents", null, problem);
                return;
            }
        }

        busy.set(true);
        setBusyUi("Uploading documents...");
        setStatus("Uploading...");
        FxAsync.run(
                () -> documentService.uploadDocuments(record.getId(), record.getReference(), files),
                uploaded -> {
                    busy.set(false);
                    refreshBusyUi();
                    AppState.getInstance().setBackendConnected(true);
                    setStatus(uploaded.size() + " document(s) uploaded successfully.");
                    loadDocuments(record.getId(), record.getReference());
                },
                error -> {
                    busy.set(false);
                    refreshBusyUi();
                    showOperationError(error, "Upload");
                }
        );
    }

    private void handleOpenDoc(DocumentInfo doc) {
        if (doc == null || busy.get()) {
            return;
        }
        busy.set(true);
        setBusyUi("Opening document...");
        setStatus("Opening...");
        FxAsync.run(
                () -> documentService.preparePreviewFile(doc),
                temp -> {
                    busy.set(false);
                    refreshBusyUi();
                    AppState.getInstance().setBackendConnected(true);
                    if (!DesktopSupport.canOpenFiles()) {
                        showInfo("Open Document",
                                "No default PDF viewer is available on this system.\n"
                                        + "The document was cached to: " + temp.toAbsolutePath());
                        setStatus("Document prepared, but no viewer is available.");
                        return;
                    }
                    try {
                        DesktopSupport.openFile(temp);
                        setStatus("Document opened in system viewer.");
                    } catch (IOException e) {
                        showError("Open Document", null, "The document could not be opened by the system viewer.");
                        setStatus("Document could not be opened by the system viewer.");
                    }
                },
                error -> {
                    busy.set(false);
                    refreshBusyUi();
                    showOperationError(error, "Open");
                }
        );
    }

    private void handleDownloadDoc(DocumentInfo doc) {
        if (doc == null || busy.get()) {
            return;
        }
        Window owner = documentsTable.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Document");
        fc.setInitialFileName(documentService.defaultDownloadName(doc));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        File destination = fc.showSaveDialog(owner);
        if (destination == null) {
            return;
        }
        Path target = destination.toPath();

        if (Files.exists(target)) {
            Alert overwrite = new Alert(Alert.AlertType.CONFIRMATION);
            overwrite.initOwner(owner);
            overwrite.setTitle("Confirm Overwrite");
            overwrite.setHeaderText("Overwrite existing file?");
            overwrite.setContentText("The file already exists:\n" + target.toAbsolutePath()
                    + "\n\nDo you want to replace it?");
            overwrite.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            if (overwrite.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
        }

        busy.set(true);
        setBusyUi("Downloading document...");
        setStatus("Downloading...");
        FxAsync.run(
                () -> documentService.downloadTo(doc, target),
                saved -> {
                    busy.set(false);
                    refreshBusyUi();
                    AppState.getInstance().setBackendConnected(true);
                    setStatus("Document downloaded to " + saved.toAbsolutePath());
                },
                error -> {
                    busy.set(false);
                    refreshBusyUi();
                    showOperationError(error, "Download");
                }
        );
    }

    private void handleReplaceDoc(DocumentInfo doc) {
        if (doc == null || busy.get()) {
            return;
        }
        Record record = recordPicker.getValue();
        Window owner = documentsTable.getScene().getWindow();

        FileChooser fc = new FileChooser();
        fc.setTitle("Select Replacement PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
        File newFile = fc.showOpenDialog(owner);
        if (newFile == null) {
            return;
        }
        String problem = validateUploadFile(newFile.toPath());
        if (problem != null) {
            showError("Replace Document", null, problem);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(owner);
        confirm.setTitle("Replace Document");
        confirm.setHeaderText("Replace Document");
        confirm.setContentText("Existing:\n" + doc.getFileName()
                + "\n\nNew:\n" + newFile.getName()
                + "\n\nOnly this document will be replaced. Sibling documents are not affected.");
        ButtonType replace = new ButtonType("Replace", ButtonBar.ButtonData.OK_DONE);
        confirm.getButtonTypes().setAll(replace, ButtonType.CANCEL);
        Button replaceButton = (Button) confirm.getDialogPane().lookupButton(replace);
        replaceButton.setDefaultButton(false);
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);
        confirm.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/heirs/desktop/css/base.css").toExternalForm());
        replaceButton.getStyleClass().add("button-desktop-primary");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != replace) {
            return;
        }

        busy.set(true);
        setBusyUi("Replacing document...");
        setStatus("Replacing...");
        FxAsync.run(
                () -> documentService.replaceDocument(doc, record != null ? record.getReference() : doc.getRelatedReference(), newFile.toPath()),
                replaced -> {
                    busy.set(false);
                    refreshBusyUi();
                    AppState.getInstance().setBackendConnected(true);
                    setStatus("Document replaced successfully.");
                    if (record != null) {
                        loadDocuments(record.getId(), record.getReference());
                    } else {
                        loadDocuments(doc.getRecordId(), doc.getRelatedReference());
                    }
                    documentsTable.getSelectionModel().clearSelection();
                },
                error -> {
                    busy.set(false);
                    refreshBusyUi();
                    showOperationError(error, "Replace");
                }
        );
    }

    private void handleRemoveDoc(DocumentInfo doc) {
        if (doc == null || busy.get()) {
            return;
        }
        Record record = recordPicker.getValue();
        Window owner = documentsTable.getScene().getWindow();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(owner);
        confirm.setTitle("Remove Document");
        confirm.setHeaderText("Remove Document");
        confirm.setContentText("Are you sure you want to remove:\n\n" + doc.getFileName()
                + "\n\nThis removes the document from the repository and its stored file.");
        ButtonType remove = new ButtonType("Remove", ButtonBar.ButtonData.OK_DONE);
        confirm.getButtonTypes().setAll(remove, ButtonType.CANCEL);
        Button removeButton = (Button) confirm.getDialogPane().lookupButton(remove);
        removeButton.setDefaultButton(false);
        removeButton.getStyleClass().add("button-desktop-danger");
        ((Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL)).setDefaultButton(true);

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != remove) {
            return;
        }

        busy.set(true);
        setBusyUi("Removing document...");
        setStatus("Removing...");
        FxAsync.run(
                () -> {
                    documentService.deleteDocument(doc);
                    return null;
                },
                ignored -> {
                    busy.set(false);
                    refreshBusyUi();
                    AppState.getInstance().setBackendConnected(true);
                    documentsTable.getSelectionModel().clearSelection();
                    setStatus("Document removed successfully.");
                    if (record != null) {
                        loadDocuments(record.getId(), record.getReference());
                    } else {
                        loadDocuments(doc.getRecordId(), doc.getRelatedReference());
                    }
                },
                error -> {
                    busy.set(false);
                    refreshBusyUi();
                    showOperationError(error, "Remove");
                }
        );
    }

    /* ------------------------------------------------------------------ */
    /* Validation and errors                                               */
    /* ------------------------------------------------------------------ */

    private String validateUploadFile(Path file) {
        String name = file.getFileName() != null ? file.getFileName().toString() : "file";
        if (!Files.exists(file)) {
            return "File does not exist:\n" + name;
        }
        if (!Files.isReadable(file)) {
            return "File is not readable:\n" + name;
        }
        try {
            long size = Files.size(file);
            if (size == 0) {
                return "File is empty (0 bytes):\n" + name;
            }
            if (size > MAX_FILE_BYTES) {
                return "File is too large (" + FileNames.formatSize(size) + "). Per-file limit is 20 MiB:\n" + name;
            }
        } catch (IOException e) {
            return "Unable to read file:\n" + name;
        }
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            return name + ":\nonly PDF documents (application/pdf) are supported.";
        }
        try (InputStream in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(5);
            if (head.length < 5 || head[0] != '%' || head[1] != 'P' || head[2] != 'D' || head[3] != 'F' || head[4] != '-') {
                return name + ":\ncontent is not a PDF document.";
            }
        } catch (IOException e) {
            return "Unable to read file:\n" + name;
        }
        return null;
    }

    private void showOperationError(Throwable error, String operation) {
        String message = "The " + operation.toLowerCase(java.util.Locale.ROOT) + " operation failed. Please retry.";
        if (error instanceof ApiException api) {
            AppState.getInstance().setBackendConnected(!api.isOffline());
            if (api.isOffline()) {
                message = "Backend Offline. Reconnect and retry. No success was confirmed.";
            } else if (api.getStatusCode() == 404) {
                message = "This document no longer exists. The list will be refreshed.";
                Record record = recordPicker.getValue();
                if (record != null) {
                    loadDocuments(record.getId(), record.getReference());
                } else if (pendingPreselectRecordId != null) {
                    loadDocuments(pendingPreselectRecordId, "Record #" + pendingPreselectRecordId);
                } else {
                    loadRecords();
                }
                setStatus(message);
                showError(operation, null, message);
                return;
            } else if (api.getStatusCode() == 413) {
                message = "File is too large. Per-file limit is 20 MiB.";
            } else {
                message = api.getUserMessage();
            }
        }
        setStatus(message);
        showError(operation, null, message);
    }

    private void showError(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message != null ? message : "An error occurred.");
        Window owner = documentsTable.getScene() != null ? documentsTable.getScene().getWindow() : null;
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title != null ? title : "Document Operation");
        alert.setHeaderText(header);
        alert.show();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        Window owner = documentsTable.getScene() != null ? documentsTable.getScene().getWindow() : null;
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.show();
    }

    private String messageOf(Throwable error) {
        if (error instanceof ApiException api) {
            return api.getUserMessage();
        }
        String msg = error != null ? error.getMessage() : "Unknown error";
        return msg != null ? msg : "Unknown error";
    }

    private static String safeReference(String reference) {
        return reference == null || reference.isBlank() ? "selected record" : reference;
    }

    /* ------------------------------------------------------------------ */
    /* Filtering and status                                                */
    /* ------------------------------------------------------------------ */

    private void applyFilter() {
        String query = filterField.getText() == null ? "" : filterField.getText().trim().toLowerCase();
        filtered.setPredicate(d -> {
            if (query.isBlank()) {
                return true;
            }
            return d.getFileName().toLowerCase().contains(query)
                    || d.getRelatedReference().toLowerCase().contains(query)
                    || d.getFileType().toLowerCase().contains(query);
        });
        lblDocStatus.setText("Showing " + filtered.size() + " of " + masterData.size() + " document(s)");
    }

    private void setBusyUi(String message) {
        busy.set(true);
        lblDocStatus.setText(message);
    }

    private void refreshBusyUi() {
        applyFilter();
    }

    private void setStatus(String message) {
        if (navigator != null && navigator.mainController() != null) {
            navigator.mainController().setStatusMessage(message);
        }
    }
}