package io.github.zuzz001zu.patentxml.rules;

import java.util.Locale;
import java.util.Set;

/** Code lists used by the rules (WIPO ST.3 country codes, ISO 639-1 languages). */
public final class Codes {

    /** Regional patent offices that can be the source of a priority claim. */
    public static final Set<String> REGIONAL_OFFICES = Set.of("EP", "EA", "AP", "OA", "GC");

    /** Offices that can act as PCT receiving office besides national offices. */
    public static final Set<String> NON_NATIONAL_RECEIVING_OFFICES = Set.of("IB", "EP", "EA", "AP", "OA");

    /** PCT Rule 48.3(a): languages of international publication. */
    public static final Set<String> PUBLICATION_LANGUAGES =
            Set.of("ar", "de", "en", "es", "fr", "ja", "ko", "pt", "ru", "zh");

    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    private static final Set<String> LANGUAGES = Set.of(Locale.getISOLanguages());

    private Codes() {
    }

    public static boolean isCountry(String code) {
        return COUNTRIES.contains(code);
    }

    public static boolean isReceivingOffice(String code) {
        return isCountry(code) || NON_NATIONAL_RECEIVING_OFFICES.contains(code);
    }

    public static boolean isLanguage(String code) {
        return LANGUAGES.contains(code);
    }
}
