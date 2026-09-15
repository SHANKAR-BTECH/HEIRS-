package com.heirs.desktop;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.imageio.ImageIO;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Dialog;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.heirs.desktop.controller.MainController;
import com.heirs.desktop.model.AddRecordDialog;
import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.ViewType;

/**
 * HEIRS — Desktop Administration (Phase J3 Record CRUD).
 *
 * Boots the JavaFX runtime, builds the single main window from main.fxml,
 * attaches the modular stylesheet set and shows the Dashboard by default.
 *
 * All record reads and writes use the Spring Boot REST API asynchronously.
 * No database drivers or direct database access are included in this module.
 *
 * <p>Self-driving "smoke" mode (screenshots + navigation check) is enabled
 * with the environment variable {@code HEIRS_SMOKE=true}. The screenshots
 * are written to {@code HEIRS_SHOTS_DIR} (default {@code target/smoke/shots}).
 */
public class HeirsDesktopApplication extends Application {

    private static final String APP_TITLE = "HEIRS \u2014 Desktop Administration";

    private static final String[] STYLESHEETS = {
            "/com/heirs/desktop/css/base.css",
            "/com/heirs/desktop/css/menu-toolbar.css",
            "/com/heirs/desktop/css/navigation.css",
            "/com/heirs/desktop/css/tables.css",
            "/com/heirs/desktop/css/forms.css",
            "/com/heirs/desktop/css/dialogs.css"
    };

    private boolean smokeMode;
    private Path shotsDir;
    private Navigator navigator;
    private Stage primaryStage;

    /** Exit code used when running in self-driving smoke verification mode. */
    private static int smokeExitCode = 0;

    @Override
    public void start(Stage stage) throws IOException {
        smokeMode = isSmokeMode();
        shotsDir = resolveShotsDir();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/heirs/desktop/fxml/main.fxml"));
        Parent root = loader.load();

        MainController mainController = loader.getController();
        navigator = mainController.getNavigator();
        navigator.setHostServices(getHostServices());

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().addAll(STYLESHEETS);

        stage.setTitle(APP_TITLE);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.centerOnScreen();
        primaryStage = stage;
        stage.show();

        if (smokeMode) {
            runSmokeSequence();
        }
    }

    @Override
    public void stop() {
        com.heirs.desktop.util.FxAsync.shutdown();
        if (smokeMode) {
            System.exit(smokeExitCode);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    /* ------------------------------------------------------------------ */
    /* Smoke mode — self-driving verification + screenshots. Not used in   */
    /* normal operation.                                                   */
    /* ------------------------------------------------------------------ */

    private boolean isSmokeMode() {
        String env = System.getenv("HEIRS_SMOKE");
        return "true".equalsIgnoreCase(env) || Boolean.getBoolean("heirs.smoke");
    }

    private Path resolveShotsDir() {
        String override = System.getenv("HEIRS_SHOTS_DIR");
        Path dir = (override != null && !override.isBlank())
                ? Paths.get(override)
                : Paths.get("target", "smoke", "shots");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create screenshot directory " + dir, e);
        }
        return dir;
    }

    private void runSmokeSequence() {
        List<ViewType> views = List.of(
                ViewType.DASHBOARD,
                ViewType.SEARCH,
                ViewType.RECORDS,
                ViewType.DOCUMENTS,
                ViewType.REPORTS,
                ViewType.ABOUT);

        PauseTransition initial = new PauseTransition(Duration.millis(1500));
        initial.setOnFinished(e -> stepSmoke(0, views));
        initial.play();
    }

    private void stepSmoke(int index, List<ViewType> views) {
        try {
            if (index >= views.size()) {
                smokeDialogs();
                return;
            }
            ViewType view = views.get(index);
            navigator.navigateTo(view);
            PauseTransition wait = new PauseTransition(Duration.millis(700));
            wait.setOnFinished(e -> {
                capture(view.name().toLowerCase(), primaryStage.getScene());
                System.out.println("[SMOKE] " + view.name() + " ok");
                stepSmoke(index + 1, views);
            });
            wait.play();
        } catch (Throwable t) {
            failSmoke(t);
        }
    }

    private void smokeDialogs() {
        try {
            Dialog<?> dialog = AddRecordDialog.build(primaryStage);
            dialog.show();
            PauseTransition wait = new PauseTransition(Duration.millis(800));
            wait.setOnFinished(e -> {
                capture("add-record", dialog.getDialogPane().getScene());
                AddRecordDialog.closeLast();
                System.out.println("[SMOKE] add-record dialog ok");
                finishSmoke();
            });
            wait.play();
        } catch (Throwable t) {
            failSmoke(t);
        }
    }

    private void finishSmoke() {
        System.out.println("[SMOKE] HEIRS desktop smoke test complete");
        Platform.exit();
    }

    private void failSmoke(Throwable t) {
        smokeExitCode = 1;
        t.printStackTrace();
        System.err.println("[SMOKE] HEIRS desktop smoke test FAILED");
        Platform.exit();
    }

    private void capture(String name, Scene scene) {
        try {
            double w = Math.max(scene.getWidth(), 1);
            double h = Math.max(scene.getHeight(), 1);
            WritableImage image = new WritableImage((int) w, (int) h);
            scene.getRoot().snapshot(new SnapshotParameters(), image);
            BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);
            ImageIO.write(buffered, "png", shotsDir.resolve(name + ".png").toFile());
            System.out.println("[SMOKE] screenshot " + name + ".png -> " + shotsDir);
        } catch (IOException e) {
            System.err.println("[SMOKE] unable to capture " + name + ": " + e.getMessage());
        }
    }
}