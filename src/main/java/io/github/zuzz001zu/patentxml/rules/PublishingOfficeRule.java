package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;

import java.util.List;

final class PublishingOfficeRule implements Rule {

    @Override
    public String id() {
        return "PUB-01";
    }

    @Override
    public String description() {
        return "International publications are published by WIPO (country code WO)";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        Field country = document.publication().country();
        if ("WO".equals(country.text())) {
            return List.of();
        }
        return List.of(error(country.line(), "publication country is " + country.text() + ", expected WO"));
    }
}
