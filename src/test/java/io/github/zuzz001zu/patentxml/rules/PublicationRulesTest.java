package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.TestXml;
import io.github.zuzz001zu.patentxml.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static io.github.zuzz001zu.patentxml.TestXml.ruleIds;
import static io.github.zuzz001zu.patentxml.TestXml.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicationRulesTest {

    @Test
    void validDocumentHasNoFindings() {
        ValidationResult result = TestXml.validate(TestXml.VALID);
        assertEquals(List.of(), result.findings());
        assertEquals(ValidationResult.Status.VALID, result.status());
    }

    @Test
    void publicationCountryMustBeWO() {
        assertEquals(List.of("PUB-01"), ruleIds(with("<country>WO</country>", "<country>EP</country>")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025/11820", "WO2025/118204", "25/118204", "2025-118204"})
    void publicationNumberHasTheWipoFormat(String number) {
        String xml = with("<doc-number>2025/118204</doc-number>", "<doc-number>" + number + "</doc-number>");
        assertEquals(List.of("PUB-02"), ruleIds(xml));
    }

    @Test
    void publicationNumberYearMatchesThePublicationDate() {
        String xml = with("<doc-number>2025/118204</doc-number>", "<doc-number>2024/118204</doc-number>");
        assertEquals(List.of("PUB-02"), ruleIds(xml));
    }

    @ParameterizedTest
    @ValueSource(strings = {"B1", "A5", "A", "U1"})
    void kindCodeMustBeOneUsedByWipo(String kind) {
        assertEquals(List.of("PUB-03"), ruleIds(with("<kind>A1</kind>", "<kind>" + kind + "</kind>")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A1", "A2", "A3", "A4", "A8", "A9"})
    void woKindCodesAreAccepted(String kind) {
        assertEquals(List.of(), ruleIds(with("<kind>A1</kind>", "<kind>" + kind + "</kind>")));
    }

    @Test
    void publicationLanguageMustBeAPctLanguage() {
        String xml = with("<patent-document lang=\"en\">", "<patent-document lang=\"it\">");
        assertTrue(ruleIds(xml).contains("PUB-04"));
    }

    @Test
    void earlyFirstPublicationIsAWarning() {
        // priority date 15.03.2024, so the 18-month date is 15.09.2025
        ValidationResult result = TestXml.validate(with("<date>20250918</date>", "<date>20250701</date>"));

        assertEquals(List.of("PUB-05"), result.findings().stream().map(Finding::ruleId).toList());
        assertEquals(Severity.WARNING, result.findings().getFirst().severity());
        assertEquals(ValidationResult.Status.WARNING, result.status());
    }

    @Test
    void laterRepublicationIsNotTreatedAsEarly() {
        String xml = with("<date>20250918</date>", "<date>20250701</date>");
        xml = TestXml.replace(xml, "<kind>A1</kind>", "<kind>A3</kind>");
        assertEquals(List.of(), ruleIds(xml));
    }
}
