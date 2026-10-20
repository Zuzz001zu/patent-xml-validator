package io.github.zuzz001zu.patentxml;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.rules.Finding;
import io.github.zuzz001zu.patentxml.rules.Rule;
import io.github.zuzz001zu.patentxml.rules.Severity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentValidatorTest {

    /** Counts how often it is called, to check that rules are skipped for broken files. */
    private static final class CountingRule implements Rule {
        int calls;

        @Override
        public String id() {
            return "TST-01";
        }

        @Override
        public String description() {
            return "test rule";
        }

        @Override
        public List<Finding> check(PatentDocument document) {
            calls++;
            return List.of();
        }
    }

    @Test
    void businessRulesDoNotRunOnASchemaInvalidDocument() {
        CountingRule rule = new CountingRule();
        DocumentValidator validator = new DocumentValidator(List.of(rule));
        String broken = TestXml.with("<kind>A1</kind>", "<kind>a1</kind>");

        ValidationResult result = validate(validator, broken);

        assertEquals(0, rule.calls);
        assertEquals(ValidationResult.Status.INVALID, result.status());
        assertEquals("XSD-01", result.findings().getFirst().ruleId());
    }

    @Test
    void findingsAreSortedByLine() {
        String xml = TestXml.with("<country>WO</country>", "<country>EP</country>");
        xml = TestXml.replace(xml, "<kind>A1</kind>", "<kind>B1</kind>");

        List<Integer> lines = TestXml.validate(xml).findings().stream().map(Finding::line).toList();

        assertEquals(List.of(6, 8), lines);
    }

    @Test
    void validatesEveryXmlFileOfAFolderInOrder(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("b.xml"), TestXml.VALID);
        Files.writeString(dir.resolve("a.xml"), TestXml.VALID);
        Files.writeString(dir.resolve("notes.txt"), "not xml");

        List<ValidationResult> results = new DocumentValidator().validateAll(List.of(dir));

        assertEquals(List.of("a.xml", "b.xml"), results.stream().map(ValidationResult::fileName).toList());
    }

    @Test
    void missingPathIsReported(@TempDir Path dir) {
        assertThrows(NoSuchFileException.class,
                () -> new DocumentValidator().validateAll(List.of(dir.resolve("missing.xml"))));
    }

    @Test
    void warningsOnlyGiveTheWarningStatus() {
        ValidationResult result = new ValidationResult("x.xml", null,
                List.of(new Finding("PUB-05", Severity.WARNING, 9, "early")));

        assertEquals(ValidationResult.Status.WARNING, result.status());
        assertTrue(result.receivingOffice().isEmpty());
    }

    private static ValidationResult validate(DocumentValidator validator, String xml) {
        try {
            return validator.validate("t.xml", new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
