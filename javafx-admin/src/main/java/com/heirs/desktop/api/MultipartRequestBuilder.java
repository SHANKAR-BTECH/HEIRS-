package com.heirs.desktop.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Builds a {@code multipart/form-data} request body from text fields and file
 * parts (RFC 7578). Repeated file parts with the same field name are supported,
 * which is how the HEIRS upload endpoint accepts several files via {@code files}.
 *
 * <p>For the current demo-scale PDFs a fully in-memory implementation is
 * acceptable; large binary streaming is intentionally not over-engineered.</p>
 */
public final class MultipartRequestBuilder {

    private static final byte[] CRLF = {'\r', '\n'};
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final String boundary;
    private final List<byte[]> parts = new ArrayList<>();

    private MultipartRequestBuilder(String boundary) {
        this.boundary = boundary;
    }

    public static MultipartRequestBuilder create() {
        return new MultipartRequestBuilder("heirs-javafx-" + UUID.randomUUID());
    }

    /** Test helper: build with a predictable boundary. */
    public static MultipartRequestBuilder withBoundary(String boundary) {
        return new MultipartRequestBuilder(boundary);
    }

    public MultipartRequestBuilder addField(String name, String value) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + headerSafe(name) + "\"\r\n\r\n";
        parts.add(concat(
                header.getBytes(StandardCharsets.UTF_8),
                (value == null ? "" : value).getBytes(StandardCharsets.UTF_8),
                CRLF));
        return this;
    }

    public MultipartRequestBuilder addFile(String fieldName, Path file) throws IOException {
        return addFile(fieldName, file.getFileName().toString(), detectContentType(file), Files.readAllBytes(file));
    }

    public MultipartRequestBuilder addFile(String fieldName, String fileName, String contentType, byte[] content) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + headerSafe(fieldName) + "\"; filename=\""
                + headerSafe(fileName) + "\"\r\n"
                + "Content-Type: " + (contentType == null || contentType.isBlank() ? DEFAULT_CONTENT_TYPE : contentType)
                + "\r\n\r\n";
        parts.add(concat(
                header.getBytes(StandardCharsets.UTF_8),
                content == null ? new byte[0] : content,
                CRLF));
        return this;
    }

    public MultipartBody build() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        out.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return new MultipartBody(out.toByteArray(), "multipart/form-data; boundary=" + boundary);
    }

    public String boundary() {
        return boundary;
    }

    private static String detectContentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        return DEFAULT_CONTENT_TYPE;
    }

    /** Content is placed into a quoted header value; neutralize line breaks and quotes. */
    private static String headerSafe(String value) {
        return value == null ? "" : value.replace("\"", "_").replace("\r", "_").replace("\n", "_").replace("\\", "_");
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) {
            total += a.length;
        }
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, offset, a.length);
            offset += a.length;
        }
        return result;
    }
}