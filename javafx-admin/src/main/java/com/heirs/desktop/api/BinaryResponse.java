package com.heirs.desktop.api;

/**
 * Result of a binary document fetch (preview or download), carrying the raw
 * bytes plus the response metadata needed by the UI layer.
 *
 * @param body          raw response bytes
 * @param contentType   Content-Type as returned by the backend
 * @param contentLength byte count (from Content-Length when present)
 * @param fileName      safe basename derived from Content-Disposition, falling
 *                      back to the document's original file name
 */
public record BinaryResponse(byte[] body, String contentType, long contentLength, String fileName) {

    public boolean isEmpty() {
        return body == null || body.length == 0;
    }
}