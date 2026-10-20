package io.github.zuzz001zu.patentxml;

import io.github.zuzz001zu.patentxml.rules.Finding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A valid document that each test changes in one place, so a failing test shows
 * exactly which change triggered a rule. Line numbers below are relied on by tests.
 */
public final class TestXml {

    public static final String VALID = """
            <?xml version="1.0" encoding="UTF-8"?>
            <patent-document lang="en">
              <bibliographic-data>
                <publication-reference>
                  <document-id>
                    <country>WO</country>
                    <doc-number>2025/118204</doc-number>
                    <kind>A1</kind>
                    <date>20250918</date>
                  </document-id>
                </publication-reference>
                <application-reference>
                  <document-id>
                    <doc-number>PCT/CH2025/050211</doc-number>
                    <date>20250312</date>
                  </document-id>
                </application-reference>
                <language-of-filing>en</language-of-filing>
                <priority-claims>
                  <priority-claim sequence="1" kind="national">
                    <country>CH</country>
                    <doc-number>000318/2024</doc-number>
                    <date>20240315</date>
                  </priority-claim>
                </priority-claims>
                <classifications-ipcr>
                  <classification-ipcr sequence="1"><text>A61K 31/496</text></classification-ipcr>
                </classifications-ipcr>
                <parties>
                  <applicants>
                    <applicant sequence="1">
                      <name>Lemanis Therapeutics SA</name>
                      <nationality><country>CH</country></nationality>
                      <residence><country>CH</country></residence>
                    </applicant>
                  </applicants>
                  <inventors>
                    <inventor sequence="1"><name>MEYLAN, Claire</name></inventor>
                  </inventors>
                </parties>
                <invention-title lang="en">Piperazine derivatives for treating neurodegenerative diseases</invention-title>
              </bibliographic-data>
            </patent-document>
            """;

    private static final DocumentValidator VALIDATOR = new DocumentValidator();

    private TestXml() {
    }

    /** The valid document with one fragment replaced. The fragment must occur exactly once. */
    public static String with(String fragment, String replacement) {
        return replace(VALID, fragment, replacement);
    }

    public static String replace(String xml, String fragment, String replacement) {
        int first = xml.indexOf(fragment);
        if (first < 0 || xml.indexOf(fragment, first + 1) >= 0) {
            throw new IllegalArgumentException("fragment must occur exactly once: " + fragment);
        }
        return xml.replace(fragment, replacement);
    }

    /** Adds a priority claim after the existing one. */
    public static String withExtraPriority(String xml, String claim) {
        return replace(xml, "</priority-claims>", claim + "\n    </priority-claims>");
    }

    public static ValidationResult validate(String xml) {
        try {
            return VALIDATOR.validate("test.xml", new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<String> ruleIds(String xml) {
        return validate(xml).findings().stream().map(Finding::ruleId).toList();
    }
}
