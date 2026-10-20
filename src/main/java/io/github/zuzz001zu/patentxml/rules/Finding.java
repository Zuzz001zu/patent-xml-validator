package io.github.zuzz001zu.patentxml.rules;

/**
 * One problem found in a document.
 *
 * @param line line in the XML file, or 0 when the problem is not tied to a line
 */
public record Finding(String ruleId, Severity severity, int line, String message) {

    public boolean isError() {
        return severity == Severity.ERROR;
    }
}
