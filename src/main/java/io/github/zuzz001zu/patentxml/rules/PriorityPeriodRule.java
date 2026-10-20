package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.PriorityClaim;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The international application must be filed within 12 months of the priority date
 * (Paris Convention, Art. 4C; PCT Art. 8). Within two more months the right of priority
 * can still be restored (PCT Rule 26bis.3), so that case is a warning, not an error.
 *
 * Extension of the period when the last day falls on a non-working day (Rule 80.5)
 * is not taken into account.
 */
final class PriorityPeriodRule implements Rule {

    @Override
    public String id() {
        return "PRI-01";
    }

    @Override
    public String description() {
        return "Filed within 12 months of each priority date (up to 14 months with restoration)";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        Optional<LocalDate> filed = Dates.parse(document.application().date());
        if (filed.isEmpty()) {
            return List.of();
        }

        List<Finding> findings = new ArrayList<>();
        for (PriorityClaim claim : document.priorityClaims()) {
            Optional<LocalDate> priority = Dates.parse(claim.date());
            if (priority.isEmpty()) {
                continue;
            }
            LocalDate periodEnd = priority.get().plusMonths(12);
            if (!filed.get().isAfter(periodEnd)) {
                continue;
            }
            long late = ChronoUnit.DAYS.between(periodEnd, filed.get());
            String where = "priority claim " + claim.sequence().text() + " (" + Dates.display(priority.get()) + ")";
            if (!filed.get().isAfter(priority.get().plusMonths(14))) {
                findings.add(warning(claim.line(), where + ": filed " + late + " days after the 12-month period,"
                        + " restoration of the right of priority is needed (Rule 26bis.3)"));
            } else {
                findings.add(error(claim.line(), where + ": filed more than 14 months after the priority date"));
            }
        }
        return findings;
    }
}
