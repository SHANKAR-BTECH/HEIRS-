package com.heirs.desktop.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;


import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.ViewType;
import com.heirs.desktop.util.UiBuilders;

/**
 * Controller for the primary desktop administration shell.
 * Coordinates MenuBar, ToolBar, Left TreeView Navigation, Center Workspace, and StatusBar.
 */
public class MainController {

    @FXML private MenuBar menuBar;
    @FXML private MenuItem menuFileNew;
    @FXML private MenuItem menuFileRefresh;
    @FXML private MenuItem menuFileExit;

    @FXML private MenuItem menuRecordsView;
    @FXML private MenuItem menuRecordsAdd;
    @FXML private MenuItem menuRecordsEdit;
    @FXML private MenuItem menuRecordsDelete;

    @FXML private MenuItem menuSearchRecords;
    @FXML private MenuItem menuSearchClear;

    @FXML private MenuItem menuReportsSummary;
    @FXML private MenuItem menuReportsCategory;
    @FXML private MenuItem menuReportsStatus;

    @FXML private MenuItem menuToolsSettings;
    @FXML private MenuItem menuToolsStatus;

    @FXML private MenuItem menuHelpAbout;

    @FXML private ToolBar toolBar;
    @FXML private Button toolNew;
    @FXML private Button toolEdit;
    @FXML private Button toolDelete;
    @FXML private Button toolRefresh;
    @FXML private Button toolSearch;
    @FXML private Button toolView;
    @FXML private Button toolReports;
    @FXML private Button toolDocuments;
    @FXML private Button toolAbout;

    @FXML private TreeView<String> navTree;
    @FXML private StackPane contentArea;

    @FXML private Region statusDot;
    @FXML private Label statusMessage;
    @FXML private Label backendStatusLabel;
    @FXML private Label recordCountLabel;
    @FXML private Label uiModeLabel;
    @FXML private Label systemInfoLabel;

    private Navigator navigator;
    private RecordActions recordActions;
    private boolean updatingTreeSelection = false;
    private final com.heirs.desktop.service.RecordService recordService = new com.heirs.desktop.service.RecordService();

    // References to tree items for sync
    private TreeItem<String> itemDashboard;
    private TreeItem<String> itemRecords;
    private TreeItem<String> itemAllRecords;
    private TreeItem<String> itemPolicies;
    private TreeItem<String> itemSchemes;
    private TreeItem<String> itemRegulations;
    private TreeItem<String> itemProjects;
    private TreeItem<String> itemRules;
    private TreeItem<String> itemSearch;
    private TreeItem<String> itemDocuments;
    private TreeItem<String> itemReports;
    private TreeItem<String> itemSystem;

    @FXML
    void initialize() {
        navigator = new Navigator(contentArea, this);
        recordActions = new RecordActions(navigator, this::getWindow, this::setStatusMessage);

        setupToolBar();
        setupMenuBar();
        setupNavigationTree();
        setupStatusBar();

        Platform.runLater(() -> {
            checkBackendConnection();
            navigator.navigateTo(ViewType.DASHBOARD);
        });
    }

    private void setupToolBar() {
        toolNew.setGraphic(UiBuilders.icon(FontAwesomeSolid.PLUS, 12));
        toolEdit.setGraphic(UiBuilders.icon(FontAwesomeSolid.EDIT, 12));
        toolDelete.setGraphic(UiBuilders.icon(FontAwesomeSolid.TRASH_ALT, 12));
        toolRefresh.setGraphic(UiBuilders.icon(FontAwesomeSolid.SYNC_ALT, 12));
        toolSearch.setGraphic(UiBuilders.icon(FontAwesomeSolid.SEARCH, 12));
        toolView.setGraphic(UiBuilders.icon(FontAwesomeSolid.FOLDER_OPEN, 12));
        toolReports.setGraphic(UiBuilders.icon(FontAwesomeSolid.CHART_BAR, 12));
        toolDocuments.setGraphic(UiBuilders.icon(FontAwesomeSolid.FILE_ALT, 12));
        toolAbout.setGraphic(UiBuilders.icon(FontAwesomeSolid.INFO_CIRCLE, 12));

        toolNew.setOnAction(e -> openAddDialog());
        toolEdit.setOnAction(e -> recordActions.edit(selectedRecord()));
        toolDelete.setOnAction(e -> recordActions.delete(selectedRecord()));
        toolNew.disableProperty().bind(recordActions.busyProperty());
        toolRefresh.setOnAction(e -> handleRefresh());
        toolSearch.setOnAction(e -> navigator.navigateTo(ViewType.SEARCH));
        toolView.setOnAction(e -> recordActions.view(selectedRecord()));
        toolReports.setOnAction(e -> navigator.navigateTo(ViewType.REPORTS));
        toolDocuments.setOnAction(e -> navigator.navigateTo(ViewType.DOCUMENTS));
        toolAbout.setOnAction(e -> navigator.navigateTo(ViewType.ABOUT));
    }

