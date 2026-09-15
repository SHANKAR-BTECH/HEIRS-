package com.heirs.desktop.navigation;

import java.io.IOException;

import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import com.heirs.desktop.controller.MainController;

/**
 * Central screen router for the desktop administration application.
 * Replaces the central working content area with the requested ViewType,
 * notifies the active controller, and synchronizes the navigation panel and status bar.
 */
public final class Navigator {

    private final StackPane contentArea;
    private final MainController mainController;

    private HostServices hostServices;
    private Object currentController;
    private ViewType currentView;

    public Navigator(StackPane contentArea, MainController mainController) {
        this.contentArea = contentArea;
        this.mainController = mainController;
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    public HostServices hostServices() {
        return hostServices;
    }

    public Object currentController() {
        return currentController;
    }

    public ViewType currentView() {
        return currentView;
    }

    public MainController mainController() {
        return mainController;
    }

    /** Views are recreated on navigation, so inactive screens have no retained data cache. */
    public void refreshRecordsAfterWrite(Long savedId, boolean created) {
        if (currentController instanceof com.heirs.desktop.controller.RecordsController records)
            records.refreshAfterWrite(savedId, created);
        else if (currentController instanceof com.heirs.desktop.controller.DashboardController dashboard)
            dashboard.loadDashboard();
        else if (currentController instanceof com.heirs.desktop.controller.SearchController search)
            search.executeSearch();
        // The global record count must update even when a write originates outside Manage Records.
        com.heirs.desktop.util.FxAsync.run(() -> new com.heirs.desktop.service.RecordService().fetchAll(0, 1),
                page -> com.heirs.desktop.state.AppState.getInstance().setTotalRecordsCount(page.totalElements()),
                error -> { if (error instanceof com.heirs.desktop.api.ApiException api && api.isOffline())
                    com.heirs.desktop.state.AppState.getInstance().setBackendConnected(false); });
    }

    /**
     * Load the given {@link ViewType} FXML, replace the central workspace,
     * and update the navigation tree and status bar.
     */
    public void navigateTo(ViewType type) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(type.fxmlPath()));
            Parent view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof NavigatorAware aware) {
                aware.setNavigator(this);
            }
            this.currentController = controller;
            this.currentView = type;

            contentArea.getChildren().setAll(view);
            if (mainController != null) {
                mainController.setActiveView(type);
                mainController.setStatusMessage("Ready \u2014 " + type.title());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load view " + type, e);
        }
    }
}
