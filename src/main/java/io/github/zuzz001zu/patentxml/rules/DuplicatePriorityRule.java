package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.PriorityClaim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class DuplicatePriorityRule implements Rule {

    @Override
    public String id() {
        return "PRI-03";
    }

    @Override
    public String description() {
        return "The same earlier application is not claimed twice";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        Map<String, PriorityClaim> seen = new HashMap<>();
        List<Finding> findings = new ArrayList<>();
        for (PriorityClaim claim : document.priorityClaims()) {
            String key = claim.country().text() + " " + normalize(claim.number().text());
            PriorityClaim first = seen.putIfAbsent(key, claim);
            if (first != null) {
                findings.add(error(claim.line(), "priority claim " + claim.sequence().text()
                        + " repeats claim " + first.sequence().text() + " (" + key + ")"));
            }
        }
        return findings;
    }

    /** "23187654.2" and "23 187 654.2" are the same number. */
    private static String normalize(String number) {
        return number.replaceAll("\\s", "");
    }
}
