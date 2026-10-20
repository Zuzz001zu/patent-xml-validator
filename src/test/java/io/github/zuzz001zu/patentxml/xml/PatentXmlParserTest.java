package io.github.zuzz001zu.patentxml.xml;

import io.github.zuzz001zu.patentxml.TestXml;
import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.rules.Finding;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatentXmlParserTest {

    private final PatentXmlParser parser = new PatentXmlParser();

    private ParsedXml parse(String xml) throws IOException {
        return parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void readsTheBibliographicData() throws IOException {
        PatentDocument doc = parse(TestXml.VALID).documentIfValid().orElseThrow();

        assertEquals("en", doc.publicationLanguage().text());
        assertEquals("2025/118204", doc.publication().number().text());
        assertEquals("A1", doc.publication().kind().text());
        assertEquals("PCT/CH2025/050211", doc.application().number().text());
        assertEquals(1, doc.priorityClaims().size());
        assertEquals("CH", doc.priorityClaims().getFirst().country().text());
        assertEquals("A61K 31/496", doc.classifications().getFirst().symbol().text());
        assertEquals("CH", doc.applicants().getFirst().nationality().text());
        assertEquals("MEYLAN, Claire", doc.inventors().getFirst().name().text());
        assertEquals("en", doc.titles().getFirst().lang().text());
    }

    @Test
    void keepsTheLineOfEveryValue() throws IOException {
        PatentDocument doc = parse(TestXml.VALID).documentIfValid().orElseThrow();

        assertEquals(7, doc.publication().number().line());
        assertEquals(15, doc.application().date().line());
        assertEquals(20, doc.priorityClaims().getFirst().line());
        assertEquals(41, doc.titles().getFirst().text().line());
    }

    @Test
    void reportsASchemaViolationWithItsLine() throws IOException {
        ParsedXml parsed = parse(TestXml.with("<date>20250918</date>", "<date>2025-09-18</date>"));

        assertEquals(1, parsed.structuralErrors().size());
        Finding error = parsed.structuralErrors().getFirst();
        assertEquals(PatentXmlParser.SCHEMA_VIOLATION, error.ruleId());
        assertEquals(9, error.line());
        assertTrue(error.message().contains("2025-09-18"), error.message());
        assertTrue(parsed.documentIfValid().isEmpty());
    }

    @Test
    void listsEverySchemaViolationAtOnce() throws IOException {
        String xml = TestXml.with("<date>20250918</date>", "<date>2025-09-18</date>");
        xml = TestXml.replace(xml, "    <language-of-filing>en</language-of-filing>\n", "");

        assertEquals(2, parse(xml).structuralErrors().size());
    }

    @Test
    void rejectsValuesMadeOnlyOfSpaces() throws IOException {
        ParsedXml parsed = parse(TestXml.with("<name>Lemanis Therapeutics SA</name>", "<name>   </name>"));

        assertEquals(PatentXmlParser.SCHEMA_VIOLATION, parsed.structuralErrors().getFirst().ruleId());
    }

    @Test
    void reportsXmlThatIsNotWellFormed() throws IOException {
        ParsedXml parsed = parse(TestXml.with("<kind>A1</kind>", "<kind>A1"));

        assertEquals(PatentXmlParser.NOT_WELL_FORMED, parsed.structuralErrors().getFirst().ruleId());
        assertNull(parsed.document());
    }

    @Test
    void refusesExternalEntities() throws IOException {
        String xxe = TestXml.VALID.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE patent-document [ <!ENTITY secret SYSTEM "file:///etc/hostname"> ]>
                """).replace("<doc-number>2025/118204</doc-number>", "<doc-number>&secret;</doc-number>");

        ParsedXml parsed = parse(xxe);

        assertNull(parsed.document());
        assertEquals(PatentXmlParser.NOT_WELL_FORMED, parsed.structuralErrors().getFirst().ruleId());
        assertTrue(parsed.structuralErrors().getFirst().message().contains("DOCTYPE"));
    }

    @Test
    void refusesEntityExpansion() throws IOException {
        String bomb = """
                <?xml version="1.0"?>
                <!DOCTYPE lolz [
                  <!ENTITY lol "lol">
                  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
                  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
                ]>
                <patent-document lang="en">&lol3;</patent-document>
                """;

        assertEquals(PatentXmlParser.NOT_WELL_FORMED, parse(bomb).structuralErrors().getFirst().ruleId());
    }
}
