package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * International application numbers have the form PCT/CCYYYY/NNNNNN, where CC is the
 * receiving office and YYYY the year of the international filing date.
 */
public final class ApplicationNumberRule implements Rule {

    private static final Pattern NUMBER = Pattern.compile("PCT/([A-Z]{2})(\\d{4})/(\\d{6})");

    /** The receiving office code, if the number is well formed. */
    public static Optional<String> receivingOffice(String applicationNumber) {
        Matcher m = NUMBER.matcher(applicationNumber == null ? "" : applicationNumber.strip());
        return m.matches() ? Optional.of(m.group(1)) : Optional.empty();
    }

    static boolean isWellFormed(String number) {
        return NUMBER.matcher(number).matches();
    }

    @Override
    public String id() {
        return "APP-01";
    }

    @Override
    public String description() {
        return "Application number has the form PCT/CCYYYY/NNNNNN with a known receiving office and the filing year";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        Field number = document.application().number();
        Matcher m = NUMBER.matcher(number.text());
        if (!m.matches()) {
            return List.of(error(number.line(),
                    "application number '" + number.text() + "' is not in the form PCT/CCYYYY/NNNNNN"));
        }

        List<Finding> findings = new ArrayList<>();
        String office = m.group(1);
        if (!Codes.isReceivingOffice(office)) {
            findings.add(error(number.line(), office + " is not a known receiving office code"));
        }
        int year = Integer.parseInt(m.group(2));
        Dates.parse(document.application().date())
                .filter(filed -> filed.getYear() != year)
                .ifPresent(filed -> findings.add(error(number.line(), "year " + year
                        + " in the application number does not match filing date " + Dates.display(filed))));
        return findings;
    }
}
