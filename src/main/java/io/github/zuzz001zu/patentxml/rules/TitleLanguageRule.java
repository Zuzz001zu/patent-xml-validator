package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.Title;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class TitleLanguageRule implements Rule {

    @Override
    public String id() {
        return "TTL-01";
    }

    @Override
    public String description() {
        return "There is a title in the publication language and at most one title per language";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        List<Finding> findings = new ArrayList<>();
        String language = document.publicationLanguage().text();
        Set<String> seen = new HashSet<>();

        for (Title title : document.titles()) {
            if (!seen.add(title.lang().text())) {
                findings.add(error(title.lang().line(), "two titles in language '" + title.lang().text() + "'"));
            }
        }
        if (!seen.contains(language)) {
            findings.add(error(document.publicationLanguage().line(),
                    "no title in the publication language '" + language + "'"));
        }
        return findings;
    }
}
