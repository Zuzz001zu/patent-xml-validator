package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.PriorityClaim;

import java.util.ArrayList;
import java.util.List;

/** The kind of a priority claim must match the office of the earlier application. */
final class PriorityClaimKindRule implements Rule {

    @Override
    public String id() {
        return "PRI-02";
    }

    @Override
    public String description() {
        return "Priority claim kind matches the office: national, regional (EP, EA, AP, OA, GC) or PCT";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        List<Finding> findings = new ArrayList<>();
        for (PriorityClaim claim : document.priorityClaims()) {
            String country = claim.country().text();
            String seq = "priority claim " + claim.sequence().text();
            if (claim.isNational() && !Codes.isCountry(country)) {
                findings.add(error(claim.country().line(), seq + ": " + country + " is not a national office"));
            } else if (claim.isRegional() && !Codes.REGIONAL_OFFICES.contains(country)) {
                findings.add(error(claim.country().line(), seq + ": " + country + " is not a regional patent office"));
            } else if (claim.isInternational() && !ApplicationNumberRule.isWellFormed(claim.number().text())) {
                findings.add(error(claim.number().line(),
                        seq + ": '" + claim.number().text() + "' is not an international application number"));
            }
        }
        return findings;
    }
}
