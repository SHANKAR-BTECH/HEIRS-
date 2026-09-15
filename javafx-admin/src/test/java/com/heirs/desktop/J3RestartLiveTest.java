package com.heirs.desktop;

import com.heirs.desktop.controller.MainController;
import com.heirs.desktop.navigation.ViewType;
import com.heirs.desktop.util.JsonUtil;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.nio.file.Files;
import static com.heirs.desktop.FxTestSupport.*;
import static com.heirs.desktop.J3LiveTest.*;
import static org.junit.jupiter.api.Assertions.*;

/** Run in a second Maven/JVM invocation after J3LiveTest. */
@EnabledIfSystemProperty(named="heirs.restart", matches="true")
class J3RestartLiveTest {
    @Test void persistedRecordSurvivesClientProcessRestartThenDeleteThroughUi() throws Exception {
        var state = JsonUtil.getMapper().readTree(Files.readString(VERIFY.resolve("j3-state.json")));
        String reference = state.get("reference").asText();
        long id = state.get("id").asLong(), baseline = state.get("baseline").asLong();
        assertTrue(reference.startsWith("TEST/JFX/2026/"));
        assertEquals(reference, get("records/" + id).get("referenceNumber").asText());
        start(); var stage = openMain();
        try {
            fx(() -> { ((MainController)stage.getUserData()).getNavigator().navigateTo(ViewType.RECORDS); return null; });
            select(stage, reference);
            assertEquals("JavaFX CRUD Verification Record - Reconnected", fx(() -> row(stage, reference).getTitle()));
            snapshot(stage.getScene().getRoot(), "j3-restart-persistence");
            fire(stage.getScene().getRoot(), "toolDelete"); click(waitDialog("Delete Record"), "Delete");
            until(() -> row(stage, reference) == null);
            assertEquals(404, http("GET", "records/" + id).statusCode());
            assertEquals(baseline, get("records?size=1").get("totalElements").asLong());
            fx(() -> { ((MainController)stage.getUserData()).getNavigator().navigateTo(ViewType.DASHBOARD); return null; });
            until(() -> ((Label)stage.getScene().lookup("#lblTotalRecords")).getText().equals(Long.toString(baseline)));
            System.out.println("J3 PASS: fresh client JVM loaded persisted edit; UI DELETE; independent 404; original count restored");
        } finally { closeWindows(); }
    }
}
