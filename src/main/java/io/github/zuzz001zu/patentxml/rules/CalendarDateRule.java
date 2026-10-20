package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.PriorityClaim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The schema only checks that a date has eight digits; 20250231 would pass it. */
final class CalendarDateRule implements Rule {

    @Override
    public String id() {
        return "DAT-01";
    }

    @Override
    public String description() {
        return "Every date is a real calendar date";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        Map<String, Field> dates = new LinkedHashMap<>();
        dates.put("publication date", document.publication().date());
        dates.put("filing date", document.application().date());
        for (PriorityClaim claim : document.priorityClaims()) {
            dates.put("priority date (claim " + claim.sequence().text() + ")", claim.date());
        }

        List<Finding> findings = new ArrayList<>();
        dates.forEach((label, field) -> {
            if (field.isPresent() && Dates.parse(field).isEmpty()) {
                findings.add(error(field.line(), label + " " + field.text() + " is not a valid date"));
            }
        });
        return findings;
    }
}
