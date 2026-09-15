package com.heirs.desktop.controller;

import java.util.function.Predicate;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import com.heirs.desktop.model.RecordOptions;
import com.heirs.desktop.model.Record;
import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.NavigatorAware;
import com.heirs.desktop.service.RecordService;
import com.heirs.desktop.state.AppState;
import com.heirs.desktop.util.FxAsync;

public class RecordsController implements NavigatorAware {

    @FXML private TextField searchField;
    @FXML private Button btnFind;
    @FXML private Button btnClearFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> statusFilter;

    @FXML private Button btnNewRecord;
    @FXML private Button btnEditRecord;
    @FXML private Button btnDeleteRecord;
    @FXML private Button btnViewRecord;
    @FXML private Button btnRefreshRecords;

    @FXML private TableView<Record> tableView;
    @FXML private Label lblFilterStatus;

    private Navigator navigator;
    private final RecordService recordService = new RecordService();
    private final ObservableList<Record> masterData = FXCollections.observableArrayList();
    private final FilteredList<Record> filtered = new FilteredList<>(masterData, p -> true);

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
        var busy = actions().busyProperty();
        btnNewRecord.disableProperty().bind(busy);
        btnEditRecord.disableProperty().bind(tableView.getSelectionModel().selectedItemProperty().isNull().or(busy));
        btnDeleteRecord.disableProperty().bind(btnEditRecord.disableProperty());
        btnViewRecord.disableProperty().bind(btnEditRecord.disableProperty());
    }

    @FXML
    void initialize() {
        setupTableColumns();
        setupFilters();
        setupActions();
        loadRecords();
        loadCategories();
    }

    private long loadGeneration;
    public Record selectedRecord() { return tableView.getSelectionModel().getSelectedItem(); }
    public javafx.beans.property.ReadOnlyObjectProperty<Record> selectionProperty() {
        return tableView.getSelectionModel().selectedItemProperty();
    }
    private RecordActions actions() { return navigator.mainController().recordActions(); }
    public void refreshAfterWrite(Long id, boolean created) {
        tableView.getSelectionModel().clearSelection();
        if (created) {
            searchField.clear();
            categoryFilter.getSelectionModel().selectFirst();
            statusFilter.getSelectionModel().selectFirst();
        }
        loadRecords(id);
    }
    public void loadRecords() { loadRecords(null); }
    private void loadRecords(Long selectId) {
        long generation = ++loadGeneration;
        showLoadingState();

        FxAsync.run(
                recordService::fetchCatalog,
                paged -> {
                    if (generation != loadGeneration) return;
                    AppState.getInstance().setBackendConnected(true);
                    AppState.getInstance().setTotalRecordsCount(paged.totalElements());
                    masterData.setAll(paged.safeContent());
                    applyFilter();
                    masterData.stream().filter(r -> r.getId().equals(selectId)).findFirst().ifPresent(r -> {
                        tableView.getSelectionModel().select(r);
                        tableView.scrollTo(r);
                    });
                    if (paged.safeContent().isEmpty()) {
                        showEmptyState();
                    }
                },
                error -> {
                    if (generation != loadGeneration) return;
                    if (error instanceof com.heirs.desktop.api.ApiException api)
                        AppState.getInstance().setBackendConnected(!api.isOffline());
                    showErrorState(error);
                }
        );
    }

    private void loadCategories() {
        FxAsync.run(
                recordService::fetchCategoryNames,
                categories -> {
                    String current = categoryFilter.getValue();
                    categoryFilter.getItems().setAll("All Categories");
                    categoryFilter.getItems().addAll(categories);
                    if (current != null && categoryFilter.getItems().contains(current)) {
                        categoryFilter.setValue(current);
                    } else {
                        categoryFilter.getSelectionModel().selectFirst();
                    }
                },
                err -> {
                    // retain defaults if category call fails
                }
        );
    }

    private void showLoadingState() {
        VBox loadingBox = new VBox(8);
        loadingBox.setAlignment(Pos.CENTER);
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(24, 24);
        Label lblLoading = new Label("Loading records from backend...");
        lblLoading.getStyleClass().add("table-placeholder");
        loadingBox.getChildren().addAll(progress, lblLoading);
        tableView.setPlaceholder(loadingBox);
    }

    private void showEmptyState() {
        Label emptyLabel = new Label("No records found in backend repository.");
        emptyLabel.getStyleClass().add("table-placeholder");
        tableView.setPlaceholder(emptyLabel);
    }

    private void showErrorState(Throwable error) {
        // Retain confirmed rows when a refresh fails; never imply deletion.
        VBox errorBox = new VBox(8);
        errorBox.setAlignment(Pos.CENTER);
        Label errLabel = new Label("Unable to connect to the HEIRS backend.");
        errLabel.getStyleClass().add("table-placeholder");
        Label hintLabel = new Label("Start the Spring Boot server and click Retry.");
        hintLabel.getStyleClass().add("form-hint");
        Button retryBtn = new Button("Retry Connection");
        retryBtn.getStyleClass().addAll("button-desktop-secondary");
        retryBtn.setOnAction(e -> {
            if (navigator != null && navigator.mainController() != null) {
                navigator.mainController().checkBackendConnection();
            }
            loadRecords();
            loadCategories();
        });
        errorBox.getChildren().addAll(errLabel, hintLabel, retryBtn);
        tableView.setPlaceholder(errorBox);
        lblFilterStatus.setText("Backend offline — 0 records loaded.");
    }

    private void setupTableColumns() {
        TableColumn<Record, String> colRef = new TableColumn<>("Reference");
        colRef.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getReference()));
        colRef.setPrefWidth(140);
        colRef.setMinWidth(120);

        TableColumn<Record, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitle()));
        colTitle.setPrefWidth(340);

        TableColumn<Record, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCategory()));
        colCat.setPrefWidth(120);

        TableColumn<Record, String> colDept = new TableColumn<>("Department");
        colDept.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getDepartment()));
        colDept.setPrefWidth(180);

        TableColumn<Record, String> colYear = new TableColumn<>("Year");
        colYear.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getYear() != null && cd.getValue().getYear() > 0 ? String.valueOf(cd.getValue().getYear()) : "—"));
        colYear.setPrefWidth(70);

        TableColumn<Record, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        colStatus.setPrefWidth(110);

        tableView.getColumns().setAll(colRef, colTitle, colCat, colDept, colYear, colStatus);

        SortedList<Record> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sorted);

        // Row factory for double-click and context menu
        tableView.setRowFactory(tv -> {
            TableRow<Record> row = new TableRow<>();

            ContextMenu contextMenu = new ContextMenu();
            MenuItem itemView = new MenuItem("View Record Details...");
            itemView.setOnAction(e -> actions().view(row.getItem()));
            MenuItem itemEdit = new MenuItem("Edit Record...");
            itemEdit.setOnAction(e -> actions().edit(row.getItem()));
            MenuItem itemDelete = new MenuItem("Delete Record");
            itemDelete.setOnAction(e -> actions().delete(row.getItem()));
            contextMenu.setOnShowing(e -> {
                tableView.getSelectionModel().select(row.getItem());
                boolean disabled = actions().busyProperty().get();
                itemView.setDisable(disabled);
                itemEdit.setDisable(disabled);
                itemDelete.setDisable(disabled);
            });

            contextMenu.getItems().addAll(itemView, itemEdit, itemDelete);

            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(contextMenu)
            );

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    actions().view(row.getItem());
                }
            });

            return row;
        });
    }

    private void setupFilters() {
        categoryFilter.getItems().setAll("All Categories");
        categoryFilter.getItems().addAll(RecordOptions.CATEGORIES);
        categoryFilter.getSelectionModel().selectFirst();

        statusFilter.getItems().setAll("All Statuses");
        statusFilter.getItems().addAll(RecordOptions.STATUSES);
        statusFilter.getSelectionModel().selectFirst();

        searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        categoryFilter.valueProperty().addListener((o, ov, nv) -> applyFilter());
        statusFilter.valueProperty().addListener((o, ov, nv) -> applyFilter());

        btnFind.setOnAction(e -> applyFilter());
        btnClearFilter.setOnAction(e -> {
            searchField.clear();
            categoryFilter.getSelectionModel().selectFirst();
            statusFilter.getSelectionModel().selectFirst();
        });
    }

    private void setupActions() {
        btnNewRecord.setOnAction(e -> actions().create());
        btnEditRecord.setOnAction(e -> actions().edit(selectedRecord()));
        btnDeleteRecord.setOnAction(e -> actions().delete(selectedRecord()));
        btnViewRecord.setOnAction(e -> actions().view(selectedRecord()));

        btnRefreshRecords.setOnAction(e -> {
            loadRecords();
            loadCategories();
            if (navigator != null && navigator.mainController() != null) {
                navigator.mainController().setStatusMessage("Refreshed records from backend");
            }
        });
    }

    public void filterByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All Records")) {
            categoryFilter.getSelectionModel().select("All Categories");
        } else {
            categoryFilter.getSelectionModel().select(category);
        }
    }

    private void applyFilter() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String cat = categoryFilter.getValue();
        String status = statusFilter.getValue();

        Predicate<Record> predicate = r -> {
            if (cat != null && !"All Categories".equals(cat) && !r.getCategory().equalsIgnoreCase(cat)) return false;
            if (status != null && !"All Statuses".equals(status) && !r.getStatus().equalsIgnoreCase(status)) return false;
            if (!query.isBlank()) {
                String haystack = (r.getTitle() + " " + r.getReference() + " " + r.getDepartment() + " " + r.getKeywords()).toLowerCase();
                if (!haystack.contains(query)) return false;
            }
            return true;
        };

        filtered.setPredicate(predicate);

        int count = filtered.size();
        int total = masterData.size();
        lblFilterStatus.setText("Showing " + count + " of " + total + " records"
                + (!"All Categories".equals(cat) && cat != null ? " [Category: " + cat + "]" : "")
                + (!"All Statuses".equals(status) && status != null ? " [Status: " + status + "]" : ""));
    }

}
