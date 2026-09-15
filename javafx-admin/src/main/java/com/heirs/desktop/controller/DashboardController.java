package com.heirs.desktop.controller;
 
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import com.heirs.desktop.model.Record;
import com.heirs.desktop.model.RecordDetailsDialog;
import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.NavigatorAware;
import com.heirs.desktop.navigation.ViewType;
import com.heirs.desktop.service.DashboardService;
import com.heirs.desktop.state.AppState;
import com.heirs.desktop.util.FxAsync;

public class DashboardController implements NavigatorAware {

    @FXML private Label lblTotalRecords;
    @FXML private Label lblPolicies;
    @FXML private Label lblSchemes;
    @FXML private Label lblRegulations;
    @FXML private Label lblProjects;
    @FXML private Label lblRules;

    @FXML private Label lblBackendService;
    @FXML private Label lblCatalogDataMode;

    @FXML private Button btnManageAllRecords;
    @FXML private Button btnViewSelected;

    @FXML private TableView<Record> recentTable;
    @FXML private Label lblRecentNote;

    private Navigator navigator;
    private final DashboardService dashboardService = new DashboardService();

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    void initialize() {
        setupRecentTable();
        loadDashboard();

        btnManageAllRecords.setOnAction(e -> {
            if (navigator != null) {
                navigator.navigateTo(ViewType.RECORDS);
            }
        });

        btnViewSelected.setOnAction(e -> {
            Record selected = recentTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Window owner = recentTable.getScene().getWindow();
                RecordDetailsDialog.show(owner, selected.getId(), navigator.mainController().recordActions()::edit, navigator);
            }
        });
    }

    private long loadGeneration;
    public void loadDashboard() {
        long generation = ++loadGeneration;
        showLoadingState();

        FxAsync.run(
                dashboardService::loadDashboardData,
                data -> {
                    if (generation != loadGeneration) return;
                    AppState.getInstance().setBackendConnected(true);
                    AppState.getInstance().setTotalRecordsCount(data.totalRecords());
                    updateSummaryCounts(data);
                    recentTable.setItems(FXCollections.observableArrayList(data.recentRecords()));
                    if (data.recentRecords().isEmpty()) {
                        showEmptyState();
                    }
                    if (lblBackendService != null) {
                        lblBackendService.setText("Spring Boot REST API (Connected)");
                    }
                    if (lblCatalogDataMode != null) {
                        lblCatalogDataMode.setText("Live REST / MySQL");
                    }
                },
                error -> {
                    if (generation != loadGeneration) return;
                    AppState.getInstance().setBackendConnected(false);
                    showErrorState(error);
                    if (lblBackendService != null) {
                        lblBackendService.setText("Offline — " + error.getMessage());
                    }
                    if (lblCatalogDataMode != null) {
                        lblCatalogDataMode.setText("Offline");
                    }
                }
        );
    }

    private void updateSummaryCounts(DashboardService.DashboardData data) {
        lblTotalRecords.setText(String.valueOf(data.totalRecords()));
        lblPolicies.setText(String.valueOf(data.policies()));
        lblSchemes.setText(String.valueOf(data.schemes()));
        lblRegulations.setText(String.valueOf(data.regulations()));
        lblProjects.setText(String.valueOf(data.projects()));
        lblRules.setText(String.valueOf(data.rules()));
    }

    private void showLoadingState() {
        VBox loadingBox = new VBox(8);
        loadingBox.setAlignment(Pos.CENTER);
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(24, 24);
        Label lblLoading = new Label("Loading dashboard data from backend...");
        lblLoading.getStyleClass().add("table-placeholder");
        loadingBox.getChildren().addAll(progress, lblLoading);
        recentTable.setPlaceholder(loadingBox);
    }

    private void showEmptyState() {
        Label emptyLabel = new Label("No records found in backend repository.");
        emptyLabel.getStyleClass().add("table-placeholder");
        recentTable.setPlaceholder(emptyLabel);
    }

    private void showErrorState(Throwable error) {
        lblTotalRecords.setText("—");
        lblPolicies.setText("—");
        lblSchemes.setText("—");
        lblRegulations.setText("—");
        lblProjects.setText("—");
        lblRules.setText("—");
        recentTable.setItems(FXCollections.emptyObservableList());

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
            loadDashboard();
        });
        errorBox.getChildren().addAll(errLabel, hintLabel, retryBtn);
        recentTable.setPlaceholder(errorBox);
    }

    private void setupRecentTable() {
        TableColumn<Record, String> colRef = new TableColumn<>("Reference");
        colRef.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getReference()));
        colRef.setPrefWidth(140);
        colRef.setMinWidth(120);

        TableColumn<Record, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitle()));
        colTitle.setPrefWidth(320);

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

        recentTable.getColumns().setAll(colRef, colTitle, colCat, colDept, colYear, colStatus);

        recentTable.setRowFactory(tv -> {
            TableRow<Record> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Record rowData = row.getItem();
                    RecordDetailsDialog.show(recentTable.getScene().getWindow(), rowData.getId(), navigator.mainController().recordActions()::edit, navigator);
                }
            });
            return row;
        });
    }
}