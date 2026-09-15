package com.heirs.desktop.api;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.heirs.desktop.config.ApiConfig;
import com.heirs.desktop.dto.ApiErrorResponse;
import com.heirs.desktop.util.JsonUtil;

/**
 * Reusable HTTP REST client using Java 21 java.net.http.HttpClient.
 */
public class ApiClient {

    private final HttpClient httpClient;

    public ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(ApiConfig.getConnectTimeout())
                .build();
    }

    public ApiClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public <T> T get(String endpointPath, Class<T> responseClass) {
        String json = executeGet(endpointPath);
        try {
            return JsonUtil.fromJson(json, responseClass);
        } catch (Exception e) {
            throw ApiException.clientError("Unable to read the backend response. Refresh to check the record before retrying.", e);
        }
    }

    public <T> T get(String endpointPath, TypeReference<T> responseType) {
        String json = executeGet(endpointPath);
        try {
            return JsonUtil.fromJson(json, responseType);
        } catch (Exception e) {
            throw ApiException.clientError("Unable to read the backend response. Refresh to check the record before retrying.", e);
        }
    }

    public String executeGet(String endpointPath) {
        String url = buildUrl(endpointPath);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(ApiConfig.getRequestTimeout())
                .header("Accept", "application/json")
                .GET()
                .build();

        return executeRequest(request);
    }

    public <T> T post(String endpointPath, Object requestBody, Class<T> responseClass) {
        String url = buildUrl(endpointPath);
        String bodyJson;
        try {
            bodyJson = JsonUtil.toJson(requestBody);
        } catch (Exception e) {
            throw ApiException.clientError("Unable to prepare the record for saving.", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(ApiConfig.getRequestTimeout())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson != null ? bodyJson : ""))
                .build();

        String responseJson = executeRequest(request);
        if (responseClass == null || Void.class.equals(responseClass)) {
            return null;
        }
        try {
            return JsonUtil.fromJson(responseJson, responseClass);
        } catch (Exception e) {
            throw ApiException.clientError("Unable to read the backend response. Refresh to check the record before retrying.", e);
        }
    }

    public <T> T put(String endpointPath, Object requestBody, Class<T> responseClass) {
        String url = buildUrl(endpointPath);
        String bodyJson;
        try {
            bodyJson = JsonUtil.toJson(requestBody);
        } catch (Exception e) {
            throw ApiException.clientError("Unable to prepare the record for saving.", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(ApiConfig.getRequestTimeout())
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(bodyJson != null ? bodyJson : ""))
                .build();

        String responseJson = executeRequest(request);
        if (responseClass == null || Void.class.equals(responseClass)) {
            return null;
        }
        try {
            return JsonUtil.fromJson(responseJson, responseClass);
        } catch (Exception e) {
            throw ApiException.clientError("Unable to read the backend response. Refresh to check the record before retrying.", e);
        }
    }

    public void delete(String endpointPath) {
        String url = buildUrl(endpointPath);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(ApiConfig.getRequestTimeout())
                .header("Accept", "application/json")
                .DELETE()
                .build();

        executeRequest(request);
    }

    public <T> T postMultipart(String endpointPath, MultipartBody multipartBody, TypeReference<T> responseType) {
        String responseJson = executeMultipart("POST", endpointPath, multipartBody);
        return parseResponse(responseJson, responseType);
    }

    public <T> T putMultipart(String endpointPath, MultipartBody multipartBody, TypeReference<T> responseType) {
        String responseJson = executeMultipart("PUT", endpointPath, multipartBody);
        return parseResponse(responseJson, responseType);
    }

    /**
     * Fetches a binary document (preview/download) with inline error mapping.
     * The fallback file name is used when the backend does not send a
     * Content-Disposition header.
     */
    public BinaryResponse getBinary(String endpointPath, String fallbackFileName) {
        String url = buildUrl(endpointPath);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(ApiConfig.getRequestTimeout())
                .header("Accept", "application/pdf, application/octet-stream, */*")
                .GET()
                .build();
        return executeBinaryRequest(request, fallbackFileName);
    }

    private String executeMultipart(String method, String endpointPath, MultipartBody multipartBody) {
        String url = buildUrl(endpointPath);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(ApiConfig.getRequestTimeout())
                .header("Accept", "application/json")
                .header("Content-Type", multipartBody.contentType());
        HttpRequest request = "PUT".equalsIgnoreCase(method)
                ? builder.PUT(HttpRequest.BodyPublishers.ofByteArray(multipartBody.bytes())).build()
                : builder.POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody.bytes())).build();

        return executeRequest(request);
    }

    private <T> T parseResponse(String responseJson, TypeReference<T> responseType) {
        if (responseJson == null || responseJson.isBlank()) {
            return null;
        }
        try {
            return JsonUtil.fromJson(responseJson, responseType);
        } catch (Exception e) {
            throw ApiException.clientError("Unable to read the backend response. Refresh to check the record before retrying.", e);
        }
    }

    private BinaryResponse executeBinaryRequest(HttpRequest request, String fallbackFileName) {
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            byte[] body = response.body();

            if (status >= 200 && status < 300) {
                String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(body != null ? body.length : 0);
                String fileName = com.heirs.desktop.util.FileNames.parseContentDispositionFilename(
                        response.headers().firstValue("Content-Disposition").orElse(null));
                if (fileName == null || fileName.isBlank()) {
                    fileName = fallbackFileName;
                }
                return new BinaryResponse(body, contentType, contentLength,
                        com.heirs.desktop.util.FileNames.safeFileName(fileName));
            }

            ApiErrorResponse err = null;
            String bodyStr = new String(body != null ? body : new byte[0], java.nio.charset.StandardCharsets.UTF_8);
            if (!bodyStr.isBlank()) {
                try {
                    err = JsonUtil.fromJson(bodyStr, ApiErrorResponse.class);
                } catch (Exception ignored) {
                }
            }
            throw ApiException.fromErrorResponse(status, err);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.clientError("Operation interrupted", e);
        } catch (IOException e) {
            throw mapNetworkException(e);
        }
    }

    private String executeRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                return response.body();
            }

            // Error response handling
            ApiErrorResponse err = null;
            if (response.body() != null && !response.body().isBlank()) {
                try {
                    err = JsonUtil.fromJson(response.body(), ApiErrorResponse.class);
                } catch (Exception ignored) {
                }
            }

            throw ApiException.fromErrorResponse(status, err);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.clientError("Operation interrupted", e);
        } catch (IOException e) {
            throw mapNetworkException(e);
        }
    }

    private ApiException mapNetworkException(Exception e) {
        if (e instanceof ConnectException || e instanceof HttpConnectTimeoutException) {
            return ApiException.offline("Unable to connect to HEIRS backend server at " + ApiConfig.getBaseUrl() + ". Is the service running?", e);
        }
        if (e instanceof HttpTimeoutException) {
            return ApiException.offline("Backend request timed out after " + ApiConfig.getRequestTimeout().toSeconds() + " seconds.", e);
        }
        return ApiException.offline("Network error communicating with HEIRS backend: " + e.getMessage(), e);
    }

    private String buildUrl(String endpointPath) {
        String base = ApiConfig.getBaseUrl();
        if (endpointPath == null || endpointPath.isBlank()) {
            return base;
        }
        if (!endpointPath.startsWith("/")) {
            return base + "/" + endpointPath;
        }
        return base + endpointPath;
    }
}
