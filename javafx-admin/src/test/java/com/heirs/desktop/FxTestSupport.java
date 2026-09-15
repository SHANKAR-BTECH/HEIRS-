package com.heirs.desktop;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

/** Real JavaFX toolkit and FXML; no substituted controllers or HTTP clients. */
public final class FxTestSupport {
    public static void start() throws Exception {
        var ready = new CountDownLatch(1);
        try { Platform.startup(ready::countDown); }
        catch (IllegalStateException alreadyStarted) { ready.countDown(); }
        assertTrue(ready.await(10, TimeUnit.SECONDS));
        fx(() -> { Platform.setImplicitExit(false); return null; });
    }
    public static <T> T fx(Callable<T> action) throws Exception {
        var task = new FutureTask<T>(() -> {
            for (Window window : java.util.List.copyOf(Window.getWindows())) {
                if (window.getScene() != null) {
                    window.getScene().getRoot().applyCss();
                    window.getScene().getRoot().layout();
                }
            }
            return action.call();
        });
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
    public static void until(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (!fx(condition::getAsBoolean)) {
            if (System.nanoTime() > deadline) fail("Timed out waiting for JavaFX state");
            Thread.sleep(50);
        }
    }
    public static Stage openMain() throws Exception {
        return fx(() -> {
            var loader = new FXMLLoader(FxTestSupport.class.getResource("/com/heirs/desktop/fxml/main.fxml"));
            Parent root = loader.load();
            var stage = new Stage();
            stage.setUserData(loader.getController());
            stage.setScene(new Scene(root, 1280, 800));
            for (String css : new String[]{"base", "menu-toolbar", "navigation", "tables", "forms", "dialogs"})
                stage.getScene().getStylesheets().add(FxTestSupport.class.getResource("/com/heirs/desktop/css/" + css + ".css").toExternalForm());
            stage.show();
            return stage;
        });
    }
    public static void closeWindows() throws Exception {
        fx(() -> { for (Window w : java.util.List.copyOf(Window.getWindows())) w.hide(); return null; });
    }
    public static Button button(Parent root, String text) {
        return root.lookupAll(".button").stream().filter(n -> n instanceof Button b && text.equals(b.getText()))
                .map(n -> (Button)n).findFirst().orElseThrow();
    }
}
