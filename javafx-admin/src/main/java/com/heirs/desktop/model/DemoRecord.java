package com.heirs.desktop.model;

/**
 * UI-only placeholder model used to render the Phase J1 screens.
 *
 * <p>TODO Phase J2/J4: replace this placeholder type with the real API/DTO
 * model returned by the Spring Boot REST layer. It must never be confused
 * with a backend entity.
 */
public final class DemoRecord {

    private final long id;
    private String reference;
    private String title;
    private String category;
    private String department;
    private int year;
    private String status;
    private String description;
    private String keywords;
    private String source;

    public DemoRecord(long id,
                      String reference,
                      String title,
                      String category,
                      String department,
                      int year,
                      String status,
                      String description,
                      String keywords,
                      String source) {
        this.id = id;
        this.reference = reference;
        this.title = title;
        this.category = category;
        this.department = department;
        this.year = year;
        this.status = status;
        this.description = description;
        this.keywords = keywords;
        this.source = source;
    }

    public long getId() {
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

    public int getYear() {
        return year;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getSource() {
        return source;
    }
}