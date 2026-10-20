package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * International publication takes place promptly after 18 months from the priority
 * date (Article 21(2)(a)). An earlier first publication is only possible at the
 * applicant's request, so it is worth a second look.
 */
final class EarlyPublicationRule implements Rule {

    private static final Set<String> FIRST_PUBLICATION = Set.of("A1", "A2");

    @Override
    public String id() {
        return "PUB-05";
    }

    @Override
    public String description() {
        return "First publication before 18 months from the priority date needs a request for early publication";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        if (!FIRST_PUBLICATION.contains(document.publication().kind().text())) {
            return List.of();
        }
        Optional<LocalDate> published = Dates.parse(document.publication().date());
        Optional<LocalDate> priority = PriorityDate.of(document);
        if (published.isEmpty() || priority.isEmpty()) {
            return List.of();
        }
        LocalDate due = priority.get().plusMonths(18);
        if (!published.get().isBefore(due)) {
            return List.of();
        }
        return List.of(warning(document.publication().date().line(),
                "published on " + Dates.display(published.get()) + ", before the 18-month date "
                        + Dates.display(due) + ": check that early publication was requested (Art. 21(2)(b))"));
    }
}
