package io.github.zuzz001zu.patentxml.model;

/** An applicant or an inventor. */
public record Party(int line, Field sequence, Field name, Field nationality, Field residence) {
}
