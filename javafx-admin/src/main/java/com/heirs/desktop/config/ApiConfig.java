package com.heirs.desktop.config;

import java.time.Duration;

/**
 * Centralized configuration for the HEIRS REST API client.
 *
 * <p>Resolution order for the API base URL:
 * <ol>
 *   <li>System/JVM property: {@code -Dheirs.api.baseUrl=http://127.0.0.1:8080}</li>
 *   <li>Environment variable: {@code HEIRS_API_BASE_URL=http://127.0.0.1:8080}</li>
 *   <li>Default fallback: {@code http://127.0.0.1:8080}</li>
 * </ol>
 */
public final class ApiConfig {

    public static final String DEFAULT_BASE_URL = "http://127.0.0.1:8080";
    public static final String SYS_PROP_BASE_URL = "heirs.api.baseUrl";
    public static final String ENV_VAR_BASE_URL = "HEIRS_API_BASE_URL";

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private static String overrideBaseUrl;

    private ApiConfig() {
    }

    /**
     * Resolves the configured base URL, trimming any trailing slashes.
     */
    public static String getBaseUrl() {
        if (overrideBaseUrl != null && !overrideBaseUrl.isBlank()) {
            return stripTrailingSlash(overrideBaseUrl.trim());
        }

        String sysProp = System.getProperty(SYS_PROP_BASE_URL);
        if (sysProp != null && !sysProp.isBlank()) {
            return stripTrailingSlash(sysProp.trim());
        }

        String envVar = System.getenv(ENV_VAR_BASE_URL);
        if (envVar != null && !envVar.isBlank()) {
            return stripTrailingSlash(envVar.trim());
        }

        return DEFAULT_BASE_URL;
    }

    /**
     * Allows programmatic base URL override (useful for integration tests).
     */
    public static void setOverrideBaseUrl(String url) {
        overrideBaseUrl = url;
    }

    public static Duration getConnectTimeout() {
        return DEFAULT_CONNECT_TIMEOUT;
    }

    public static Duration getRequestTimeout() {
        return DEFAULT_REQUEST_TIMEOUT;
    }

    private static String stripTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
