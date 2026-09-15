package com.heirs.desktop.util;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Thin wrapper around {@link java.awt.Desktop} native open behavior so the
 * rest of the codebase stays free of AWT references.
 */
public final class DesktopSupport {

    private DesktopSupport() {
    }

    public static boolean canOpenFiles() {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        Desktop desktop = Desktop.getDesktop();
        return desktop.isSupported(Desktop.Action.OPEN);
    }

    public static void openFile(Path file) throws IOException {
        Desktop.getDesktop().open(file.toFile());
    }
}