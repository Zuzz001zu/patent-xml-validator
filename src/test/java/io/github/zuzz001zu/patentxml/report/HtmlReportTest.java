package io.github.zuzz001zu.patentxml.report;

import io.github.zuzz001zu.patentxml.TestXml;
import io.github.zuzz001zu.patentxml.ValidationResult;
import io.github.zuzz001zu.patentxml.rules.Finding;
import io.github.zuzz001zu.patentxml.rules.Severity;
import io.github.zuzz001zu.patentxml.store.Table;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlReportTest {

    private static final Table EMPTY = new Table(List.of("a"), List.of());

    private static RunReport report(List<ValidationResult> results, Table byRule) {
        return new RunReport(7, LocalDateTime.of(2026, 10, 20, 9, 30), "samples", "1.0.0",
                results, byRule, EMPTY, EMPTY);
    }

    @Test
    void showsTheSummaryAndEveryDocument() {
        ValidationResult valid = TestXml.validate(TestXml.VALID);
        ValidationResult invalid = TestXml.validate(TestXml.with("<kind>A1</kind>", "<kind>B1</kind>"));
        Table byRule = new Table(List.of("rule_id", "severity", "occurrences", "documents"),
                List.of(List.of("PUB-03", "ERROR", 1L, 1L)));

        String html = new HtmlReport().render(report(List.of(valid, invalid), byRule));

        assertTrue(html.contains("Run 7 · 20.10.2026 09:30"));
        assertTrue(html.contains("Kind code is one used for WO publications"), "rule description");
        assertTrue(html.contains("kind code B1 is not used for WO publications"));
        assertTrue(html.contains("WO 2025/118204"));
        assertTrue(html.contains("All checks passed."));
    }

    @Test
    void escapesTextFromTheFiles() {
        ValidationResult result = new ValidationResult("<script>alert(1)</script>.xml", null,
                List.of(new Finding("XML-01", Severity.ERROR, 3, "bad \"value\" & <tag>")));

        String html = new HtmlReport().render(report(List.of(result), EMPTY));

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("bad &quot;value&quot; &amp; &lt;tag&gt;"));
    }

    @Test
    void escapeHandlesNull() {
        assertEquals("", HtmlReport.esc(null));
    }
}
