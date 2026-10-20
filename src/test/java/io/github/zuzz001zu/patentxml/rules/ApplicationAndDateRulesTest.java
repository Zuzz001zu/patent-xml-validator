package io.github.zuzz001zu.patentxml.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static io.github.zuzz001zu.patentxml.TestXml.ruleIds;
import static io.github.zuzz001zu.patentxml.TestXml.with;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationAndDateRulesTest {

    private static String applicationNumber(String number) {
        return with("<doc-number>PCT/CH2025/050211</doc-number>", "<doc-number>" + number + "</doc-number>");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PCT/CH2025/50211", "CH2025/050211", "PCT/CH25/050211", "pct/ch2025/050211"})
    void applicationNumberHasThePctFormat(String number) {
        assertEquals(List.of("APP-01"), ruleIds(applicationNumber(number)));
    }

    @Test
    void receivingOfficeMustExist() {
        assertEquals(List.of("APP-01"), ruleIds(applicationNumber("PCT/QQ2025/050211")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PCT/IB2025/050211", "PCT/EP2025/050211", "PCT/US2025/050211"})
    void internationalBureauAndRegionalOfficesAreReceivingOffices(String number) {
        assertEquals(List.of(), ruleIds(applicationNumber(number)));
    }

    @Test
    void yearInTheNumberIsTheFilingYear() {
        assertEquals(List.of("APP-01"), ruleIds(applicationNumber("PCT/CH2024/050211")));
    }

    @Test
    void receivingOfficeIsExtractedFromTheNumber() {
        assertEquals(Optional.of("CH"), ApplicationNumberRule.receivingOffice("PCT/CH2025/050211"));
        assertEquals(Optional.empty(), ApplicationNumberRule.receivingOffice("CH 2025 050211"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"20250230", "20251301", "20250000"})
    void datesMustExistInTheCalendar(String date) {
        assertTrue(ruleIds(with("<date>20250312</date>", "<date>" + date + "</date>")).contains("DAT-01"));
    }

    @Test
    void publicationCannotBeBeforeFiling() {
        assertTrue(ruleIds(with("<date>20250918</date>", "<date>20250301</date>")).contains("DAT-02"));
    }

    @Test
    void priorityDateMustBeBeforeFiling() {
        assertTrue(ruleIds(with("<date>20240315</date>", "<date>20250312</date>")).contains("DAT-02"));
    }
}
