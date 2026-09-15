package com.heirs.desktop.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import com.heirs.desktop.model.report.DocumentReportRow;
import com.heirs.desktop.model.report.ReportCountRow;
import com.heirs.desktop.model.report.ReportData;
import com.heirs.desktop.model.report.YearReportRow;
import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.NavigatorAware;
import com.heirs.desktop.service.ReportService;
import com.heirs.desktop.state.AppState;
import com.heirs.desktop.util.CsvExporter;
import com.heirs.desktop.util.FileNames;
import com.heirs.desktop.util.FxAsync;

public class ReportsController implements NavigatorAware {

    @FXML private TabPane reportsTabPane;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button btnRefresh;
    @FXML private Button btnExport;

    @FXML private TableView<SummaryRow> summaryTable;
    @FXML private TableView<ReportCountRow> categoryTable;
    @FXML private BarChart<String, Number> categoryChart;
    @FXML private TableView<ReportCountRow> statusTable;
    @FXML private PieChart statusChart;
    @FXML private TableView<YearReportRow> yearTable;
    @FXML private BarChart<String, Number> yearChart;
    @FXML private TableView<ReportCountRow> departmentTable;
    @FXML private BarChart<String, Number> departmentChart;
    @FXML private TableView<DocumentReportRow> documentsTable;
    @FXML private Label docStatsLabel;
    @FXML private Label docWarningLabel;

    @FXML private Label reportStatusLabel;
    @FXML private Label generatedLabel;

    private Navigator navigator;
    private final ReportService reportService = new ReportService();
    private ReportData currentData;
    private long loadGeneration;

