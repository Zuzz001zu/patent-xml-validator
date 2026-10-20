package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.PatentDocument;

import java.util.List;
import java.util.regex.Pattern;

/**
 * IPC symbol: section (A-H), class (2 digits), subclass (letter), main group (1-4 digits)
 * and subgroup (2-6 digits), e.g. "A61K 31/496" or "G06F 16/00".
 */
final class IpcSymbolRule implements Rule {

    private static final Pattern SYMBOL = Pattern.compile("[A-H]\\d{2}[A-Z] +\\d{1,4}/\\d{2,6}");

    @Override
    public String id() {
        return "CLS-01";
    }

    @Override
    public String description() {
        return "IPC symbols are well formed (e.g. A61K 31/496)";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        return document.classifications().stream()
                .filter(c -> !SYMBOL.matcher(c.symbol().text()).matches())
                .map(c -> error(c.symbol().line(), "'" + c.symbol().text() + "' is not a valid IPC symbol"))
                .toList();
    }

    static boolean isValid(String symbol) {
        return SYMBOL.matcher(symbol).matches();
    }
}
