package com.heirs.desktop.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class CsvExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void escapeReturnsPlainStringUnchanged() {
        assertEquals("hello", CsvExporter.escape("hello"));
    }

    @Test
    void escapeReturnsNullAsEmptyString() {
        assertEquals("", CsvExporter.escape(null));
        assertEquals("", CsvExporter.escape(""));
    }

    @Test
    void escapeQuotesStringContainingComma() {
        assertEquals("\"a,b\"", CsvExporter.escape("a,b"));
    }

    @Test
    void escapeDoublesExistingQuotes() {
        assertEquals("\"say \"\"hello\"\"\"", CsvExporter.escape("say \"hello\""));
    }

    @Test
    void escapeQuotesStringContainingNewline() {
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"));
    }

    @Test
    void escapeQuotesStringContainingCarriageReturn() {
        assertEquals("\"line1\rline2\"", CsvExporter.escape("line1\rline2"));
    }

    @Test
    void escapeHandlesNumericValues() {
        assertEquals("42", CsvExporter.escape(42));
        assertEquals("3.14", CsvExporter.escape(3.14));
    }

    @Test
    void rowBuildsCorrectCsvLine() {
        String line = CsvExporter.row("Alpha", "Beta", "Gamma");
        assertEquals("Alpha,Beta,Gamma", line);
    }

    @Test
    void rowEscapesFieldsNeedingQuotes() {
        String line = CsvExporter.row(List.of("has,comma", "has\"quote", "plain"));
        assertEquals("\"has,comma\",\"has\"\"quote\",plain", line);
    }

    @Test
    void rowHandlesNullElements() {
        var cells = new ArrayList<String>();
        cells.add("a");
        cells.add(null);
        cells.add("c");
        String line = CsvExporter.row(cells);
        assertEquals("a,,c", line);
    }

    @Test
    void writeCreatesValidUtf8File() throws IOException {
        Path target = tempDir.resolve("test.csv");
        List<List<String>> rows = List.of(
                List.of("Name", "Count"),
                List.of("Policy", "19"),
                List.of("Scheme, Regional", "12"),
                List.of("say \"ok\"", "1")
        );
        CsvExporter.write(target, rows);

        assertTrue(Files.exists(target));
        String content = Files.readString(target, StandardCharsets.UTF_8);
        assertTrue(content.contains("Name,Count\r\n"));
        assertTrue(content.contains("Policy,19\r\n"));
        assertTrue(content.contains("\"Scheme, Regional\",12\r\n"));
        assertTrue(content.contains("\"say \"\"ok\"\"\",1\r\n"));
    }
}
