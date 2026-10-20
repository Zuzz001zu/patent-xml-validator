package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;

import java.util.List;

/**
 * A business rule applied to a document that is already valid against the schema,
 * so required elements are known to be present.
 */
public interface Rule {

    String id();

    String description();

    List<Finding> check(PatentDocument document);

    default Finding error(int line, String message) {
        return new Finding(id(), Severity.ERROR, line, message);
    }

    default Finding warning(int line, String message) {
        return new Finding(id(), Severity.WARNING, line, message);
    }
}
