package com.heirs.desktop.navigation;

/**
 * Implemented by a sub-view controller so that {@link Navigator} can inject
 * itself during load. Useful when a child view needs to trigger navigation
 * or hand off context (e.g., opening a dialog).
 */
public interface NavigatorAware {

    void setNavigator(Navigator navigator);
}