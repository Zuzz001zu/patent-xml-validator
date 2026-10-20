package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.TestXml;
import io.github.zuzz001zu.patentxml.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.github.zuzz001zu.patentxml.TestXml.ruleIds;
import static io.github.zuzz001zu.patentxml.TestXml.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureRulesTest {

    @Test
    void sequenceNumbersMustStartAtOne() {
        String xml = with("<applicant sequence=\"1\">", "<applicant sequence=\"2\">");
        assertEquals(List.of("SEQ-01"), ruleIds(xml));
    }

    @Test
    void sequenceNumbersCannotRepeat() {
        String xml = with(
                "<classification-ipcr sequence=\"1\"><text>A61K 31/496</text></classification-ipcr>",
                "<classification-ipcr sequence=\"1\"><text>A61K 31/496</text></classification-ipcr>\n"
                        + "      <classification-ipcr sequence=\"1\"><text>A61P 25/28</text></classification-ipcr>");
        assertEquals(List.of("SEQ-01"), ruleIds(xml));
    }

    @Test
    void titleInThePublicationLanguageIsRequired() {
        String xml = with("<invention-title lang=\"en\">", "<invention-title lang=\"fr\">");
        assertEquals(List.of("TTL-01"), ruleIds(xml));
    }

    @Test
    void oneTitlePerLanguage() {
        String xml = with("</bibliographic-data>",
                "  <invention-title lang=\"en\">Another title</invention-title>\n  </bibliographic-data>");
        assertEquals(List.of("TTL-01"), ruleIds(xml));
    }

    @Test
    void veryLongEnglishTitleIsAWarning() {
        String longTitle = "Method and system for the controlled release of an active pharmaceutical ingredient "
                + "using a biodegradable polymer matrix";
        ValidationResult result = TestXml.validate(with(
                "Piperazine derivatives for treating neurodegenerative diseases", longTitle));

        assertEquals(List.of("TTL-02"), result.findings().stream().map(Finding::ruleId).toList());
        assertEquals(Severity.WARNING, result.findings().getFirst().severity());
    }

    @Test
    void everyRuleHasAUniqueIdAndADescription() {
        Set<String> ids = new HashSet<>();
        for (Rule rule : Rules.all()) {
            assertTrue(ids.add(rule.id()), "duplicate rule id " + rule.id());
            assertTrue(rule.id().matches("[A-Z]{3}-\\d{2}"), rule.id());
            assertTrue(!rule.description().isBlank(), rule.id());
        }
    }
}
