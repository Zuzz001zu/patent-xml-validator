package io.github.zuzz001zu.patentxml.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static io.github.zuzz001zu.patentxml.TestXml.ruleIds;
import static io.github.zuzz001zu.patentxml.TestXml.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {"A61K 31/496", "G06F 16/00", "H01M 10/0525", "B25J  9/16", "C07D 401/123456"})
    void validIpcSymbols(String symbol) {
        assertTrue(IpcSymbolRule.isValid(symbol));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A61K31/496", "Z01B 1/00", "A61K 31/4", "a61k 31/496", "A6K 31/496", "A61K 31"})
    void invalidIpcSymbols(String symbol) {
        assertFalse(IpcSymbolRule.isValid(symbol));
        String xml = with("<text>A61K 31/496</text>", "<text>" + symbol + "</text>");
        assertEquals(List.of("CLS-01"), ruleIds(xml));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XX", "EU", "UK"})
    void nationalityMustBeACountry(String code) {
        String xml = with("<nationality><country>CH</country></nationality>",
                "<nationality><country>" + code + "</country></nationality>");
        assertEquals(List.of("CTY-01"), ruleIds(xml));
    }

    @Test
    void filingLanguageMustBeAnIsoCode() {
        String xml = with("<language-of-filing>en</language-of-filing>", "<language-of-filing>xx</language-of-filing>");
        assertEquals(List.of("LNG-01"), ruleIds(xml));
    }

    @Test
    void codeLists() {
        assertTrue(Codes.isCountry("CH"));
        assertFalse(Codes.isCountry("EP"));
        assertTrue(Codes.isReceivingOffice("IB"));
        assertTrue(Codes.isReceivingOffice("EP"));
        assertTrue(Codes.isLanguage("de"));
        assertEquals(10, Codes.PUBLICATION_LANGUAGES.size());
    }
}
