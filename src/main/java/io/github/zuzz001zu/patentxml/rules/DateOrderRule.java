package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.PriorityClaim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class DateOrderRule implements Rule {

    @Override
    public String id() {
        return "DAT-02";
    }

    @Override
    public String description() {
        return "Priority dates are before the filing date, and the filing date before publication";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        List<Finding> findings = new ArrayList<>();
        Optional<LocalDate> filed = Dates.parse(document.application().date());
        Optional<LocalDate> published = Dates.parse(document.publication().date());

        if (filed.isPresent() && published.isPresent() && published.get().isBefore(filed.get())) {
            findings.add(error(document.publication().date().line(),
                    "publication date " + Dates.display(published.get())
                            + " is before the filing date " + Dates.display(filed.get())));
        }
        if (filed.isPresent()) {
            for (PriorityClaim claim : document.priorityClaims()) {
                Dates.parse(claim.date())
                        .filter(priority -> !priority.isBefore(filed.get()))
                        .ifPresent(priority -> findings.add(error(claim.date().line(),
                                "priority date " + Dates.display(priority) + " (claim " + claim.sequence().text()
                                        + ") is not before the filing date " + Dates.display(filed.get()))));
            }
        }
        return findings;
    }
}
