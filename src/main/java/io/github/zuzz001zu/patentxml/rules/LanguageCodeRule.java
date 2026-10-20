package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.Title;

import java.util.ArrayList;
import java.util.List;

final class LanguageCodeRule implements Rule {

    @Override
    public String id() {
        return "LNG-01";
    }

    @Override
    public String description() {
        return "Filing language and title languages are ISO 639-1 codes";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        List<Field> codes = new ArrayList<>();
        codes.add(document.filingLanguage());
        document.titles().stream().map(Title::lang).forEach(codes::add);

        return codes.stream()
                .filter(code -> !Codes.isLanguage(code.text()))
                .map(code -> error(code.line(), "'" + code.text() + "' is not an ISO 639-1 language code"))
                .toList();
    }
}
