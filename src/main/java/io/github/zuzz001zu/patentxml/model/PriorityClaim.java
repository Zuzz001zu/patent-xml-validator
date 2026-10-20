package io.github.zuzz001zu.patentxml.model;

public record PriorityClaim(int line, Field sequence, Field kind, Field country, Field number, Field date) {

    public boolean isNational() {
        return "national".equals(kind.text());
    }

    public boolean isRegional() {
        return "regional".equals(kind.text());
    }

    public boolean isInternational() {
        return "international".equals(kind.text());
    }
}
