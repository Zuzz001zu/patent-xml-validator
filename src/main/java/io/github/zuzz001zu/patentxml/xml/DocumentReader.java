package io.github.zuzz001zu.patentxml.xml;

import io.github.zuzz001zu.patentxml.model.Classification;
import io.github.zuzz001zu.patentxml.model.DocumentId;
import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.Party;
import io.github.zuzz001zu.patentxml.model.PriorityClaim;
import io.github.zuzz001zu.patentxml.model.Title;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Maps a schema-valid DOM to {@link PatentDocument}. Because the schema has already
 * been checked, required elements can be assumed; optional ones become empty fields.
 */
final class DocumentReader {

    private final Element root;

    DocumentReader(Document document) {
        this.root = document.getDocumentElement();
    }

    PatentDocument read() {
        Element biblio = child(root, "bibliographic-data").orElseThrow();
        Element parties = child(biblio, "parties").orElseThrow();

        return new PatentDocument(
                attribute(root, "lang"),
                text(biblio, "language-of-filing"),
                documentId(child(biblio, "publication-reference").orElseThrow()),
                documentId(child(biblio, "application-reference").orElseThrow()),
                priorityClaims(biblio),
                classifications(biblio),
                parties(parties, "applicants", "applicant"),
                parties(parties, "inventors", "inventor"),
                titles(biblio));
    }

    private static DocumentId documentId(Element reference) {
        Element id = child(reference, "document-id").orElseThrow();
        return new DocumentId(text(id, "country"), text(id, "doc-number"), text(id, "kind"), text(id, "date"));
    }

    private static List<PriorityClaim> priorityClaims(Element biblio) {
        return child(biblio, "priority-claims")
                .map(claims -> children(claims, "priority-claim").stream()
                        .map(c -> new PriorityClaim(
                                LocatingDomBuilder.lineOf(c),
                                attribute(c, "sequence"),
                                attribute(c, "kind"),
                                text(c, "country"),
                                text(c, "doc-number"),
                                text(c, "date")))
                        .toList())
                .orElse(List.of());
    }

    private static List<Classification> classifications(Element biblio) {
        Element list = child(biblio, "classifications-ipcr").orElseThrow();
        return children(list, "classification-ipcr").stream()
                .map(c -> new Classification(attribute(c, "sequence"), text(c, "text")))
                .toList();
    }

    private static List<Party> parties(Element parties, String group, String member) {
        return child(parties, group)
                .map(g -> children(g, member).stream()
                        .map(p -> new Party(
                                LocatingDomBuilder.lineOf(p),
                                attribute(p, "sequence"),
                                text(p, "name"),
                                nested(p, "nationality"),
                                nested(p, "residence")))
                        .toList())
                .orElse(List.of());
    }

    private static List<Title> titles(Element biblio) {
        return children(biblio, "invention-title").stream()
                .map(t -> new Title(attribute(t, "lang"), new Field(t.getTextContent(), LocatingDomBuilder.lineOf(t))))
                .toList();
    }

    /** nationality and residence wrap a country element. */
    private static Field nested(Element parent, String name) {
        return child(parent, name)
                .map(e -> text(e, "country"))
                .orElse(Field.missing(LocatingDomBuilder.lineOf(parent)));
    }

    private static Field text(Element parent, String name) {
        return child(parent, name)
                .map(e -> new Field(e.getTextContent(), LocatingDomBuilder.lineOf(e)))
                .orElse(Field.missing(LocatingDomBuilder.lineOf(parent)));
    }

    private static Field attribute(Element element, String name) {
        String value = element.hasAttribute(name) ? element.getAttribute(name) : null;
        return new Field(value, LocatingDomBuilder.lineOf(element));
    }

    private static Optional<Element> child(Element parent, String name) {
        List<Element> found = children(parent, name);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e && e.getTagName().equals(name)) {
                result.add(e);
            }
        }
        return result;
    }
}
