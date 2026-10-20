package io.github.zuzz001zu.patentxml;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.rules.ApplicationNumberRule;
import io.github.zuzz001zu.patentxml.rules.Finding;
import io.github.zuzz001zu.patentxml.rules.Severity;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Outcome of validating one file. */
public record ValidationResult(String fileName, PatentDocument document, List<Finding> findings) {

    public enum Status { VALID, WARNING, INVALID }

    public ValidationResult {
        findings = findings.stream()
                .sorted(Comparator.comparingInt(Finding::line).thenComparing(Finding::ruleId))
                .toList();
    }

    public Status status() {
        if (findings.stream().anyMatch(Finding::isError)) {
            return Status.INVALID;
        }
        return findings.isEmpty() ? Status.VALID : Status.WARNING;
    }

    public long count(Severity severity) {
        return findings.stream().filter(f -> f.severity() == severity).count();
    }

    public Optional<String> publicationNumber() {
        return Optional.ofNullable(document).map(d -> d.publication().number().text());
    }

    public Optional<String> applicationNumber() {
        return Optional.ofNullable(document).map(d -> d.application().number().text());
    }

    public Optional<String> receivingOffice() {
        return applicationNumber().flatMap(ApplicationNumberRule::receivingOffice);
    }
}
