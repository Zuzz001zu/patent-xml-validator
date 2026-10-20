package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PublicationNumberRule implements Rule {

    private static final Pattern NUMBER = Pattern.compile("(\\d{4})/(\\d{6})");

    @Override
    public String id() {
        return "PUB-02";
    }

    @Override
    public String description() {
        return "Publication number has the form YYYY/NNNNNN and YYYY is the publication year";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        Field number = document.publication().number();
        Matcher m = NUMBER.matcher(number.text());
        if (!m.matches()) {
            return List.of(error(number.line(),
                    "publication number '" + number.text() + "' is not in the form YYYY/NNNNNN"));
        }
        return Dates.parse(document.publication().date())
                .filter(date -> date.getYear() != Integer.parseInt(m.group(1)))
                .map(date -> List.of(error(number.line(), "publication number year " + m.group(1)
                        + " does not match publication date " + Dates.display(date))))
                .orElse(List.of());
    }
}
