package com.heirs.desktop.util;

import java.util.List;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Reusable UI factory methods shared across the J1 screens.
 * Keeps controllers short and the visual contract in one place.
 */
public final class UiBuilders {

    private UiBuilders() {
    }

    /* ------------------------------------------------------------------ */
    /* Icons                                                               */
    /* ------------------------------------------------------------------ */

    /** Convenience icon initializer – sets graphic, size and optional extra class. */
    public static FontIcon icon(FontAwesomeSolid glyph, int size, String... cssClasses) {
        FontIcon icon = new FontIcon(glyph);
        icon.setIconSize(size);
        for (String c : cssClasses) {
            icon.getStyleClass().add(c);
        }
        return icon;
    }

    public static void setIcon(Label label, FontAwesomeSolid glyph, int size) {
        label.setGraphic(icon(glyph, size));
    }

    /* ------------------------------------------------------------------ */
    /* Buttons                                                             */
    /* ------------------------------------------------------------------ */

    public static Button buttonPrimary(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "button-primary");
        return b;
    }

    public static Button buttonSecondary(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "button-secondary");
        return b;
    }

    public static Button buttonGhost(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "button-ghost");
        return b;
    }

    public static Button buttonDanger(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "button-danger");
        return b;
    }

    public static Button buttonSmall(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "button-sm");
        return b;
    }

    public static Button buttonMini(String text) {
        Button b = new Button(text);
        b.getStyleClass().addAll("button", "button-mini");
        return b;
    }

    public static Button buttonMiniIcon(String text, FontAwesomeSolid glyph) {
        Button b = new Button(text, icon(glyph, 13));
        b.getStyleClass().addAll("button", "button-mini");
        return b;
    }

    /* ------------------------------------------------------------------ */
    /* Badges & pills                                                      */
    /* ------------------------------------------------------------------ */

    public static Label badge(String text, String cssOverride) {
        Label l = new Label(text);
        l.getStyleClass().add("plain-label");
        return l;
    }

    public static Label categoryBadge(String category) {
        return new Label(category);
    }

    public static Label statusPill(String status) {
        return new Label(status);
    }

    public static Label chip(String text) {
        return new Label(text);
    }

    public static Label chipHighlight(String text, String cssClass) {
        return new Label(text);
    }

    /* ------------------------------------------------------------------ */
    /* Status / chapter tokens                                              */
    /* ------------------------------------------------------------------ */

    public static HBox statusDotRow(String text) {
        Region dot = new Region();
        dot.getStyleClass().add("status-dot");
        Label label = new Label(text);
        label.getStyleClass().add("meta");
        HBox box = new HBox(8, dot, label);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /* ------------------------------------------------------------------ */
    /* Metric card                                                          */
    /* ------------------------------------------------------------------ */

    /** Assembles the small metric cards used on the Dashboard. */
    public static VBox metricCard(String title,
                                  String value,
                                  String hint,
                                  FontAwesomeSolid glyph,
                                  String valueColor) {
        FontIcon icon = icon(glyph, 18);
        icon.getStyleClass().add("metric-icon-glyph");

        Region iconTile = new Region();
        iconTile.getStyleClass().addAll("metric-icon", "metric-icon-" + title.toLowerCase().replace(" ", "-"));
        StackPane graphic = new StackPane(iconTile, icon);
        graphic.setAlignment(Pos.CENTER);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("meta");
        HBox header = new HBox(10, graphic, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("metric-value");
        if (valueColor != null && !valueColor.isBlank()) {
            valueLabel.setStyle("-fx-text-fill: " + valueColor + ";");
        }

        Label hintLabel = new Label(hint);
        hintLabel.getStyleClass().add("hint");

        VBox card = new VBox(8, header, valueLabel, hintLabel);
        card.getStyleClass().addAll("card", "metric-card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /* ------------------------------------------------------------------ */
    /* Summary chips (Reports screen)                                      */
    /* ------------------------------------------------------------------ */

    public static VBox summaryChip(String label, String value, String textColor, String accentColor) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("metric-value", "summary-chip-value");
        valueLabel.setStyle("-fx-text-fill: " + accentColor + ";");

        Label labelNode = new Label(label);
        labelNode.getStyleClass().addAll("meta", "summary-chip-label");

        VBox chip = new VBox(4, labelNode, valueLabel);
        chip.getStyleClass().addAll("card", "summary-chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setMinWidth(132);
        return chip;
    }

    /* ------------------------------------------------------------------ */
    /* Placeholder / empty / loading states                                 */
    /* ------------------------------------------------------------------ */

    public static void placeholderAlert(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("Coming soon \u2014 Phase J4");
        alert.setHeaderText("This feature will be connected in Phase J4");
        alert.setContentText(message);
        alert.getDialogPane().getStyleClass().add("heirs-dialog");
        for (String sheet : List.of(
                "/com/heirs/desktop/css/base.css",
                "/com/heirs/desktop/css/forms.css",
                "/com/heirs/desktop/css/dialogs.css")) {
            alert.getDialogPane().getStylesheets().addAll(sheet);
        }
        alert.showAndWait();
    }

    public static VBox emptyState(String title, String body) {
        FontIcon icon = icon(FontAwesomeSolid.INBOX, 36);
        icon.getStyleClass().add("state-icon");
        Label titleL = new Label(title);
        titleL.getStyleClass().addAll("section-title");
        Label bodyL = new Label(body);
        bodyL.getStyleClass().addAll("meta");
        bodyL.setWrapText(true);
        bodyL.setMaxWidth(360);
        bodyL.setAlignment(Pos.CENTER);
        titleL.setAlignment(Pos.CENTER);
        VBox v = new VBox(10, icon, titleL, bodyL);
        v.getStyleClass().add("empty-view");
        v.setAlignment(Pos.CENTER);
        return v;
    }

    public static VBox errorState(String title, String body, Runnable onRetry) {
        FontIcon icon = icon(FontAwesomeSolid.EXCLAMATION_TRIANGLE, 36);
        icon.getStyleClass().add("state-icon");
        Label titleL = new Label(title);
        titleL.getStyleClass().addAll("section-title");
        Label bodyL = new Label(body);
        bodyL.getStyleClass().addAll("meta");
        bodyL.setWrapText(true);
        bodyL.setAlignment(Pos.CENTER);
        titleL.setAlignment(Pos.CENTER);
        Button retry = buttonSecondary("Retry");
        if (onRetry != null) {
            retry.setOnAction(e -> onRetry.run());
        }
        VBox v = new VBox(10, icon, titleL, bodyL, retry);
        v.getStyleClass().add("error-view");
        v.setAlignment(Pos.CENTER);
        return v;
    }

    public static VBox loadingState() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(32, 32);
        spinner.getStyleClass().add("loading-spinner");
        Label label = new Label("Loading…");
        label.getStyleClass().add("meta");
        VBox v = new VBox(10, spinner, label);
        v.getStyleClass().add("loading-view");
        v.setAlignment(Pos.CENTER);
        return v;
    }
}