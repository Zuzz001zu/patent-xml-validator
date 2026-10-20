package io.github.zuzz001zu.patentxml.xml;

import io.github.zuzz001zu.patentxml.rules.Finding;
import io.github.zuzz001zu.patentxml.rules.Severity;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Reads a patent XML file: secure parsing, schema validation and mapping to the model. */
public final class PatentXmlParser {

    public static final String NOT_WELL_FORMED = "XML-01";
    public static final String SCHEMA_VIOLATION = "XSD-01";

    private static final String SCHEMA = "/schema/patent-document.xsd";

    private final SAXParserFactory parserFactory;
    private final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

    public PatentXmlParser() {
        var schema = SecureXml.loadSchema(PatentXmlParser.class.getResource(SCHEMA));
        this.parserFactory = SecureXml.parserFactory(schema);
    }

    public ParsedXml parse(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return parse(in);
        }
    }

    public ParsedXml parse(InputStream in) throws IOException {
        Document dom = newDocument();
        var builder = new LocatingDomBuilder(dom);
        try {
            parserFactory.newSAXParser().parse(in, builder);
        } catch (SAXParseException e) {
            return new ParsedXml(null, List.of(structural(NOT_WELL_FORMED, e)));
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException("cannot parse XML", e);
        }

        if (!builder.schemaErrors().isEmpty()) {
            List<Finding> errors = withoutFollowUps(builder.schemaErrors()).stream()
                    .map(e -> structural(SCHEMA_VIOLATION, e))
                    .toList();
            return new ParsedXml(null, errors);
        }
        return new ParsedXml(new DocumentReader(dom).read(), List.of());
    }

    private Document newDocument() {
        try {
            return domFactory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Finding structural(String ruleId, SAXParseException e) {
        return new Finding(ruleId, Severity.ERROR, Math.max(e.getLineNumber(), 0), cleanMessage(e));
    }

    /**
     * Xerces reports a wrong value twice: once for the failed facet (pattern, enumeration)
     * and once more saying the element or attribute is not valid. Keep the first, which
     * says what is wrong.
     */
    static List<SAXParseException> withoutFollowUps(List<SAXParseException> errors) {
        List<SAXParseException> kept = new ArrayList<>();
        for (SAXParseException e : errors) {
            String message = String.valueOf(e.getMessage());
            boolean followUp = message.startsWith("cvc-type.3.1.3") || message.startsWith("cvc-attribute.3");
            boolean sameLineAsPrevious = !kept.isEmpty() && kept.getLast().getLineNumber() == e.getLineNumber();
            if (!(followUp && sameLineAsPrevious)) {
                kept.add(e);
            }
        }
        return kept;
    }

    /** Xerces prefixes messages with an internal code such as "cvc-pattern-valid: ". */
    static String cleanMessage(SAXParseException e) {
        String message = e.getMessage() == null ? "invalid XML" : e.getMessage();
        if (message.contains("DOCTYPE is disallowed")) {
            return "DOCTYPE declarations are not accepted (protection against external entities)";
        }
        return message.replaceFirst("^cvc-[\\w.-]+: ", "");
    }
}
