package io.github.zuzz001zu.patentxml.model;

/** Country, number, kind and date of a publication or an application. */
public record DocumentId(Field country, Field number, Field kind, Field date) {
}
