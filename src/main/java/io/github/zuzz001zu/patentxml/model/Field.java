package io.github.zuzz001zu.patentxml.model;

/**
 * A value read from the XML document, with the line it was found on so that
 * findings can point to the right place in the file.
 */
public record Field(String value, int line) {

    public static Field missing(int line) {
        return new Field(null, line);
    }

    public boolean isPresent() {
        return value != null && !value.isBlank();
    }

    /** The value without surrounding whitespace, or an empty string. */
    public String text() {
        return value == null ? "" : value.strip();
    }
}