    private static final DateTimeFormatter GENERATED_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH);

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    void initialize() {
        setupSummaryTab();
        setupCategoryTab();
        setupStatusTab();
        setupYearTab();
        setupDepartmentTab();
        setupDocumentsTab();

        generateReports();
    }

    /* ------------------------------------------------------------------ */
    /* Report generation                                                   */
    /* ------------------------------------------------------------------ */

    void generateReports() {
        long generation = ++loadGeneration;
        int selectedTab = reportsTabPane.getSelectionModel().getSelectedIndex();
        setBusy(true);
        reportStatusLabel.setText("Generating report...");
        generatedLabel.setText("");

        FxAsync.run(
                reportService::generateReports,
                data -> {
                    if (generation != loadGeneration) return;
                    AppState.getInstance().setBackendConnected(true);
                    currentData = data;
                    renderAll(data);
                    reportStatusLabel.setText("Reports loaded. " + data.totalRecords() + " records analyzed.");
                    if (data.generatedAt() != null) {
                        generatedLabel.setText("Generated: " + data.generatedAt().format(GENERATED_FORMAT));
                    } else {
                        generatedLabel.setText("");
                    }
                    if (selectedTab >= 0 && selectedTab < reportsTabPane.getTabs().size()) {
                        reportsTabPane.getSelectionModel().select(selectedTab);
                    }
                    setBusy(false);
                },
                error -> {
                    if (generation != loadGeneration) return;
                    AppState.getInstance().setBackendConnected(false);
                    currentData = null;
                    clearAll();
                    showOfflineState();
                    setBusy(false);
                }
        );
    }

    private void renderAll(ReportData data) {
        renderSummary(data);
        renderCategory(data);
        renderStatus(data);
        renderYear(data);
        renderDepartment(data);
        renderDocuments(data);
    }

    private void clearAll() {
        summaryTable.setItems(FXCollections.observableArrayList());
        categoryTable.setItems(FXCollections.observableArrayList());
        categoryChart.getData().clear();
        statusTable.setItems(FXCollections.observableArrayList());
        statusChart.setData(FXCollections.observableArrayList());
        yearTable.setItems(FXCollections.observableArrayList());
        yearChart.getData().clear();
        departmentTable.setItems(FXCollections.observableArrayList());
        departmentChart.getData().clear();
        documentsTable.setItems(FXCollections.observableArrayList());
        docStatsLabel.setText("Total Documents: -");
        docWarningLabel.setVisible(false);
        docWarningLabel.setManaged(false);
        reportStatusLabel.setText("Backend Offline.");
    }

    private void setBusy(boolean busy) {
        loadingIndicator.setVisible(busy);
        btnRefresh.setDisable(busy);
        btnExport.setDisable(busy);
    }

    /* ------------------------------------------------------------------ */
    /* Tab 1: Summary                                                      */
    /* ------------------------------------------------------------------ */

    private static final class SummaryRow {
        final String metric;
        final String value;

        SummaryRow(String metric, String value) {
            this.metric = metric;
            this.value = value;
        }
    }

    private void renderSummary(ReportData data) {
        List<SummaryRow> rows = new ArrayList<>();
        var summary = data.summary();
        rows.add(new SummaryRow("Total Records", String.valueOf(summary.totalRecords())));
        summary.categoryCounts().forEach((name, count) ->
                rows.add(new SummaryRow("Records - " + name, String.valueOf(count))));
        summary.statusCounts().forEach((name, count) ->
                rows.add(new SummaryRow("Records - " + name, String.valueOf(count))));
        rows.add(new SummaryRow("Departments Represented", String.valueOf(summary.departmentsRepresented())));
        rows.add(new SummaryRow("Years Represented", String.valueOf(summary.yearsRepresented())));
        rows.add(new SummaryRow("Supporting Documents", String.valueOf(summary.totalDocuments())));
        rows.add(new SummaryRow("Records With Documents", String.valueOf(summary.recordsWithDocuments())));
        rows.add(new SummaryRow("Records Without Documents", String.valueOf(summary.recordsWithoutDocuments())));
        summaryTable.setItems(FXCollections.observableArrayList(rows));
    }

    /* ------------------------------------------------------------------ */
    /* Tab 2: By Category                                                 */
    /* ------------------------------------------------------------------ */

    private void renderCategory(ReportData data) {
        categoryTable.setItems(FXCollections.observableArrayList(data.categories()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Records");
        for (ReportCountRow row : data.categories()) {
            series.getData().add(new XYChart.Data<>(row.name(), row.count()));
        }
        categoryChart.getData().setAll(series);
    }

    /* ------------------------------------------------------------------ */
    /* Tab 3: By Status                                                   */
    /* ------------------------------------------------------------------ */

    private void renderStatus(ReportData data) {
        statusTable.setItems(FXCollections.observableArrayList(data.statuses()));
        List<PieChart.Data> slices = new ArrayList<>();
        for (ReportCountRow row : data.statuses()) {
            slices.add(new PieChart.Data(row.name() + " (" + row.count() + ")", row.count()));
        }
        statusChart.setData(FXCollections.observableArrayList(slices));
    }

    /* ------------------------------------------------------------------ */
    /* Tab 4: By Year                                                     */
    /* ------------------------------------------------------------------ */

    private void renderYear(ReportData data) {
        yearTable.setItems(FXCollections.observableArrayList(data.years()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Publications");
        for (YearReportRow row : data.years()) {
            series.getData().add(new XYChart.Data<>(row.yearText(), row.count()));
        }
        yearChart.getData().setAll(series);
    }

    /* ------------------------------------------------------------------ */
    /* Tab 5: By Department                                               */
    /* ------------------------------------------------------------------ */

    private void renderDepartment(ReportData data) {
        departmentTable.setItems(FXCollections.observableArrayList(data.departments()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Records");
        for (ReportCountRow row : data.departments()) {
            series.getData().add(new XYChart.Data<>(row.name(), row.count()));
        }
        departmentChart.getData().setAll(series);
    }

    /* ------------------------------------------------------------------ */
    /* Tab 6: Documents                                                   */
    /* ------------------------------------------------------------------ */

    private void renderDocuments(ReportData data) {
        documentsTable.setItems(FXCollections.observableArrayList(data.documents()));
        docStatsLabel.setText("Total Documents: " + data.supportedReportTotalDocuments()
                + "   Records With: " + data.supportedReportRecordsWithDocs()
                + "   Records Without: " + data.supportedReportRecordsWithoutDocs()
                + "   Avg per Record: " + String.format(Locale.ROOT, "%.2f", data.averageDocumentsPerRecord())
                + "   Largest: " + data.largestDocumentCount()
                + "   Stored Bytes: " + FileNames.formatSize(data.totalStoredFileBytes()));
        boolean incomplete = !data.documentsLoaded();
        docWarningLabel.setVisible(incomplete);
        docWarningLabel.setManaged(incomplete);
        if (incomplete) {
            docWarningLabel.setText("Document statistics could not be fully loaded.");
        }
    }

    /* ------------------------------------------------------------------ */
    /* Offline state                                                       */
    /* ------------------------------------------------------------------ */

    private void showOfflineState() {
        VBox errorBox = new VBox(8);
        errorBox.setAlignment(Pos.CENTER);
        Label errLabel = new Label("Unable to load reports. Backend Offline.");
        errLabel.getStyleClass().add("table-placeholder");
        Label hintLabel = new Label("Start the Spring Boot server and click Retry.");
        hintLabel.getStyleClass().add("form-hint");
        Button retryBtn = new Button("Retry");
        retryBtn.getStyleClass().addAll("button-desktop-secondary");
        retryBtn.setOnAction(e -> {
            if (navigator != null && navigator.mainController() != null) {
                navigator.mainController().checkBackendConnection();
            }
            generateReports();
        });
        errorBox.getChildren().addAll(errLabel, hintLabel, retryBtn);
        summaryTable.setPlaceholder(errorBox);
        reportStatusLabel.setText("Unable to load reports. Backend Offline.");
    }

    /* ------------------------------------------------------------------ */
    /* Column setup                                                        */
    /* ------------------------------------------------------------------ */

    private void setupSummaryTab() {
        TableColumn<SummaryRow, String> colMetric = new TableColumn<>("Metric");
        colMetric.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().metric));
        colMetric.setPrefWidth(260);
        TableColumn<SummaryRow, String> colValue = new TableColumn<>("Value");
        colValue.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().value));
        colValue.setPrefWidth(120);
        summaryTable.getColumns().setAll(colMetric, colValue);
        summaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupCategoryTab() {
        TableColumn<ReportCountRow, String> colName = new TableColumn<>("Category");
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        colName.setPrefWidth(160);
        TableColumn<ReportCountRow, String> colCnt = new TableColumn<>("Record Count");
        colCnt.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().countText()));
        colCnt.setPrefWidth(110);
        TableColumn<ReportCountRow, String> colPct = new TableColumn<>("Percentage");
        colPct.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().percentageText()));
        colPct.setPrefWidth(110);
        categoryTable.getColumns().setAll(colName, colCnt, colPct);
        categoryChart.setAnimated(false);
    }

    private void setupStatusTab() {
        TableColumn<ReportCountRow, String> colName = new TableColumn<>("Status");
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        colName.setPrefWidth(150);
        TableColumn<ReportCountRow, String> colCnt = new TableColumn<>("Record Count");
        colCnt.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().countText()));
        colCnt.setPrefWidth(110);
        TableColumn<ReportCountRow, String> colPct = new TableColumn<>("Percentage");
        colPct.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().percentageText()));
        colPct.setPrefWidth(110);
        statusTable.getColumns().setAll(colName, colCnt, colPct);
        statusChart.setAnimated(false);
    }

    private void setupYearTab() {
        TableColumn<YearReportRow, String> colYear = new TableColumn<>("Year");
        colYear.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().yearText()));
        colYear.setPrefWidth(140);
        TableColumn<YearReportRow, String> colCnt = new TableColumn<>("Record Count");
        colCnt.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().count())));
        colCnt.setPrefWidth(110);
        TableColumn<YearReportRow, String> colPct = new TableColumn<>("Percentage");
        colPct.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().percentageText()));
        colPct.setPrefWidth(110);
        yearTable.getColumns().setAll(colYear, colCnt, colPct);
        yearChart.setAnimated(false);
    }

    private void setupDepartmentTab() {
        TableColumn<ReportCountRow, String> colDept = new TableColumn<>("Department");
        colDept.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name()));
        colDept.setPrefWidth(300);
        TableColumn<ReportCountRow, String> colCnt = new TableColumn<>("Record Count");
        colCnt.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().countText()));
        colCnt.setPrefWidth(110);
        TableColumn<ReportCountRow, String> colPct = new TableColumn<>("Percentage");
        colPct.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().percentageText()));
        colPct.setPrefWidth(110);
        departmentTable.getColumns().setAll(colDept, colCnt, colPct);
        departmentChart.setAnimated(false);
    }

    private void setupDocumentsTab() {
        TableColumn<DocumentReportRow, String> colRef = new TableColumn<>("Reference");
        colRef.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().reference()));
        colRef.setPrefWidth(180);
        TableColumn<DocumentReportRow, String> colTitle = new TableColumn<>("Record Title");
        colTitle.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().title()));
        colTitle.setPrefWidth(360);
        TableColumn<DocumentReportRow, String> colCnt = new TableColumn<>("Document Count");
        colCnt.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().documentCount())));
        colCnt.setPrefWidth(120);
        TableColumn<DocumentReportRow, String> colBytes = new TableColumn<>("Total Size Bytes");
        colBytes.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().totalSizeBytes())));
        colBytes.setPrefWidth(120);
        TableColumn<DocumentReportRow, String> colSize = new TableColumn<>("Total Size");
        colSize.setCellValueFactory(cd -> new SimpleStringProperty(FileNames.formatSize(cd.getValue().totalSizeBytes())));
        colSize.setPrefWidth(110);
        documentsTable.getColumns().setAll(colRef, colTitle, colCnt, colBytes, colSize);
    }

    /* ------------------------------------------------------------------ */
    /* CSV export                                                          */
    /* ------------------------------------------------------------------ */

    @FXML
    void onRefresh() {
        generateReports();
    }

    @FXML
    void onExport() {
        if (currentData == null) {
            Alert info = new Alert(Alert.AlertType.INFORMATION,
                    "No report is currently loaded. Refresh the reports first.");
            info.setTitle("Nothing to Export");
            info.setHeaderText(null);
            if (getWindow() != null) info.initOwner(getWindow());
            info.show();
            return;
        }

        String selectedTab = reportsTabPane.getSelectionModel().getSelectedItem() == null
                ? "" : reportsTabPane.getSelectionModel().getSelectedItem().getText();
        String reportKey = reportKeyForTab(selectedTab);
        List<List<String>> rows = rowsForCurrentTab();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + selectedTab + " Report as CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        chooser.setInitialFileName("HEIRS_" + reportKey + "_Report_" + LocalDate.now() + ".csv");
        java.io.File target = chooser.showSaveDialog(getWindow());
        if (target == null) {
            return;
        }

        try {
            CsvExporter.write(target.toPath(), rows);
            setStatusBar("Report exported successfully.");
        } catch (IOException e) {
            Alert error = new Alert(Alert.AlertType.ERROR,
                    "Could not write the report file: " + e.getMessage());
            error.setTitle("Export Failed");
            error.setHeaderText(null);
            if (getWindow() != null) error.initOwner(getWindow());
            error.showAndWait();
        }
    }

    private static String reportKeyForTab(String tabTitle) {
        return switch (tabTitle) {
            case "Repository Summary" -> "Summary";
            case "By Category" -> "Category";
            case "By Status" -> "Status";
            case "By Year" -> "Year";
            case "By Department" -> "Department";
            case "Documents" -> "Documents";
            default -> "Report";
        };
    }

    private List<List<String>> rowsForCurrentTab() {
        String selectedTab = reportsTabPane.getSelectionModel().getSelectedItem() == null
                ? "" : reportsTabPane.getSelectionModel().getSelectedItem().getText();
        List<List<String>> rows = new ArrayList<>();
        switch (selectedTab) {
            case "Repository Summary" -> buildSummaryCsvRows(rows);
            case "By Category" -> buildCountCsvRows(rows, "Category", currentData.categories());
            case "By Status" -> buildCountCsvRows(rows, "Status", currentData.statuses());
            case "By Year" -> buildYearCsvRows(rows);
            case "By Department" -> buildCountCsvRows(rows, "Department", currentData.departments());
            case "Documents" -> buildDocumentsCsvRows(rows);
            default -> { }
        }
        return rows;
    }

    private void buildSummaryCsvRows(List<List<String>> rows) {
        rows.add(List.of("Metric", "Value"));
        var summary = currentData.summary();
        rows.add(List.of("Total Records", String.valueOf(summary.totalRecords())));
        summary.categoryCounts().forEach((name, count) ->
                rows.add(List.of("Records - " + name, String.valueOf(count))));
        summary.statusCounts().forEach((name, count) ->
                rows.add(List.of("Records - " + name, String.valueOf(count))));
        rows.add(List.of("Departments Represented", String.valueOf(summary.departmentsRepresented())));
        rows.add(List.of("Years Represented", String.valueOf(summary.yearsRepresented())));
        rows.add(List.of("Supporting Documents", String.valueOf(summary.totalDocuments())));
        rows.add(List.of("Records With Documents", String.valueOf(summary.recordsWithDocuments())));
        rows.add(List.of("Records Without Documents", String.valueOf(summary.recordsWithoutDocuments())));
    }

    private void buildCountCsvRows(List<List<String>> rows, String header, List<ReportCountRow> data) {
        rows.add(List.of(header, "Record Count", "Percentage"));
        for (ReportCountRow row : data) {
            rows.add(List.of(row.name(), String.valueOf(row.count()), row.percentageText()));
        }
    }

    private void buildYearCsvRows(List<List<String>> rows) {
        rows.add(List.of("Year", "Record Count", "Percentage"));
        for (YearReportRow row : currentData.years()) {
            rows.add(List.of(row.yearText(), String.valueOf(row.count()), row.percentageText()));
        }
    }

    private void buildDocumentsCsvRows(List<List<String>> rows) {
        rows.add(List.of("Reference", "Title", "Document Count", "Total Size Bytes", "Total Size"));
        for (DocumentReportRow row : currentData.documents()) {
            rows.add(List.of(row.reference(), row.title(), String.valueOf(row.documentCount()),
                    String.valueOf(row.totalSizeBytes()), FileNames.formatSize(row.totalSizeBytes())));
        }
    }

    private void setStatusBar(String message) {
        if (navigator != null && navigator.mainController() != null) {
            navigator.mainController().setStatusMessage(message);
        }
        reportStatusLabel.setText(message);
    }

    private Window getWindow() {
        if (reportsTabPane.getScene() == null) return null;
        return reportsTabPane.getScene().getWindow();
    }
}