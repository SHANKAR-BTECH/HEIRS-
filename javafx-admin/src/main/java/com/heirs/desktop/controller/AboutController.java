package com.heirs.desktop.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import com.heirs.desktop.navigation.Navigator;
import com.heirs.desktop.navigation.NavigatorAware;
import com.heirs.desktop.util.UiBuilders;

public class AboutController implements NavigatorAware {

    @FXML private Label brandGlyph;
    @FXML private Label lblJavafxVersion;
    @FXML private Label lblJavaVersion;
    @FXML private Label lblOsInfo;
    @FXML private Hyperlink webDemoLink;

    private Navigator navigator;

    @Override
    public void setNavigator(Navigator navigator) {
        this.navigator = navigator;
    }

    @FXML
    void initialize() {
        UiBuilders.setIcon(brandGlyph, FontAwesomeSolid.GRADUATION_CAP, 38);

        String fxVer = System.getProperty("javafx.version");
        if (fxVer != null && !fxVer.isBlank()) {
            lblJavafxVersion.setText("JavaFX " + fxVer + " Desktop Controls");
        } else {
            lblJavafxVersion.setText("JavaFX 21.0.5 Desktop Controls");
        }

        String javaVer = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        lblJavaVersion.setText("Java " + (javaVer != null ? javaVer : "21") + " (" + (javaVendor != null ? javaVendor : "Oracle") + ")");

        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        lblOsInfo.setText((osName != null ? osName : "Windows") + " (" + (osArch != null ? osArch : "x64") + ")");

        webDemoLink.setText("https://heirs-azure.vercel.app/");
        webDemoLink.setOnAction(e -> {
            if (navigator != null && navigator.hostServices() != null) {
                navigator.hostServices().showDocument("https://heirs-azure.vercel.app/");
            }
        });
    }
}