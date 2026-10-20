package io.github.zuzz001zu.patentxml.rules;

import java.util.List;

public final class Rules {

    private static final List<Rule> ALL = List.of(
            new PublishingOfficeRule(),
            new PublicationNumberRule(),
            new KindCodeRule(),
            new PublicationLanguageRule(),
            new EarlyPublicationRule(),
            new ApplicationNumberRule());

    private Rules() {
    }

    public static List<Rule> all() {
        return ALL;
    }
}
