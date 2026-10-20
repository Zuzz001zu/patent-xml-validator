package io.github.zuzz001zu.patentxml.rules;

import java.util.List;

public final class Rules {

    private static final List<Rule> ALL = List.of(
            new PublishingOfficeRule(),
            new PublicationNumberRule(),
            new KindCodeRule(),
            new PublicationLanguageRule(),
            new EarlyPublicationRule(),
            new ApplicationNumberRule(),
            new CalendarDateRule(),
            new DateOrderRule(),
            new PriorityPeriodRule(),
            new PriorityClaimKindRule(),
            new DuplicatePriorityRule(),
            new IpcSymbolRule(),
            new SequenceRule(),
            new CountryCodeRule(),
            new LanguageCodeRule(),
            new TitleLanguageRule(),
            new TitleLengthRule());

    private Rules() {
    }

    public static List<Rule> all() {
        return ALL;
    }
}
