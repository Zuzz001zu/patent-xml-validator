package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;

import java.util.List;

/**
 * PCT Rule 4.3: the title shall be short, preferably two to seven words in English.
 * Long titles are common in practice, so only clearly excessive ones are flagged.
 */
final class TitleLengthRule implements Rule {

    static final int MAX_WORDS = 15;

    @Override
    public String id() {
        return "TTL-02";
    }

    @Override
    public String description() {
        return "English title is not longer than " + MAX_WORDS + " words (Rule 4.3 asks for a short title)";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        return document.titles().stream()
                .filter(t -> "en".equals(t.lang().text()))
                .filter(t -> words(t.text().text()) > MAX_WORDS)
                .map(t -> warning(t.text().line(),
                        "English title has " + words(t.text().text()) + " words, consider a shorter title"))
                .toList();
    }

    private static int words(String text) {
        return text.isBlank() ? 0 : text.strip().split("\\s+").length;
    }
}
