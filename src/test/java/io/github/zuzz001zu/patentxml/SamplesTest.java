package io.github.zuzz001zu.patentxml;

import io.github.zuzz001zu.patentxml.rules.Finding;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The files in samples/ double as regression tests: each one shows a known situation. */
class SamplesTest {

    @ParameterizedTest
    @CsvSource({
            "wo2025-118204-lemanis-therapeutics.xml, VALID,   ''",
            "wo2025-120577-nordhaven-robotics.xml,   VALID,   ''",
            "wo2025-121930-kisaragi-sensors.xml,     VALID,   ''",
            "wo2025-124415-andina-agro.xml,          WARNING, PRI-01 TTL-02",
            "wo2025-126003-helvetic-battery.xml,     WARNING, PUB-05",
            "wo2025-127781-date-errors.xml,          INVALID, PUB-02 DAT-01 DAT-02",
            "wo2025-129050-code-errors.xml,          INVALID, PUB-04 PUB-03 APP-01 LNG-01 CLS-01 CLS-01 CTY-01",
            "wo2025-130412-claim-errors.xml,         INVALID, PRI-03 PRI-02 SEQ-01",
            "wo2025-131877-schema-errors.xml,        INVALID, XSD-01 XSD-01 XSD-01",
            "broken-not-well-formed.xml,             INVALID, XML-01",
            "xxe-attempt.xml,                        INVALID, XML-01"})
    void sampleGivesTheExpectedResult(String file, ValidationResult.Status status, String rules) throws IOException {
        ValidationResult result = new DocumentValidator().validate(Path.of("samples", file));

        assertEquals(status, result.status(), file);
        List<String> expected = rules.isBlank() ? List.of() : Arrays.asList(rules.split(" "));
        assertEquals(expected, result.findings().stream().map(Finding::ruleId).toList(), file);
    }
}
