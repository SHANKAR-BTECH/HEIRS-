package com.heirs.desktop.state;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Shared state container for global desktop client status.
 */
public final class AppState {

    private static final AppState INSTANCE = new AppState();

    private final BooleanProperty backendConnected = new SimpleBooleanProperty(false);
    private final StringProperty backendStatusMessage = new SimpleStringProperty("Backend: Checking...");
    private final StringProperty totalRecordsCount = new SimpleStringProperty("Records: —");

    private AppState() {
    }

    public static AppState getInstance() {
        return INSTANCE;
    }

    public boolean isBackendConnected() {
        return backendConnected.get();
    }

    public BooleanProperty backendConnectedProperty() {
        return backendConnected;
    }

    public void setBackendConnected(boolean connected) {
        this.backendConnected.set(connected);
        if (connected) {
            this.backendStatusMessage.set("Backend: Connected");
        } else {
            this.backendStatusMessage.set("Backend: Offline");
        }
    }

    public StringProperty backendStatusMessageProperty() {
        return backendStatusMessage;
    }

    public String getBackendStatusMessage() {
        return backendStatusMessage.get();
    }

    public void setBackendStatusMessage(String message) {
        this.backendStatusMessage.set(message);
    }

    public StringProperty totalRecordsCountProperty() {
        return totalRecordsCount;
    }

    public void setTotalRecordsCount(long count) {
        this.totalRecordsCount.set("Records: " + count);
    }

    private String searchQuery = "";
    private String searchCategory;
    private String searchYear;
    private String searchStatus;
    private String searchDepartment;
    private String searchSortBy;
    private String searchSortDirection;
    private int searchPage;
    private int searchPageSize = 100;

    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public String getSearchCategory() { return searchCategory; }
    public void setSearchCategory(String searchCategory) { this.searchCategory = searchCategory; }
    public String getSearchYear() { return searchYear; }
    public void setSearchYear(String searchYear) { this.searchYear = searchYear; }
    public String getSearchStatus() { return searchStatus; }
    public void setSearchStatus(String searchStatus) { this.searchStatus = searchStatus; }
    public String getSearchDepartment() { return searchDepartment; }
    public void setSearchDepartment(String searchDepartment) { this.searchDepartment = searchDepartment; }
    public String getSearchSortBy() { return searchSortBy; }
    public void setSearchSortBy(String searchSortBy) { this.searchSortBy = searchSortBy; }
    public String getSearchSortDirection() { return searchSortDirection; }
    public void setSearchSortDirection(String searchSortDirection) { this.searchSortDirection = searchSortDirection; }
    public int getSearchPage() { return searchPage; }
    public void setSearchPage(int searchPage) { this.searchPage = searchPage; }
    public int getSearchPageSize() { return searchPageSize; }
    public void setSearchPageSize(int searchPageSize) { this.searchPageSize = searchPageSize; }
}
