package com.heirs.desktop;

import com.heirs.desktop.config.ApiConfig;
import com.heirs.desktop.controller.MainController;
import com.heirs.desktop.model.Record;
import com.heirs.desktop.navigation.ViewType;
import com.heirs.desktop.state.AppState;
import com.heirs.desktop.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.List;
import static com.heirs.desktop.FxTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in real MySQL test. All mutations are limited to newly generated TEST/JFX references. */
@EnabledIfSystemProperty(named="heirs.live", matches="true")
class J3LiveTest {
    static final Path VERIFY = Path.of("verification");
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static HttpResponse<String> http(String method, String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080/api/" + path))
                .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }
    static JsonNode get(String path) throws Exception {
        var response = http("GET", path); assertEquals(200, response.statusCode());
        return JsonUtil.getMapper().readTree(response.body());
    }
    static Parent dialog(String title) {
        return Window.getWindows().stream().filter(w -> w instanceof Stage s && s.getTitle() != null && s.getTitle().startsWith(title) && w.isShowing())
                .map(w -> w.getScene().getRoot()).findFirst().orElse(null);
    }
    static Parent waitDialog(String title) throws Exception {
        until(() -> dialog(title) != null); return fx(() -> dialog(title));
    }
    static void fire(Parent root, String id) throws Exception { fx(() -> { ((Button)root.lookup("#" + id)).fire(); return null; }); }
    static void click(Parent root, String text) throws Exception { fx(() -> { button(root, text).fire(); return null; }); }
    static void text(Parent root, String id, String value) throws Exception {
        fx(() -> { ((TextInputControl)root.lookup("#" + id)).setText(value); return null; });
    }
    static void fill(Parent root, String reference) throws Exception {
        text(root, "referenceField", reference);
        text(root, "titleField", "JavaFX CRUD Verification Record");
        text(root, "departmentField", "JavaFX Verification Department");
        text(root, "yearField", "2026");
        text(root, "sourceField", "Disposable J3 live verification");
        text(root, "descriptionField", "Dedicated disposable JavaFX CRUD verification record.");
        text(root, "keywordsField", "Digital Learning, Infrastructure, Universities");
        fx(() -> {
            ((ComboBox<String>)root.lookup("#categoryField")).setValue("Policy");
            ((DatePicker)root.lookup("#publishDateField")).setValue(java.time.LocalDate.of(2026,9,15));
            return null;
        });
    }
    static void snapshot(Parent root, String name) throws Exception {
        fx(() -> {
            Files.createDirectories(VERIFY);
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(root.snapshot(null, null), null), "png", VERIFY.resolve(name + ".png").toFile());
            return null;
        });
    }
    static TableView<Record> table(Stage stage) { return (TableView<Record>)stage.getScene().lookup("#tableView"); }
    static Record row(Stage stage, String ref) { return table(stage).getItems().stream().filter(r -> r.getReference().equals(ref)).findFirst().orElse(null); }
    static void select(Stage stage, String ref) throws Exception {
        until(() -> row(stage, ref) != null);
        fx(() -> { table(stage).getSelectionModel().select(row(stage, ref)); return null; });
    }
    static void save(Parent form, String title) throws Exception {
        fx(() -> {
            Button b = (Button)form.lookup("#saveRecord");
            b.fire(); assertTrue(b.isDisabled(), "Save disabled synchronously");
            b.fire(); // Must not submit again.
            assertEquals("Saving...", ((Label)form.lookup("#formMessage")).getText());
            return null;
        });
        until(() -> dialog(title) == null);
    }
    static void error(Parent form, String field) throws Exception {
        try { until(() -> form.lookup("#" + field + "Error").isVisible() && !form.lookup("#saveRecord").isDisabled()); }
        catch (AssertionError failure) {
            snapshot(form, "j3-failed-" + field);
            System.out.println(fx(() -> form.lookupAll(".label").stream().map(n -> ((Label)n).getText()).toList()));
            throw failure;
        }
    }
    static void marker(String name) throws Exception {
        Files.writeString(VERIFY.resolve(name), "ready");
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(120);
        while (!Files.exists(VERIFY.resolve(name + ".continue"))) {
            if (System.nanoTime() > deadline) fail("Awaiting backend coordinator: " + name);
            Thread.sleep(100);
        }
    }
    @Test void realFormsCrudErrorsAndOfflineRetry() throws Exception {
        start(); Files.createDirectories(VERIFY);
        String prefix = "TEST/JFX/2026/" + System.currentTimeMillis();
        String a = prefix + "/A", b = prefix + "/B", c = prefix + "/C";
        long baseline = get("records?size=1").get("totalElements").asLong();
        Files.writeString(VERIFY.resolve("j3-state.json"), "{\"reference\":\"" + a + "\",\"baseline\":" + baseline + "}");
        Stage stage = openMain();
        boolean keepForRestart = false;
        try {
            until(() -> AppState.getInstance().isBackendConnected());
            fx(() -> { ((MainController)stage.getUserData()).getNavigator().navigateTo(ViewType.RECORDS); return null; });
            until(() -> table(stage).getItems().size() == baseline);
            assertTrue(fx(() -> stage.getScene().lookup("#toolEdit").isDisabled()));
            fire(stage.getScene().getRoot(), "toolNew");
            Parent form = waitDialog("Add Record");
            fire(form, "saveRecord"); error(form, "title"); error(form, "referenceNumber");
            fill(form, a);
            text(form, "yearField", "2101"); fire(form, "saveRecord"); error(form, "publicationYear");
            text(form, "yearField", "2026");
            final Parent dateForm = form;
            fx(() -> { ((DatePicker)dateForm.lookup("#publishDateField")).getEditor().setText("2026-02-30"); return null; });
            fire(form, "saveRecord"); error(form, "publishedDate");
            fx(() -> { ((DatePicker)dateForm.lookup("#publishDateField")).getEditor().setText("2026-09-15"); return null; });
            text(form, "sourceField", "x".repeat(501)); fire(form, "saveRecord"); error(form, "source");
            text(form, "sourceField", "Disposable J3 live verification");
            snapshot(form, "j3-validation");
            save(form, "Add Record");
            select(stage, a);
            long id = fx(() -> row(stage, a).getId());
            var created = get("records/" + id);
            assertEquals(a, created.get("referenceNumber").asText());
            assertEquals("2026-09-15", created.get("publishedDate").asText());
            assertEquals(baseline + 1, get("records?size=1").get("totalElements").asLong());
            Files.writeString(VERIFY.resolve("j3-state.json"), "{\"reference\":\"" + a + "\",\"id\":" + id + ",\"baseline\":" + baseline + "}");
            fire(stage.getScene().getRoot(), "btnRefreshRecords"); select(stage, a);
            System.out.println("J3 PASS: CREATE and independent GET; double-submit prevented; refresh persistence");

            fire(stage.getScene().getRoot(), "btnNewRecord"); form = waitDialog("Add Record"); fill(form, a);
            fire(form, "saveRecord"); error(form, "referenceNumber");
            snapshot(form, "j3-duplicate-create");
            text(form, "referenceField", b); save(form, "Add Record"); select(stage, b);
            long bId = fx(() -> row(stage, b).getId());
            select(stage, a); fire(stage.getScene().getRoot(), "toolEdit"); form = waitDialog("Edit Record");
            text(form, "referenceField", b); fire(form, "saveRecord"); error(form, "referenceNumber");
            assertEquals(a, get("records/" + id).get("referenceNumber").asText());
            text(form, "referenceField", a); text(form, "titleField", "JavaFX CRUD Verification Record - Edited");
            text(form, "descriptionField", "Edited description persisted through JavaFX PUT.");
            Parent edit = form;
            fx(() -> { ((ComboBox<String>)edit.lookup("#statusField")).setValue("Completed"); return null; });
            save(form, "Edit Record");
            until(() -> row(stage, a) != null && row(stage, a).getStatus().equals("Completed"));
            assertEquals("Completed", get("records/" + id).get("status").asText());
            select(stage, a); fire(stage.getScene().getRoot(), "btnViewRecord");
            Parent details = waitDialog("Record Details");
            assertEquals("JavaFX CRUD Verification Record - Edited", fx(() -> ((TextField)details.lookup("#titleField")).getText()));
            snapshot(details, "j3-details"); click(details, "Close");
            System.out.println("J3 PASS: duplicate CREATE/UPDATE retained input; EDIT and independent GET; fresh details");

            select(stage, b); fire(stage.getScene().getRoot(), "btnEditRecord"); form = waitDialog("Edit Record");
            text(form, "sourceField", "x".repeat(501)); fire(form, "saveRecord");
            Parent invalidUpdate = form;
            // Spring's PUT method-validation handler currently reports key "request", not "source".
            until(() -> ((Label)invalidUpdate.lookup("#formMessage")).getText().contains("size must be between 0 and 500")
                    && !invalidUpdate.lookup("#saveRecord").isDisabled());
            assertEquals("Disposable J3 live verification", get("records/" + bId).get("source").asText());
            click(form, "Cancel");
            fire(stage.getScene().getRoot(), "toolDelete");
            Parent confirm = waitDialog("Delete Record"); snapshot(confirm, "j3-delete-confirmation");
            assertTrue(fx(() -> ((DialogPane)confirm).getContentText().contains("This action cannot be undone.")));
            click(confirm, "Cancel"); assertEquals(200, http("GET", "records/" + bId).statusCode());

            // Transport outage during DELETE: preserve the real row and recover via the same action.
            ApiConfig.setOverrideBaseUrl("http://127.0.0.1:1");
            fire(stage.getScene().getRoot(), "toolDelete"); click(waitDialog("Delete Record"), "Delete");
            Parent failure = waitDialog("Record Operation");
            assertFalse(fx(() -> AppState.getInstance().isBackendConnected()));
            assertNotNull(fx(() -> row(stage, b))); click(failure, "OK");
            ApiConfig.setOverrideBaseUrl(null);
            assertEquals(204, http("DELETE", "records/" + bId).statusCode());
            fire(stage.getScene().getRoot(), "toolDelete"); click(waitDialog("Delete Record"), "Delete");
            failure = waitDialog("Record Operation");
            Parent stale = failure;
            assertTrue(fx(() -> ((DialogPane)stale).getContentText().contains("no longer exists")));
            click(failure, "OK"); until(() -> row(stage, b) == null);
            System.out.println("J3 PASS: backend 400 field mapping; Cancel; offline DELETE retains row; stale DELETE 404 refresh");

            select(stage, a); fire(stage.getScene().getRoot(), "toolEdit"); form = waitDialog("Edit Record");
            text(form, "titleField", "JavaFX CRUD Verification Record - Reconnected");
            if (Boolean.getBoolean("heirs.coordinatedOutage")) marker("stop-backend");
            else ApiConfig.setOverrideBaseUrl("http://127.0.0.1:1");
            fire(form, "saveRecord");
            Parent offline = form;
            until(() -> !offline.lookup("#saveRecord").isDisabled() && ((Label)offline.lookup("#formMessage")).getText().contains("Backend Offline"));
            assertEquals("JavaFX CRUD Verification Record - Reconnected", fx(() -> ((TextField)offline.lookup("#titleField")).getText()));
            assertEquals("JavaFX CRUD Verification Record - Edited", fx(() -> row(stage, a).getTitle()));
            snapshot(form, "j3-offline-save");
            if (Boolean.getBoolean("heirs.coordinatedOutage")) marker("start-backend");
            else ApiConfig.setOverrideBaseUrl(null);
            save(form, "Edit Record");
            assertEquals("JavaFX CRUD Verification Record - Reconnected", get("records/" + id).get("title").asText());
            System.out.println("J3 PASS: backend-off SAVE keeps input and table; reconnect Save retry persists");

            // Search remains active across a toolbar create and is rerun with the same criteria.
            fx(() -> { ((MainController)stage.getUserData()).getNavigator().navigateTo(ViewType.SEARCH); return null; });
            text(stage.getScene().getRoot(), "searchField", prefix);
            fire(stage.getScene().getRoot(), "searchButton");
            until(() -> !stage.getScene().lookup("#searchButton").isDisabled());
            fire(stage.getScene().getRoot(), "toolNew"); form = waitDialog("Add Record"); fill(form, c); save(form, "Add Record");
            until(() -> ((TableView<?>)stage.getScene().lookup("#resultsTable")).getItems().size() == 2);
            fx(() -> { ((MainController)stage.getUserData()).getNavigator().navigateTo(ViewType.DASHBOARD); return null; });
            until(() -> ((Label)stage.getScene().lookup("#lblTotalRecords")).getText().equals(Long.toString(baseline + 2)));
            snapshot(stage.getScene().getRoot(), "j3-dashboard");
            fx(() -> { ((MainController)stage.getUserData()).getNavigator().navigateTo(ViewType.RECORDS); return null; });
            select(stage, c); long cId = fx(() -> row(stage, c).getId());
            fire(stage.getScene().getRoot(), "btnDeleteRecord"); click(waitDialog("Delete Record"), "Delete");
            until(() -> row(stage, c) == null);
            assertEquals(404, http("GET", "records/" + cId).statusCode());
            assertNull(fx(() -> table(stage).getSelectionModel().getSelectedItem()));
            snapshot(stage.getScene().getRoot(), "j3-records");
            System.out.println("J3 PASS: Search refresh; Dashboard counts; confirmed UI DELETE and independent GET 404");
            keepForRestart = true;
        } finally {
            ApiConfig.setOverrideBaseUrl(null); closeWindows();
            // Never touch anything outside this run's exact reference prefix.
            var page = get("records/search?q=" + java.net.URLEncoder.encode(prefix, java.nio.charset.StandardCharsets.UTF_8) + "&size=100");
            for (JsonNode record : page.get("content")) {
                String ref = record.get("referenceNumber").asText();
                if ((ref.equals(a) && !keepForRestart) || ref.equals(b) || ref.equals(c))
                    assertEquals(204, http("DELETE", "records/" + record.get("id").asLong()).statusCode());
            }
        }
    }
}
