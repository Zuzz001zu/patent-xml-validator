package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;

import java.util.List;
import java.util.Map;

/** WIPO ST.16 kind codes used for WO publications. */
final class KindCodeRule implements Rule {

    static final Map<String, String> WO_KIND_CODES = Map.of(
            "A1", "with international search report",
            "A2", "without international search report",
            "A3", "later publication of the international search report",
            "A4", "later publication of amended claims",
            "A8", "corrected bibliographic data",
            "A9", "corrected complete document");

    @Override
    public String id() {
        return "PUB-03";
    }

    @Override
    public String description() {
        return "Kind code is one used for WO publications (A1, A2, A3, A4, A8, A9)";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        Field kind = document.publication().kind();
        if (WO_KIND_CODES.containsKey(kind.text())) {
            return List.of();
        }
        return List.of(error(kind.line(), "kind code " + kind.text() + " is not used for WO publications"));
    }
}
