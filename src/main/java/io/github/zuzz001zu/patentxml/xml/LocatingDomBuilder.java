package io.github.zuzz001zu.patentxml.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Builds a DOM from SAX events and stores the start line of every element.
 *
 * A standard DocumentBuilder loses line numbers, but a report that says
 * "line 42" is far more useful to the person who has to fix the file.
 * Schema violations reported by the validating parser are collected instead
 * of stopping the parse, so all of them are listed at once.
 */
final class LocatingDomBuilder extends DefaultHandler {

    static final String LINE = "line";

    private final Document document;
    private final Deque<Node> open = new ArrayDeque<>();
    private final StringBuilder text = new StringBuilder();
    private final List<SAXParseException> schemaErrors = new ArrayList<>();
    private Locator locator;

    LocatingDomBuilder(Document document) {
        this.document = document;
        open.push(document);
    }

    static int lineOf(Node node) {
        Object line = node.getUserData(LINE);
        return line instanceof Integer value ? value : 0;
    }

    List<SAXParseException> schemaErrors() {
        return List.copyOf(schemaErrors);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        flushText();
        Element element = document.createElement(qName);
        for (int i = 0; i < attributes.getLength(); i++) {
            element.setAttribute(attributes.getQName(i), attributes.getValue(i));
        }
        element.setUserData(LINE, locator == null ? 0 : locator.getLineNumber(), null);
        open.peek().appendChild(element);
        open.push(element);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        flushText();
        open.pop();
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        text.append(ch, start, length);
    }

    @Override
    public void error(SAXParseException e) {
        schemaErrors.add(e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXParseException {
        throw e;
    }

    private void flushText() {
        if (!text.isEmpty()) {
            open.peek().appendChild(document.createTextNode(text.toString()));
            text.setLength(0);
        }
    }
}