    private void setupMenuBar() {
        menuFileNew.setOnAction(e -> openAddDialog());
        menuFileRefresh.setOnAction(e -> handleRefresh());
        menuFileExit.setOnAction(e -> Platform.exit());

        menuRecordsView.setOnAction(e -> navigator.navigateTo(ViewType.RECORDS));
        menuRecordsAdd.setOnAction(e -> openAddDialog());
        menuRecordsEdit.setOnAction(e -> recordActions.edit(selectedRecord()));
        menuRecordsDelete.setOnAction(e -> recordActions.delete(selectedRecord()));
        menuRecordsAdd.disableProperty().bind(recordActions.busyProperty());
        menuFileNew.disableProperty().bind(recordActions.busyProperty());

        menuSearchRecords.setOnAction(e -> navigator.navigateTo(ViewType.SEARCH));
        menuSearchClear.setOnAction(e -> navigator.navigateTo(ViewType.SEARCH));

        menuReportsSummary.setOnAction(e -> navigator.navigateTo(ViewType.REPORTS));
        menuReportsCategory.setOnAction(e -> navigator.navigateTo(ViewType.REPORTS));
        menuReportsStatus.setOnAction(e -> navigator.navigateTo(ViewType.REPORTS));

        menuToolsSettings.setOnAction(e -> UiBuilders.placeholderAlert(getWindow(),
                "Client settings and configuration will be connected in Phase J2."));
        menuToolsStatus.setOnAction(e -> navigator.navigateTo(ViewType.ABOUT));

        menuHelpAbout.setOnAction(e -> navigator.navigateTo(ViewType.ABOUT));
    }

