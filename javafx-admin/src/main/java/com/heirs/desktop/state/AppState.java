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
}
