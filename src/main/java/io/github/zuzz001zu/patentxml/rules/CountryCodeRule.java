package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.Party;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

final class CountryCodeRule implements Rule {

    @Override
    public String id() {
        return "CTY-01";
    }

    @Override
    public String description() {
        return "Nationality and residence of applicants and inventors are valid country codes";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        List<Finding> findings = new ArrayList<>();
        Stream.concat(document.applicants().stream(), document.inventors().stream())
                .forEach(party -> {
                    check(party, "nationality", party.nationality(), findings);
                    check(party, "residence", party.residence(), findings);
                });
        return findings;
    }

    private void check(Party party, String what, Field country, List<Finding> findings) {
        if (country.isPresent() && !Codes.isCountry(country.text())) {
            findings.add(error(country.line(),
                    what + " of " + party.name().text() + " is " + country.text() + ", which is not a country code"));
        }
    }
}
