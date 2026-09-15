package com.heirs.desktop;

import com.heirs.desktop.config.ApiConfig;
import com.heirs.desktop.controller.MainController;
import com.heirs.desktop.navigation.ViewType;
import com.heirs.desktop.state.AppState;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static com.heirs.desktop.FxTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="heirs.live", matches="true")
class J2LiveTest {
    @Test void offlineLaunchThenRetryLoadsRealRecords() throws Exception {
        start();
        ApiConfig.setOverrideBaseUrl("http://127.0.0.1:1");
        var stage = openMain();
        try {
            until(() -> AppState.getInstance().getBackendStatusMessage().equals("Backend: Offline"));
            fx(() -> { ((MainController)stage.getUserData()).getNavigator().navigateTo(ViewType.RECORDS); return null; });
            until(() -> stage.getScene().getRoot().lookupAll(".button").stream().anyMatch(n -> n instanceof javafx.scene.control.Button b && b.getText().equals("Retry Connection")));
            assertFalse(fx(() -> AppState.getInstance().isBackendConnected()));
            ApiConfig.setOverrideBaseUrl(null);
            fx(() -> { button(stage.getScene().getRoot(), "Retry Connection").fire(); return null; });
            until(() -> AppState.getInstance().isBackendConnected() && !((TableView<?>)stage.getScene().lookup("#tableView")).getItems().isEmpty());
            System.out.println("J2 VERIFIED: offline launch, Retry control, Connected, real records loaded");
        } finally { ApiConfig.setOverrideBaseUrl(null); closeWindows(); }
    }
}
