package com.heirs.desktop.navigation;

import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * Each screen in the application is a single {@code ViewType}.
 * The enum carries the FXML path, visible header text, and icon glyph.
 */
public enum ViewType {

    DASHBOARD(
            "/com/heirs/desktop/fxml/dashboard.fxml",
            "Dashboard",
            "Overview of the HEIRS repository",
            FontAwesomeSolid.HOME),

    SEARCH(
            "/com/heirs/desktop/fxml/search.fxml",
            "Search & Retrieval",
            "Find regulations, policies, schemes, projects, and rules",
            FontAwesomeSolid.SEARCH),

    RECORDS(
            "/com/heirs/desktop/fxml/records.fxml",
            "Manage Records",
            "Create, review, and manage repository records",
            FontAwesomeSolid.FOLDER_OPEN),

    DOCUMENTS(
            "/com/heirs/desktop/fxml/documents.fxml",
            "Document Management",
            "Repository supporting documents and attachments",
            FontAwesomeSolid.FILE_ALT),

    REPORTS(
            "/com/heirs/desktop/fxml/reports.fxml",
            "Reports",
            "Repository analytics and distribution",
            FontAwesomeSolid.CHART_BAR),

    ABOUT(
            "/com/heirs/desktop/fxml/about.fxml",
            "System / About",
            "Desktop client information and architecture",
            FontAwesomeSolid.INFO_CIRCLE);

    private final String fxmlPath;
    private final String title;
    private final String subtitle;
    private final FontAwesomeSolid icon;

    ViewType(String fxmlPath, String title, String subtitle, FontAwesomeSolid icon) {
        this.fxmlPath = fxmlPath;
        this.title = title;
        this.subtitle = subtitle;
        this.icon = icon;
    }

    public String fxmlPath() {
        return fxmlPath;
    }

    public String title() {
        return title;
    }

    public String subtitle() {
        return subtitle;
    }

    public FontAwesomeSolid icon() {
        return icon;
    }
}