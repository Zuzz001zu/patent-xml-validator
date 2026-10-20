package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.PriorityClaim;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The "priority date" of PCT Article 2(xi): the earliest claimed priority,
 * or the international filing date when there is no priority claim.
 */
final class PriorityDate {

    private PriorityDate() {
    }

    /** Empty when a date needed to work it out cannot be read (DAT-01 reports that). */
    static Optional<LocalDate> of(PatentDocument document) {
        List<Optional<LocalDate>> claimed = document.priorityClaims().stream()
                .map(PriorityClaim::date)
                .map(Dates::parse)
                .toList();
        if (claimed.isEmpty()) {
            return Dates.parse(document.application().date());
        }
        if (claimed.stream().anyMatch(Optional::isEmpty)) {
            return Optional.empty();
        }
        return claimed.stream().map(Optional::get).min(LocalDate::compareTo);
    }
}
