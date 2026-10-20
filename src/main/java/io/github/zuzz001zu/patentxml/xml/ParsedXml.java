package io.github.zuzz001zu.patentxml.xml;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.rules.Finding;

import java.util.List;
import java.util.Optional;

/**
 * Result of reading one file: the document when it is structurally valid,
 * otherwise the structural problems (not well-formed, schema violations).
 */
public record ParsedXml(PatentDocument document, List<Finding> structuralErrors) {

    public ParsedXml {
        structuralErrors = List.copyOf(structuralErrors);
    }

    public Optional<PatentDocument> documentIfValid() {
        return structuralErrors.isEmpty() ? Optional.ofNullable(document) : Optional.empty();
    }
}
