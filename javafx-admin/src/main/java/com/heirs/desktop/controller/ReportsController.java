package com.heirs.desktop.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import com.heirs.desktop.model.DemoData;
import com.heirs.desktop.model.DemoRecord;
import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.NavigatorAware;

public class ReportsController implements NavigatorAware {

    @FXML private TabPane reportsTabPane;

    @FXML private TableView<MetricRow> summaryTable;
    @FXML private TableView<DepartmentRow> departmentTable;

    @FXML private TableView<CountRow> categoryTable;
    @FXML private BarChart<String, Number> categoryChart;

    @FXML private TableView<CountRow> statusTable;
    @FXML private PieChart statusChart;

    @FXML private TableView<CountRow> yearTable;
    @FXML private BarChart<String, Number> yearChart;

    private Navigator navigator;

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
    }

    /* ------------------------------------------------------------------ */
    /* Tab 1: Summary                                                      */
    /* ------------------------------------------------------------------ */

    private void setupSummaryTab() {
        TableColumn<MetricRow, String> colMetric = new TableColumn<>("Indicator / Metric");
        colMetric.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().metric));
        colMetric.setPrefWidth(220);

        TableColumn<MetricRow, String> colVal = new TableColumn<>("Value");
        colVal.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().value));
        colVal.setPrefWidth(120);

        TableColumn<MetricRow, String> colNote = new TableColumn<>("Scope / Remarks");
        colNote.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().notes));
        colNote.setPrefWidth(320);

        summaryTable.getColumns().setAll(colMetric, colVal, colNote);

        int total = DemoData.records().size();
        List<MetricRow> metrics = List.of(
                new MetricRow("Total Repository Records", String.valueOf(total), "Institutional policy and governance catalogue"),
                new MetricRow("Total Distinct Categories", String.valueOf(DemoData.CATEGORIES.length), "Policies, Schemes, Regulations, Projects, Rules"),
                new MetricRow("Total Participating Departments", String.valueOf(DemoData.DEPARTMENTS.length), "State higher education governing bodies"),
                new MetricRow("Publication Year Span", "2019 \u2014 2026", "Eight academic cycles indexed in repository"),
                new MetricRow("Active Records Ratio", "65.0%", "39 of 60 records currently in Active status")
        );
        summaryTable.setItems(FXCollections.observableArrayList(metrics));

        // Department Table
        TableColumn<DepartmentRow, String> colDept = new TableColumn<>("Department");
        colDept.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().department));
        colDept.setPrefWidth(200);

        TableColumn<DepartmentRow, String> colPol = new TableColumn<>("Policies");
        colPol.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().policies)));
        colPol.setPrefWidth(85);

        TableColumn<DepartmentRow, String> colSch = new TableColumn<>("Schemes");
        colSch.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().schemes)));
        colSch.setPrefWidth(85);

        TableColumn<DepartmentRow, String> colReg = new TableColumn<>("Regulations");
        colReg.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().regulations)));
        colReg.setPrefWidth(95);

        TableColumn<DepartmentRow, String> colPrj = new TableColumn<>("Projects");
        colPrj.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().projects)));
        colPrj.setPrefWidth(85);

        TableColumn<DepartmentRow, String> colRul = new TableColumn<>("Rules");
        colRul.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().rules)));
        colRul.setPrefWidth(85);

        TableColumn<DepartmentRow, String> colTot = new TableColumn<>("Total");
        colTot.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().total)));
        colTot.setPrefWidth(85);

        departmentTable.getColumns().setAll(colDept, colPol, colSch, colReg, colPrj, colRul, colTot);

        Map<String, DepartmentRow> deptMap = new LinkedHashMap<>();
        for (String d : DemoData.DEPARTMENTS) {
            deptMap.put(d, new DepartmentRow(d));
        }

        for (DemoRecord r : DemoData.records()) {
            DepartmentRow row = deptMap.get(r.getDepartment());
            if (row != null) {
                switch (r.getCategory()) {
                    case "Policy" -> row.policies++;
                    case "Scheme" -> row.schemes++;
                    case "Regulation" -> row.regulations++;
                    case "Project" -> row.projects++;
                    case "Rules" -> row.rules++;
                }
                row.total++;
            }
        }
        departmentTable.setItems(FXCollections.observableArrayList(deptMap.values()));
    }

    /* ------------------------------------------------------------------ */
    /* Tab 2: By Category                                                 */
    /* ------------------------------------------------------------------ */

    private void setupCategoryTab() {
        TableColumn<CountRow, String> colName = new TableColumn<>("Category");
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name));
        colName.setPrefWidth(160);

        TableColumn<CountRow, String> colCnt = new TableColumn<>("Count");
        colCnt.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().count)));
        colCnt.setPrefWidth(90);

        TableColumn<CountRow, String> colPct = new TableColumn<>("Percentage");
        colPct.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().percentage));
        colPct.setPrefWidth(100);

        categoryTable.getColumns().setAll(colName, colCnt, colPct);

        Map<String, Integer> catCounts = DemoData.categoryCounts();
        int total = DemoData.records().size();
        List<CountRow> rows = new ArrayList<>();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Records");

        for (Map.Entry<String, Integer> e : catCounts.entrySet()) {
            double pct = (e.getValue() * 100.0) / total;
            rows.add(new CountRow(e.getKey(), e.getValue(), String.format("%.1f%%", pct)));
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }

        categoryTable.setItems(FXCollections.observableArrayList(rows));
        categoryChart.getData().setAll(series);
    }

    /* ------------------------------------------------------------------ */
    /* Tab 3: By Status                                                   */
    /* ------------------------------------------------------------------ */

    private void setupStatusTab() {
        TableColumn<CountRow, String> colName = new TableColumn<>("Status");
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name));
        colName.setPrefWidth(150);

        TableColumn<CountRow, String> colCnt = new TableColumn<>("Count");
        colCnt.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().count)));
        colCnt.setPrefWidth(90);

        TableColumn<CountRow, String> colPct = new TableColumn<>("Percentage");
        colPct.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().percentage));
        colPct.setPrefWidth(100);

        statusTable.getColumns().setAll(colName, colCnt, colPct);

        Map<String, Integer> statusCounts = DemoData.statusCounts();
        int total = DemoData.records().size();
        List<CountRow> rows = new ArrayList<>();
        List<PieChart.Data> slices = new ArrayList<>();

        for (Map.Entry<String, Integer> e : statusCounts.entrySet()) {
            double pct = (e.getValue() * 100.0) / total;
            rows.add(new CountRow(e.getKey(), e.getValue(), String.format("%.1f%%", pct)));
            slices.add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
        }

        statusTable.setItems(FXCollections.observableArrayList(rows));
        statusChart.setData(FXCollections.observableArrayList(slices));
    }

    /* ------------------------------------------------------------------ */
    /* Tab 4: By Year                                                     */
    /* ------------------------------------------------------------------ */

    private void setupYearTab() {
        TableColumn<CountRow, String> colName = new TableColumn<>("Year");
        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().name));
        colName.setPrefWidth(140);

        TableColumn<CountRow, String> colCnt = new TableColumn<>("Count");
        colCnt.setCellValueFactory(cd -> new SimpleStringProperty(String.valueOf(cd.getValue().count)));
        colCnt.setPrefWidth(90);

        TableColumn<CountRow, String> colPct = new TableColumn<>("Percentage");
        colPct.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().percentage));
        colPct.setPrefWidth(100);

        yearTable.getColumns().setAll(colName, colCnt, colPct);

        Map<Integer, Integer> yearCounts = DemoData.yearCounts();
        int total = DemoData.records().size();
        List<CountRow> rows = new ArrayList<>();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Publications");

        for (Map.Entry<Integer, Integer> e : yearCounts.entrySet()) {
            double pct = (e.getValue() * 100.0) / total;
            String yearStr = String.valueOf(e.getKey());
            rows.add(new CountRow(yearStr, e.getValue(), String.format("%.1f%%", pct)));
            series.getData().add(new XYChart.Data<>(yearStr, e.getValue()));
        }

        yearTable.setItems(FXCollections.observableArrayList(rows));
        yearChart.getData().setAll(series);
    }

    /* ------------------------------------------------------------------ */
    /* Data Classes for Tables                                             */
    /* ------------------------------------------------------------------ */

    public static class MetricRow {
        public final String metric;
        public final String value;
        public final String notes;

        public MetricRow(String metric, String value, String notes) {
            this.metric = metric;
            this.value = value;
            this.notes = notes;
        }
    }

    public static class DepartmentRow {
        public final String department;
        public int policies = 0;
        public int schemes = 0;
        public int regulations = 0;
        public int projects = 0;
        public int rules = 0;
        public int total = 0;

        public DepartmentRow(String department) {
            this.department = department;
        }
    }

    public static class CountRow {
        public final String name;
        public final int count;
        public final String percentage;

        public CountRow(String name, int count, String percentage) {
            this.name = name;
            this.count = count;
            this.percentage = percentage;
        }
    }
}