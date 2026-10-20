package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.TestXml;
import io.github.zuzz001zu.patentxml.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static io.github.zuzz001zu.patentxml.TestXml.ruleIds;
import static io.github.zuzz001zu.patentxml.TestXml.with;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityRulesTest {

    /** The valid document with another priority date and filing date. */
    private static String dates(String priority, String filing) {
        String xml = with("<date>20240315</date>", "<date>" + priority + "</date>");
        xml = TestXml.replace(xml, "<date>20250312</date>", "<date>" + filing + "</date>");
        return xml;
    }

    private static String claim(int sequence, String kind, String country, String number, String date) {
        return """
                  <priority-claim sequence="%d" kind="%s">
                    <country>%s</country>
                    <doc-number>%s</doc-number>
                    <date>%s</date>
                  </priority-claim>""".formatted(sequence, kind, country, number, date);
    }

    @ParameterizedTest
    @CsvSource({
            "20240312, 20250312",   // exactly 12 months
            "20240229, 20250228",   // leap day: the period ends on 28 February
            "20240318, 20250312"})  // three months: publication still at 18 months
    void filingWithinTwelveMonthsIsFine(String priority, String filing) {
        assertEquals(List.of(), ruleIds(dates(priority, filing)));
    }

    @Test
    void filingInTheRestorationPeriodIsAWarning() {
        ValidationResult result = TestXml.validate(dates("20240210", "20250312"));

        assertEquals(1, result.findings().size());
        Finding finding = result.findings().getFirst();
        assertEquals("PRI-01", finding.ruleId());
        assertEquals(Severity.WARNING, finding.severity());
        assertEquals(20, finding.line());
    }

    @Test
    void dayAfterTheLeapYearPeriodIsLate() {
        assertEquals(List.of("PRI-01"), ruleIds(dates("20240229", "20250301")));
    }

    @Test
    void filingAfterFourteenMonthsIsAnError() {
        ValidationResult result = TestXml.validate(dates("20231201", "20250312"));

        assertEquals("PRI-01", result.findings().getFirst().ruleId());
        assertEquals(Severity.ERROR, result.findings().getFirst().severity());
    }

    @ParameterizedTest
    @CsvSource({
            "regional, EP, 24170512.3",
            "regional, EA, 202490123",
            "international, WO, PCT/EP2024/061234",
            "national, DE, 10 2024 114 200.5"})
    void priorityKindMatchesTheOffice(String kind, String country, String number) {
        String xml = TestXml.withExtraPriority(TestXml.VALID, claim(2, kind, country, number, "20240320"));
        assertEquals(List.of(), ruleIds(xml));
    }

    @ParameterizedTest
    @CsvSource({
            "regional, DE, 10 2024 114 200.5",
            "national, EP, 24170512.3",
            "international, EP, EP24170512"})
    void priorityKindThatDoesNotMatchIsAnError(String kind, String country, String number) {
        String xml = TestXml.withExtraPriority(TestXml.VALID, claim(2, kind, country, number, "20240320"));
        assertEquals(List.of("PRI-02"), ruleIds(xml));
    }

    @Test
    void sameApplicationClaimedTwiceIsAnError() {
        String xml = TestXml.withExtraPriority(TestXml.VALID, claim(2, "national", "CH", "000 318/2024", "20240315"));
        assertEquals(List.of("PRI-03"), ruleIds(xml));
    }
}
