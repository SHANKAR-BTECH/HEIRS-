package com.heirs.desktop.controller;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import com.heirs.desktop.dto.DocumentResponse;
import com.heirs.desktop.model.Record;

public class RecordDetailsController {

    @FXML private Label headerTitle;
    @FXML private Label headerReference;
    @FXML private Label statusBadge;

    @FXML private TextField referenceField;
    @FXML private TextField categoryField;
    @FXML private TextField titleField;
    @FXML private TextField departmentField;
    @FXML private TextField yearField;
    @FXML private TextField keywordsField;
    @FXML private TextArea descriptionField;

    public void setRecord(Record record) {
        if (record == null) return;

        headerTitle.setText(record.getTitle());
        headerReference.setText("Reference: " + record.getReference());
        statusBadge.setText(record.getStatus());

        referenceField.setText(record.getReference());
        categoryField.setText(record.getCategory());
        titleField.setText(record.getTitle());
        departmentField.setText(record.getDepartment());
        yearField.setText(record.getYear() != null && record.getYear() > 0 ? String.valueOf(record.getYear()) : "—");
        keywordsField.setText(record.getKeywords() == null || record.getKeywords().isBlank() ? "—" : record.getKeywords());
        descriptionField.setText(record.getDescription() == null || record.getDescription().isBlank() ? "No detailed description provided." : record.getDescription());
    }

    public void setDocuments(List<DocumentResponse> documents) {
        if (documents != null && !documents.isEmpty()) {
            headerReference.setText(headerReference.getText() + "  [" + documents.size() + " attached document(s)]");
        }
    }
}
