package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Field;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

/** Dates are written YYYYMMDD, as in WIPO ST.36. */
public final class Dates {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd.MM.uuuu");

    private Dates() {
    }

    public static Optional<LocalDate> parse(Field field) {
        if (!field.isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(field.text(), FORMAT));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    public static String display(LocalDate date) {
        return date.format(DISPLAY);
    }
}
