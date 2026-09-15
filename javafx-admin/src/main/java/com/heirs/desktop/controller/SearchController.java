package com.heirs.desktop.controller;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import com.heirs.desktop.model.Record;
import com.heirs.desktop.model.RecordDetailsDialog;
import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.NavigatorAware;
import com.heirs.desktop.service.RecordService;
import com.heirs.desktop.state.AppState;
import com.heirs.desktop.util.FxAsync;
import com.heirs.desktop.util.UiBuilders;

public class SearchController implements NavigatorAware {

    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> yearFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> departmentFilter;
    @FXML private ComboBox<String> sortByFilter;
    @FXML private ComboBox<String> sortDirectionFilter;
    @FXML private Button clearFilters;

    @FXML private TableView<Record> resultsTable;
    @FXML private Label resultCount;
    @FXML private Label pageInfoLabel;
    @FXML private ComboBox<String> pageSizeFilter;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Button btnViewDetails;

    private Navigator navigator;
    private final RecordService recordService = new RecordService();
    private final ObservableList<Record> searchResults = FXCollections.observableArrayList();

    private int currentPage;
    private int currentPageSize = 100;

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    void initialize() {
        setupTable();
        setupFilters();
        restoreSearchState();

        searchButton.setOnAction(e -> executeSearch());
        searchField.setOnAction(e -> executeSearch());

        clearFilters.setOnAction(e -> {
            searchField.clear();
            categoryFilter.getSelectionModel().selectFirst();
            yearFilter.getSelectionModel().selectFirst();
            statusFilter.getSelectionModel().selectFirst();
            departmentFilter.getSelectionModel().selectFirst();
            sortByFilter.getSelectionModel().selectFirst();
            sortDirectionFilter.getSelectionModel().selectFirst();
            currentPage = 0;
            executeSearch();
        });

        pageSizeFilter.valueProperty().addListener((obs, oldV, newV) -> {
            int parsed = newV == null ? 100 : Integer.parseInt(newV);
            if (parsed != currentPageSize) {
                currentPageSize = parsed;
                currentPage = 0;
                executeSearch();
            }
        });

        prevPageButton.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                executeSearch();
            }
        });

        nextPageButton.setOnAction(e -> {
            currentPage++;
            executeSearch();
        });

        btnViewDetails.setOnAction(e -> {
            int selectedIndex = resultsTable.getSelectionModel().getSelectedIndex();
            if (selectedIndex < 0 || selectedIndex >= searchResults.size()) {
                UiBuilders.placeholderAlert(resultsTable.getScene().getWindow(), "Please select a record from the search results table.");
                return;
            }
            RecordDetailsDialog.showInOrder(resultsTable.getScene().getWindow(), searchResults,
                    searchResults.get(selectedIndex), navigator.mainController().recordActions()::edit, navigator);
        });

        executeSearch();
    }

    public void focusSearchField() {
        searchField.requestFocus();
        searchField.selectAll();
    }

    private void restoreSearchState() {
        AppState state = AppState.getInstance();
        searchField.setText(state.getSearchQuery());
        restoreCombo(categoryFilter, state.getSearchCategory());
        restoreCombo(yearFilter, state.getSearchYear());
        restoreCombo(statusFilter, state.getSearchStatus());
        restoreCombo(departmentFilter, state.getSearchDepartment());
        restoreCombo(sortByFilter, state.getSearchSortBy());
        restoreCombo(sortDirectionFilter, state.getSearchSortDirection());
        currentPage = state.getSearchPage();
        int savedSize = state.getSearchPageSize();
        if (savedSize >= 10 && pageSizeFilter.getItems().contains(String.valueOf(savedSize))) {
            pageSizeFilter.setValue(String.valueOf(savedSize));
            currentPageSize = savedSize;
        }
    }

    private void restoreCombo(ComboBox<String> combo, String value) {
        if (value != null && combo.getItems().contains(value)) {
            combo.setValue(value);
        }
    }

    private void saveSearchState() {
        AppState state = AppState.getInstance();
        state.setSearchQuery(searchField.getText() == null ? "" : searchField.getText());
        state.setSearchCategory(categoryFilter.getValue());
        state.setSearchYear(yearFilter.getValue());
        state.setSearchStatus(statusFilter.getValue());
        state.setSearchDepartment(departmentFilter.getValue());
        state.setSearchSortBy(sortByFilter.getValue());
        state.setSearchSortDirection(sortDirectionFilter.getValue());
        state.setSearchPage(currentPage);
        state.setSearchPageSize(currentPageSize);
    }

    private long loadGeneration;
    public void executeSearch() {
        long generation = ++loadGeneration;
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        String cat = categoryFilter.getValue();
        String yearStr = yearFilter.getValue();
        String stat = statusFilter.getValue();
        String dept = departmentFilter.getValue();
        String sortBy = sortByFilter.getValue();
        String sortDirection = sortDirectionFilter.getValue();

        String normalizedCategory = (cat == null || "All Categories".equals(cat)) ? null : cat;
        String normalizedStatus = (stat == null || "All Statuses".equals(stat)) ? null : stat;
        String normalizedDept = (dept == null || "All Departments".equals(dept)) ? null : dept;
        String normalizedSortBy = (sortBy == null || "Default".equals(sortBy)) ? null : toSortField(sortBy);
        String normalizedSortDirection = (sortDirection == null || "Ascending".equals(sortDirection)) ? "asc" : "desc";
        Integer normalizedYear = null;
        if (yearStr != null && !"All Years".equals(yearStr)) {
            try {
                normalizedYear = Integer.parseInt(yearStr.trim());
            } catch (NumberFormatException ignored) {}
        }
        int page = currentPage;
        int size = currentPageSize;

        searchButton.setDisable(true);
        showLoadingState();

        final Integer finalYear = normalizedYear;
        final String finalSortBy = normalizedSortBy;
        final String finalSortDirection = normalizedSortDirection;
        FxAsync.run(
                () -> recordService.search(query, normalizedCategory, finalYear, normalizedStatus, normalizedDept, page, size, finalSortBy, finalSortDirection),
                paged -> {
                    if (generation != loadGeneration) return;
                    searchButton.setDisable(false);
                    AppState.getInstance().setBackendConnected(true);
                    searchResults.setAll(paged.safeContent());
                    resultCount.setText("Showing " + paged.safeContent().size() + " of " + paged.totalElements() + " records matching criteria");
                    updatePagination(paged.page(), paged.totalPages(), paged.first(), paged.last());
                    if (paged.safeContent().isEmpty()) {
                        showEmptyState();
                    }
                    saveSearchState();
                },
                error -> {
                    if (generation != loadGeneration) return;
                    searchButton.setDisable(false);
                    AppState.getInstance().setBackendConnected(false);
                    searchResults.clear();
                    pageInfoLabel.setText("1 of 1");
                    prevPageButton.setDisable(true);
                    nextPageButton.setDisable(true);
                    showErrorState(error);
                }
        );
    }

    private void updatePagination(int page, int totalPages, boolean first, boolean last) {
        currentPage = page;
        int safeTotal = Math.max(totalPages, 1);
        int safeCurrent = Math.max(page + 1, 1);
        pageInfoLabel.setText(safeCurrent + " of " + safeTotal);
        prevPageButton.setDisable(first || page <= 0);
        nextPageButton.setDisable(last);
    }

    private static String toSortField(String label) {
        switch (label) {
            case "Reference":
                return "reference";
            case "Title":
                return "title";
            case "Category":
                return "category";
            case "Department":
                return "department";
            case "Year":
                return "year";
            case "Status":
                return "status";
            default:
                return null;
        }
    }

    private void showLoadingState() {
        VBox loadingBox = new VBox(8);
        loadingBox.setAlignment(Pos.CENTER);
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(24, 24);
        Label lblLoading = new Label("Searching backend repository...");
        lblLoading.getStyleClass().add("table-placeholder");
        loadingBox.getChildren().addAll(progress, lblLoading);
        resultsTable.setPlaceholder(loadingBox);
    }

    private void showEmptyState() {
        Label emptyLabel = new Label("No records matching criteria found.");
        emptyLabel.getStyleClass().add("table-placeholder");
        resultsTable.setPlaceholder(emptyLabel);
    }

    private void showErrorState(Throwable error) {
        VBox errorBox = new VBox(8);
        errorBox.setAlignment(Pos.CENTER);
        Label errLabel = new Label("Unable to connect to the HEIRS backend.");
        errLabel.getStyleClass().add("table-placeholder");
        Label hintLabel = new Label("Start the Spring Boot server and click Retry.");
        hintLabel.getStyleClass().add("form-hint");
        Button retryBtn = new Button("Retry Search");
        retryBtn.getStyleClass().addAll("button-desktop-secondary");
        retryBtn.setOnAction(e -> {
            if (navigator != null && navigator.mainController() != null) {
                navigator.mainController().checkBackendConnection();
            }
            executeSearch();
        });
        errorBox.getChildren().addAll(errLabel, hintLabel, retryBtn);
        resultsTable.setPlaceholder(errorBox);
        resultCount.setText("Backend offline — 0 records found.");
    }

    private void setupFilters() {
        categoryFilter.getItems().setAll("All Categories");
        categoryFilter.getItems().addAll(com.heirs.desktop.model.RecordOptions.CATEGORIES);
        categoryFilter.getSelectionModel().selectFirst();

        // Load canonical backend categories asynchronously
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
                err -> {}
        );

        yearFilter.getItems().setAll("All Years", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017", "2016", "2015");
        yearFilter.getSelectionModel().selectFirst();

        statusFilter.getItems().setAll("All Statuses");
        statusFilter.getItems().addAll(com.heirs.desktop.model.RecordOptions.STATUSES);
        statusFilter.getSelectionModel().selectFirst();

        departmentFilter.getItems().setAll("All Departments",
                "Department of Higher Education",
                "National Institutional Ranking Framework",
                "Technical Education Division",
                "Policy & Planning Division",
                "Academic Administration Division",
                "Research & Innovation Cell",
                "Quality Assurance Directorate",
                "Distance & Online Education Bureau",
                "International Academic Affairs Division",
                "Statistics & Information Cell",
                "Central University Bureau",
                "Finance & Grants Committee");
        departmentFilter.getSelectionModel().selectFirst();

        sortByFilter.getItems().setAll("Default", "Reference", "Title", "Category", "Department", "Year", "Status");
        sortByFilter.getSelectionModel().selectFirst();

        sortDirectionFilter.getItems().setAll("Ascending", "Descending");
        sortDirectionFilter.getSelectionModel().selectFirst();

        pageSizeFilter.getItems().setAll("10", "20", "50", "100");
        pageSizeFilter.setValue("100");
    }

    private void setupTable() {
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

        resultsTable.getColumns().setAll(colRef, colTitle, colCat, colDept, colYear, colStatus);
        resultsTable.setItems(searchResults);

        resultsTable.setRowFactory(tv -> {
            TableRow<Record> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Window owner = resultsTable.getScene().getWindow();
                    int index = row.getIndex();
                    if (index >= 0 && index < searchResults.size()) {
                        RecordDetailsDialog.showInOrder(owner, searchResults, searchResults.get(index),
                                navigator.mainController().recordActions()::edit, navigator);
                    }
                }
            });
            return row;
        });
    }
}