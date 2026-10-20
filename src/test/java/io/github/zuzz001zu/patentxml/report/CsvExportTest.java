package io.github.zuzz001zu.patentxml.report;

import io.github.zuzz001zu.patentxml.TestXml;
import io.github.zuzz001zu.patentxml.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExportTest {

    @Test
    void writesOneLinePerFinding(@TempDir Path dir) throws IOException {
        ValidationResult invalid = TestXml.validate(TestXml.with("<kind>A1</kind>", "<kind>B1</kind>"));
        ValidationResult valid = TestXml.validate(TestXml.VALID);

        Path csv = CsvExport.write(List.of(invalid, valid), dir.resolve("findings.csv"));
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith("﻿file;publication_number"));
        assertEquals("test.xml;2025/118204;PCT/CH2025/050211;PUB-03;ERROR;8;"
                + "kind code B1 is not used for WO publications", lines.get(1));
    }

    @Test
    void quotesCellsThatNeedIt() {
        assertEquals("plain", CsvExport.cell("plain"));
        assertEquals("\"a;b\"", CsvExport.cell("a;b"));
        assertEquals("\"say \"\"hi\"\"\"", CsvExport.cell("say \"hi\""));
    }
}
