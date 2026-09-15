package com.heirs.desktop.api;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MultipartRequestBuilderTest {

    @Test
    void buildsRepeatedFilePartsForUploadField() {
        byte[] pdfA = "%PDF-1.4 fake-a".getBytes(StandardCharsets.UTF_8);
        byte[] pdfB = "%PDF-1.4 fake-b".getBytes(StandardCharsets.UTF_8);

        MultipartBody body = MultipartRequestBuilder.withBoundary("testboundary")
                .addFile("files", "j4-test-a.pdf", "application/pdf", pdfA)
                .addFile("files", "j4-test-b.pdf", "application/pdf", pdfB)
                .build();

        String text = new String(body.bytes(), StandardCharsets.UTF_8);
        assertEquals("multipart/form-data; boundary=testboundary", body.contentType());
        assertTrue(text.startsWith("--testboundary\r\n"));
        assertTrue(text.endsWith("--testboundary--\r\n"));
        assertEquals(2, countOccurrences(text, "Content-Disposition: form-data; name=\"files\"; filename=\""));
        assertTrue(text.contains("filename=\"j4-test-a.pdf\""));
        assertTrue(text.contains("filename=\"j4-test-b.pdf\""));
        assertTrue(text.contains("Content-Type: application/pdf"));
        assertTrue(text.contains("fake-a"));
        assertTrue(text.contains("fake-b"));
    }

    @Test
    void buildsSingleReplacementPartWithSingularField() {
        byte[] pdf = "%PDF-1.4 replacement".getBytes(StandardCharsets.UTF_8);

        MultipartBody body = MultipartRequestBuilder.withBoundary("replboundary")
                .addFile("file", "j4-test-replacement.pdf", "application/pdf", pdf)
                .build();

        String text = new String(body.bytes(), StandardCharsets.UTF_8);
        assertTrue(text.contains("name=\"file\""));
        assertFalse(text.contains("name=\"files\""));
        assertTrue(text.contains("filename=\"j4-test-replacement.pdf\""));
    }

    @Test
    void supportsPlainTextFields() {
        MultipartBody body = MultipartRequestBuilder.withBoundary("fieldboundary")
                .addField("note", "hello")
                .build();

        String text = new String(body.bytes(), StandardCharsets.UTF_8);
        assertTrue(text.contains("Content-Disposition: form-data; name=\"note\""));
        assertTrue(text.contains("hello"));
    }

    @Test
    void neutralizesUnsafeCharactersInHeaderValues() {
        byte[] pdf = "%PDF-1.4 x".getBytes(StandardCharsets.UTF_8);

        MultipartBody body = MultipartRequestBuilder.withBoundary("unsafeboundary")
                .addFile("files", "evil\r\nname.pdf", "application/pdf", pdf)
                .build();

        String text = new String(body.bytes(), StandardCharsets.UTF_8);
        assertFalse(text.contains("\r\nname"));
        assertTrue(text.contains("evil__name.pdf"));
    }

    @Test
    void defaultContentTypeWhenNotPdf() {
        MultipartBody body = MultipartRequestBuilder.withBoundary("octetboundary")
                .addFile("file", "data.bin", null, new byte[]{1, 2, 3})
                .build();

        String text = new String(body.bytes(), StandardCharsets.UTF_8);
        assertTrue(text.contains("Content-Type: application/octet-stream"));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}