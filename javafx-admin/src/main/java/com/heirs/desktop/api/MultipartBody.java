package com.heirs.desktop.api;

/**
 * A fully serialized {@code multipart/form-data} request body together with
 * the exact Content-Type header value (including the boundary) that must be
 * sent with it.
 */
public record MultipartBody(byte[] bytes, String contentType) {
}