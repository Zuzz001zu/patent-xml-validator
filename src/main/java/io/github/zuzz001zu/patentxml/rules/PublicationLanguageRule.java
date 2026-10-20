package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;

import java.util.List;

final class PublicationLanguageRule implements Rule {

    @Override
    public String id() {
        return "PUB-04";
    }

    @Override
    public String description() {
        return "Publication language is one of the ten PCT publication languages (Rule 48.3(a))";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        Field lang = document.publicationLanguage();
        if (Codes.PUBLICATION_LANGUAGES.contains(lang.text())) {
            return List.of();
        }
        return List.of(error(lang.line(), "'" + lang.text() + "' is not a language of international publication"));
    }
}
