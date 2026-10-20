package io.github.zuzz001zu.patentxml.xml;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.net.URL;

/**
 * Parser factories configured for untrusted input.
 *
 * Patent data comes from many offices and filing tools, so DOCTYPE declarations and
 * external entities are refused outright. This closes XXE and "billion laughs" attacks.
 */
final class SecureXml {

    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    private SecureXml() {
    }

    static Schema loadSchema(URL location) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newSchema(location);
        } catch (SAXException e) {
            throw new IllegalStateException("cannot load schema " + location, e);
        }
    }

    static SAXParserFactory parserFactory(Schema schema) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(DISALLOW_DOCTYPE, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setSchema(schema);
            return factory;
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("cannot configure the XML parser", e);
        }
    }
}
