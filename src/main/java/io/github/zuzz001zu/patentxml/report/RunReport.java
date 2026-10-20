package io.github.zuzz001zu.patentxml.report;

import io.github.zuzz001zu.patentxml.ValidationResult;
import io.github.zuzz001zu.patentxml.store.Table;

import java.time.LocalDateTime;
import java.util.List;

/** Everything a report needs about one run: the detailed results and the SQL summaries. */
public record RunReport(
        long runId,
        LocalDateTime startedAt,
        String source,
        String toolVersion,
        List<ValidationResult> results,
        Table findingsByRule,
        Table byReceivingOffice,
        Table history) {

    public RunReport {
        results = List.copyOf(results);
    }

    public long count(ValidationResult.Status status) {
        return results.stream().filter(r -> r.status() == status).count();
    }
}
