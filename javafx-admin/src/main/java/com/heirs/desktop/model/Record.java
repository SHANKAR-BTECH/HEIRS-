package com.heirs.desktop.model;

import java.time.LocalDate;
import java.util.Objects;

import com.heirs.desktop.dto.RecordResponse;

/**
 * UI display model for higher education records.
 * Keeps controllers clean and decoupled from raw REST DTOs.
 */
public class Record {

    private final Long id;
    private final String reference;
    private final String title;
    private final String category;
    private final String department;
    private final Integer year;
    private final LocalDate publishedDate;
    private final String status;
    private final String description;
    private final String source;
    private final String keywords;

    public Record(Long id,
                  String reference,
                  String title,
                  String category,
                  String department,
                  Integer year,
                  LocalDate publishedDate,
                  String status,
                  String description,
                  String source,
                  String keywords) {
        this.id = id;
        this.reference = reference != null ? reference : "";
        this.title = title != null ? title : "";
        this.category = category != null ? category : "";
        this.department = department != null ? department : "";
        this.year = year != null ? year : 0;
        this.publishedDate = publishedDate;
        this.status = status != null ? status : "";
        this.description = description != null ? description : "";
        this.source = source != null ? source : "";
        this.keywords = keywords != null ? keywords : "";
    }

    public static Record from(RecordResponse dto) {
        if (dto == null) {
            return null;
        }
        return new Record(
                dto.id(),
                dto.referenceNumber(),
                dto.title(),
                dto.category(),
                dto.department(),
                dto.publicationYear(),
                dto.publishedDate(),
                dto.status(),
                dto.description(),
                dto.source(),
                dto.keywords()
        );
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDepartment() {
        return department;
    }

    public Integer getYear() {
        return year;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    public String getKeywords() {
        return keywords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return Objects.equals(id, record.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return reference + " — " + title;
    }
}
