package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.Party;

import java.util.List;
import java.util.Optional;

/**
 * A national receiving office is competent only if at least one applicant is a national
 * or a resident of that country (PCT Rule 19.1(a)). The International Bureau is always
 * competent. Regional offices are not checked: that needs the list of their member states.
 *
 * An application filed with an office that is not competent is forwarded to the
 * International Bureau (Rule 19.4) instead of being lost, so this is a warning.
 */
final class ReceivingOfficeRule implements Rule {

    @Override
    public String id() {
        return "APP-02";
    }

    @Override
    public String description() {
        return "A national receiving office matches the nationality or residence of an applicant (Rule 19.1)";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        Optional<String> office = ApplicationNumberRule.receivingOffice(document.application().number().text());
        if (office.isEmpty() || !Codes.isCountry(office.get())) {
            return List.of();
        }
        String ro = office.get();
        boolean competent = document.applicants().stream().anyMatch(a -> isNationalOrResident(a, ro));
        if (competent) {
            return List.of();
        }
        return List.of(warning(document.application().number().line(),
                "no applicant is a national or resident of " + ro + ", so RO/" + ro
                        + " is not competent: check whether the application was forwarded to RO/IB (Rule 19.4)"));
    }

    private static boolean isNationalOrResident(Party applicant, String country) {
        return country.equals(applicant.nationality().text()) || country.equals(applicant.residence().text());
    }
}
