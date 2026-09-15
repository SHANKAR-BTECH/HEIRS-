package com.heirs.desktop.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import com.heirs.desktop.dto.RecordRequest;
import com.heirs.desktop.model.RecordOptions;
import com.heirs.desktop.model.RecordValidation;
import com.heirs.desktop.service.RecordService;
import com.heirs.desktop.util.FxAsync;

/** Local form values only; never modifies an immutable table row. */
public class RecordFormController {
    @FXML private Label formTitle;
    @FXML private GridPane formGrid;
    @FXML private TextField titleField, departmentField, referenceField, yearField, sourceField, keywordsField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> categoryField, statusField;
    @FXML private DatePicker publishDateField;
    @FXML private Label formMessage;
    private final Map<String, Label> errors = new LinkedHashMap<>();
    private final Map<String, Control> fields = new LinkedHashMap<>();

    @FXML void initialize() {
        fields.put("referenceNumber", referenceField);
        fields.put("category", categoryField);
        fields.put("title", titleField);
        fields.put("department", departmentField);
        fields.put("publicationYear", yearField);
        fields.put("publishedDate", publishDateField);
        fields.put("status", statusField);
        fields.put("source", sourceField);
        fields.put("keywords", keywordsField);
        fields.put("description", descriptionField);
        fields.forEach((key, control) -> {
            Label error = new Label();
            error.setId(key + "Error");
            error.getStyleClass().add("form-error");
            error.setWrapText(true);
            error.setVisible(false);
            error.setManaged(false);
            errors.put(key, error);
            Integer col = GridPane.getColumnIndex(control), row = GridPane.getRowIndex(control), span = GridPane.getColumnSpan(control);
            formGrid.getChildren().remove(control);
            VBox wrapper = new VBox(2, control, error);
            GridPane.setColumnIndex(wrapper, col);
            GridPane.setRowIndex(wrapper, row);
            GridPane.setColumnSpan(wrapper, span);
            formGrid.getChildren().add(wrapper);
        });
        categoryField.getItems().setAll(RecordOptions.CATEGORIES);
        statusField.getItems().setAll(RecordOptions.STATUSES);
        statusField.setValue("Draft");
        publishDateField.setPromptText("yyyy-MM-dd");
        publishDateField.setConverter(new StringConverter<>() {
            public String toString(LocalDate date) { return date == null ? "" : date.toString(); }
            public LocalDate fromString(String text) {
                return text == null || text.isBlank() ? null : LocalDate.parse(text.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            }
        });
        FxAsync.run(new RecordService()::fetchCategoryNames, values -> {
            String selected = categoryField.getValue();
            categoryField.getItems().setAll(values);
            categoryField.setValue(selected);
        }, error -> { /* canonical fallback remains available offline */ });
    }

    public void populate(RecordRequest r) {
        formTitle.setText("Edit Record");
        titleField.setText(r.title());
        referenceField.setText(r.referenceNumber());
        categoryField.setValue(r.category());
        departmentField.setText(r.department());
        yearField.setText(r.publicationYear() == null ? "" : r.publicationYear().toString());
        publishDateField.setValue(r.publishedDate());
        statusField.setValue(r.status());
        sourceField.setText(r.source());
        keywordsField.setText(r.keywords());
        descriptionField.setText(r.description());
    }

    public RecordRequest validatedRequest() {
        var issues = new LinkedHashMap<String, String>();
        Integer year = null;
        if (!yearField.getText().isBlank()) {
            try { year = Integer.valueOf(yearField.getText().trim()); }
            catch (NumberFormatException e) { issues.put("publicationYear", "Enter a whole year from 1900 to 2100"); }
        }
        LocalDate date = null;
        try { date = publishDateField.getConverter().fromString(publishDateField.getEditor().getText()); }
        catch (RuntimeException e) { issues.put("publishedDate", "Enter a valid date as yyyy-MM-dd"); }
        var request = new RecordRequest(titleField.getText().trim(), descriptionField.getText(), categoryField.getValue(),
                departmentField.getText().trim(), referenceField.getText().trim(), year, date, statusField.getValue(),
                sourceField.getText().trim(), keywordsField.getText().trim());
        issues.putAll(RecordValidation.validate(request));
        showErrors(issues, issues.isEmpty() ? "" : "Please correct the marked fields.");
        return issues.isEmpty() ? request : null;
    }

    public void showErrors(Map<String, String> issues, String message) {
        errors.values().forEach(label -> { label.setVisible(false); label.setManaged(false); label.setText(""); });
        StringBuilder summary = new StringBuilder(message == null ? "" : message);
        issues.forEach((field, text) -> {
            Label label = errors.get(field);
            if (label != null) { label.setText(text); label.setManaged(true); label.setVisible(true); }
            else summary.append("\n").append(text);
        });
        formMessage.setText(summary.toString());
        issues.keySet().stream().filter(fields::containsKey).findFirst().ifPresent(key -> fields.get(key).requestFocus());
    }

    public void setSaving(boolean saving) {
        formGrid.setDisable(saving);
        formMessage.setText(saving ? "Saving..." : "");
    }
}
