package com.heirs.desktop;

import com.heirs.desktop.controller.MainController;
import com.heirs.desktop.model.Record;
import com.heirs.desktop.navigation.ViewType;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static com.heirs.desktop.FxTestSupport.*;
import static com.heirs.desktop.J3LiveTest.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named="heirs.live", matches="true")
class J3ActionsLiveTest {
    private static void menu(Stage stage, String id) throws Exception {
        fx(() -> {
            ((MenuBar)stage.getScene().lookup("#menuBar")).getMenus().stream()
                    .flatMap(m -> m.getItems().stream()).filter(m -> id.equals(m.getId())).findFirst().orElseThrow().fire();
            return null;
        });
    }
    @Test void menuContextDoubleClickDetailsAndStaleUpdate() throws Exception {
        start(); Stage stage = openMain();
        String reference = "TEST/JFX/2026/ACTIONS/" + System.currentTimeMillis();
        Long id = null;
        try {
            fx(() -> { ((MainController)stage.getUserData()).getNavigator().navigateTo(ViewType.RECORDS); return null; });
            menu(stage, "menuRecordsAdd"); Parent form = waitDialog("Add Record");
            fill(form, reference); click(form, "Cancel");
            menu(stage, "menuRecordsAdd"); form = waitDialog("Add Record");
            Parent fresh = form;
            assertEquals("", fx(() -> ((TextField)fresh.lookup("#titleField")).getText()));
            fill(form, reference); save(form, "Add Record"); select(stage, reference);
            id = fx(() -> row(stage, reference).getId());
            menu(stage, "menuRecordsEdit"); form = waitDialog("Edit Record");
            text(form, "titleField", "Unsaved change"); click(form, "Cancel");
            assertEquals("JavaFX CRUD Verification Record", get("records/" + id).get("title").asText());
            var visualRow = fx(() -> {
                table(stage).scrollTo(table(stage).getSelectionModel().getSelectedIndex());
                table(stage).layout();
                return table(stage).lookupAll(".table-row-cell").stream()
                        .filter(n -> n instanceof TableRow<?> r && r.getItem() instanceof Record record && reference.equals(record.getReference()))
                        .map(n -> (TableRow<Record>)n).findFirst().orElseThrow();
            });
            fx(() -> { visualRow.getContextMenu().getItems().get(1).fire(); return null; });
            form = waitDialog("Edit Record"); text(form, "titleField", "JavaFX context menu edit"); save(form, "Edit Record");
            until(() -> row(stage, reference) != null && row(stage, reference).getTitle().equals("JavaFX context menu edit"));
            select(stage, reference);
            fx(() -> {
                table(stage).scrollTo(table(stage).getSelectionModel().getSelectedIndex()); table(stage).layout();
                var row = table(stage).lookupAll(".table-row-cell").stream()
                        .filter(n -> n instanceof TableRow<?> r && r.getItem() instanceof Record record && reference.equals(record.getReference())).findFirst().orElseThrow();
                row.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 10, 10, 10, 10, MouseButton.PRIMARY, 2,
                        false, false, false, false, false, false, false, false, false, false, null));
                return null;
            });
            Parent details = waitDialog("Record Details");
            assertNull(fx(() -> dialog("Edit Record")), "Double click opens details");
            click(details, "Edit Record..."); form = waitDialog("Edit Record");
            text(form, "titleField", "Attempt stale edit");
            assertEquals(204, http("DELETE", "records/" + id).statusCode());
            fire(form, "saveRecord"); Parent staleForm = form;
            until(() -> ((Label)staleForm.lookup("#formMessage")).getText().contains("no longer exists"));
            assertTrue(fx(() -> staleForm.lookup("#saveRecord").isDisabled()));
            assertEquals("Attempt stale edit", fx(() -> ((TextField)staleForm.lookup("#titleField")).getText()));
            click(form, "Cancel"); until(() -> row(stage, reference) == null);
            assertEquals(404, http("GET", "records/" + id).statusCode());
            // A menu delete uses the same confirmation without relying on toolbar routing.
            menu(stage, "menuRecordsAdd"); form = waitDialog("Add Record"); fill(form, reference); save(form, "Add Record");
            select(stage, reference); id = fx(() -> row(stage, reference).getId());
            menu(stage, "menuRecordsDelete"); click(waitDialog("Delete Record"), "Delete");
            until(() -> row(stage, reference) == null);
            assertEquals(404, http("GET", "records/" + id).statusCode());
            System.out.println("J3 PASS: menu CRUD, context edit, create/edit Cancel, double-click details, details Edit, stale PUT 404 retains values and refreshes");
        } finally {
            closeWindows();
            if (id != null && http("GET", "records/" + id).statusCode() == 200) {
                assertEquals(reference, get("records/" + id).get("referenceNumber").asText());
                assertEquals(204, http("DELETE", "records/" + id).statusCode());
            }
        }
    }
}