    private void setupNavigationTree() {
        TreeItem<String> root = new TreeItem<>("HEIRS", UiBuilders.icon(FontAwesomeSolid.GRADUATION_CAP, 13));
        root.setExpanded(true);

        itemDashboard = new TreeItem<>("Dashboard", UiBuilders.icon(FontAwesomeSolid.TACHOMETER_ALT, 12));

        itemRecords = new TreeItem<>("Records", UiBuilders.icon(FontAwesomeSolid.FOLDER, 12));
        itemRecords.setExpanded(true);

        itemAllRecords = new TreeItem<>("All Records", UiBuilders.icon(FontAwesomeSolid.LIST, 12));
        itemPolicies = new TreeItem<>("Policies", UiBuilders.icon(FontAwesomeSolid.BOOK, 12));
        itemSchemes = new TreeItem<>("Schemes", UiBuilders.icon(FontAwesomeSolid.GRADUATION_CAP, 12));
        itemRegulations = new TreeItem<>("Regulations", UiBuilders.icon(FontAwesomeSolid.CHECK_SQUARE, 12));
        itemProjects = new TreeItem<>("Projects", UiBuilders.icon(FontAwesomeSolid.LAYER_GROUP, 12));
        itemRules = new TreeItem<>("Rules", UiBuilders.icon(FontAwesomeSolid.CLIPBOARD, 12));
        itemRecords.getChildren().addAll(itemAllRecords, itemPolicies, itemSchemes, itemRegulations, itemProjects, itemRules);

        itemSearch = new TreeItem<>("Search", UiBuilders.icon(FontAwesomeSolid.SEARCH, 12));
        itemDocuments = new TreeItem<>("Documents", UiBuilders.icon(FontAwesomeSolid.FILE_ALT, 12));
        itemReports = new TreeItem<>("Reports", UiBuilders.icon(FontAwesomeSolid.CHART_BAR, 12));
        itemSystem = new TreeItem<>("System", UiBuilders.icon(FontAwesomeSolid.INFO_CIRCLE, 12));

        root.getChildren().addAll(itemDashboard, itemRecords, itemSearch, itemDocuments, itemReports, itemSystem);

        navTree.setRoot(root);
        navTree.setShowRoot(true);

        navTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (updatingTreeSelection || newVal == null) return;

            String value = newVal.getValue();
            if ("Dashboard".equals(value) || "HEIRS".equals(value)) {
                navigator.navigateTo(ViewType.DASHBOARD);
            } else if ("Records".equals(value) || "All Records".equals(value)) {
                navigator.navigateTo(ViewType.RECORDS);
                if (navigator.currentController() instanceof RecordsController rc) {
                    rc.filterByCategory("All Records");
                }
            } else if ("Policies".equals(value)) {
                navigator.navigateTo(ViewType.RECORDS);
                if (navigator.currentController() instanceof RecordsController rc) {
                    rc.filterByCategory("Policy");
                }
            } else if ("Schemes".equals(value)) {
                navigator.navigateTo(ViewType.RECORDS);
                if (navigator.currentController() instanceof RecordsController rc) {
                    rc.filterByCategory("Scheme");
                }
            } else if ("Regulations".equals(value)) {
                navigator.navigateTo(ViewType.RECORDS);
                if (navigator.currentController() instanceof RecordsController rc) {
                    rc.filterByCategory("Regulation");
                }
            } else if ("Projects".equals(value)) {
                navigator.navigateTo(ViewType.RECORDS);
                if (navigator.currentController() instanceof RecordsController rc) {
                    rc.filterByCategory("Project");
                }
            } else if ("Rules".equals(value)) {
                navigator.navigateTo(ViewType.RECORDS);
                if (navigator.currentController() instanceof RecordsController rc) {
                    rc.filterByCategory("Rules");
                }
            } else if ("Search".equals(value)) {
                navigator.navigateTo(ViewType.SEARCH);
            } else if ("Documents".equals(value)) {
                navigator.navigateTo(ViewType.DOCUMENTS);
            } else if ("Reports".equals(value)) {
                navigator.navigateTo(ViewType.REPORTS);
            } else if ("System".equals(value)) {
                navigator.navigateTo(ViewType.ABOUT);
            }
        });
    }

    private void setupStatusBar() {
        com.heirs.desktop.state.AppState state = com.heirs.desktop.state.AppState.getInstance();

        backendStatusLabel.textProperty().bind(state.backendStatusMessageProperty());
        recordCountLabel.textProperty().bind(state.totalRecordsCountProperty());

        state.backendConnectedProperty().addListener((obs, oldVal, connected) -> {
            statusDot.getStyleClass().removeAll("statusbar-dot-ready", "statusbar-dot-offline");
            if (connected) {
                statusDot.getStyleClass().add("statusbar-dot-ready");
                setStatusMessage("Ready");
            } else {
                statusDot.getStyleClass().add("statusbar-dot-offline");
                setStatusMessage("Unable to connect to backend");
            }
        });

        uiModeLabel.setText("REST Mode");
        systemInfoLabel.setText("HEIRS Desktop · Phase J3");
    }

    public void checkBackendConnection() {
        com.heirs.desktop.state.AppState state = com.heirs.desktop.state.AppState.getInstance();
        state.setBackendStatusMessage("Backend: Checking...");

        com.heirs.desktop.util.FxAsync.run(
                recordService::checkHealth,
                connected -> {
                    state.setBackendConnected(connected);
                },
                err -> {
                    state.setBackendConnected(false);
                }
        );
    }

    public RecordActions recordActions() { return recordActions; }
    private com.heirs.desktop.model.Record selectedRecord() {
        return navigator.currentController() instanceof RecordsController rc ? rc.selectedRecord() : null;
    }
    public void setActiveView(ViewType type) {
        toolEdit.disableProperty().unbind();
        var disabled = navigator.currentController() instanceof RecordsController rc
                ? rc.selectionProperty().isNull().or(recordActions.busyProperty())
                : new javafx.beans.property.SimpleBooleanProperty(true);
        toolEdit.disableProperty().bind(disabled);
        toolDelete.disableProperty().bind(disabled);
        toolView.disableProperty().bind(disabled);
        menuRecordsEdit.disableProperty().bind(disabled);
        menuRecordsDelete.disableProperty().bind(disabled);
        updatingTreeSelection = true;
        try {
            switch (type) {
                case DASHBOARD -> navTree.getSelectionModel().select(itemDashboard);
                case RECORDS -> {
                    TreeItem<String> sel = navTree.getSelectionModel().getSelectedItem();
                    if (sel == null || !itemRecords.getChildren().contains(sel)) {
                        navTree.getSelectionModel().select(itemAllRecords);
                    }
                }
                case SEARCH -> navTree.getSelectionModel().select(itemSearch);
                case DOCUMENTS -> navTree.getSelectionModel().select(itemDocuments);
                case REPORTS -> navTree.getSelectionModel().select(itemReports);
                case ABOUT -> navTree.getSelectionModel().select(itemSystem);
            }
        } finally {
            updatingTreeSelection = false;
        }
    }

    public void setStatusMessage(String message) {
        statusMessage.setText(message);
    }

    private void handleRefresh() {
        checkBackendConnection();
        ViewType current = navigator.currentView();
        if (current != null) {
            navigator.navigateTo(current);
            setStatusMessage("Refreshed " + current.title());
        }
    }

    private void openAddDialog() {
        recordActions.create();
    }

    private Window getWindow() {
        return contentArea.getScene() != null ? contentArea.getScene().getWindow() : null;
    }

    public Navigator getNavigator() {
        return navigator;
    }
}