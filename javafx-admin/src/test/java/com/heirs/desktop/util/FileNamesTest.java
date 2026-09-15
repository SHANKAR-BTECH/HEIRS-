package com.heirs.desktop.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileNamesTest {

    @Test
    void parsesExtendedUtf8Filename() {
        String header = "attachment; filename=\"safe_name.pdf\"; filename*=UTF-8''%E2%82%ACrate.pdf";
        assertEquals("\u20ACrate.pdf", FileNames.parseContentDispositionFilename(header));
    }

    @Test
    void parsesSimpleQuotedFilename() {
        String header = "inline; filename=\"report.pdf\"";
        assertEquals("report.pdf", FileNames.parseContentDispositionFilename(header));
    }

    @Test
    void prefersExtendedFilenameOverPlain() {
        String header = "attachment; filename=\"old.pdf\"; filename*=UTF-8''new.pdf";
        assertEquals("new.pdf", FileNames.parseContentDispositionFilename(header));
    }

    @Test
    void returnsNullWhenAbsent() {
        assertNull(FileNames.parseContentDispositionFilename(null));
        assertNull(FileNames.parseContentDispositionFilename(""));
        assertNull(FileNames.parseContentDispositionFilename("attachment"));
    }

    @Test
    void stripsAllPathComponents() {
        assertEquals("foo.pdf", FileNames.safeFileName("C:\\evil\\dir\\foo.pdf"));
        assertEquals("bar.pdf", FileNames.safeFileName("../bar.pdf"));
        assertEquals("baz.pdf", FileNames.safeFileName("/tmp/x/baz.pdf"));
        assertEquals("doc.pdf", FileNames.safeFileName("C:\\absolute\\doc.pdf"));
    }

    @Test
    void fallsBackForUnsafeOrBlankInput() {
        assertEquals("document.pdf", FileNames.safeFileName(null));
        assertEquals("document.pdf", FileNames.safeFileName(""));
        assertEquals("document.pdf", FileNames.safeFileName(".."));
        assertEquals("document.pdf", FileNames.safeFileName("."));
    }

    @Test
    void formatsHumanReadableSizes() {
        assertEquals("0 B", FileNames.formatSize(0));
        assertEquals("153 B", FileNames.formatSize(153));
        assertEquals("153 KB", FileNames.formatSize(153 * 1024));
        assertEquals("1.8 MB", FileNames.formatSize((long) (1.8 * 1024 * 1024)));
        assertEquals("1.5 MB", FileNames.formatSize((long) (1.5 * 1024 * 1024)));
    }
}