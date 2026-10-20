package io.github.zuzz001zu.patentxml;

import io.github.zuzz001zu.patentxml.rules.Rules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        Main main = new Main(new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return main.run(args);
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private static String[] validate(Path dir, String input, String... extra) {
        String[] base = {"validate", input,
                "--db", dir.resolve("db").toString(),
                "--report", dir.resolve("report.html").toString(),
                "--csv", dir.resolve("findings.csv").toString()};
        String[] all = new String[base.length + extra.length];
        System.arraycopy(base, 0, all, 0, base.length);
        System.arraycopy(extra, 0, all, base.length, extra.length);
        return all;
    }

    @Test
    void validatingTheSamplesFailsBecauseSomeAreInvalid(@TempDir Path dir) throws IOException {
        assertEquals(Main.FINDINGS, run(validate(dir, "samples")));

        assertTrue(stdout().contains("12 documents: 3 valid, 3 with warnings, 6 invalid"), stdout());
        assertTrue(Files.readString(dir.resolve("report.html")).contains("Patent XML validation report"));
        assertTrue(Files.exists(dir.resolve("findings.csv")));
    }

    @Test
    void failOnNeverAlwaysSucceeds(@TempDir Path dir) {
        assertEquals(Main.OK, run(validate(dir, "samples", "--fail-on", "never")));
    }

    @Test
    void warningsFailOnlyWhenAskedTo(@TempDir Path dir) {
        String file = "samples/wo2025-126003-helvetic-battery.xml";
        assertEquals(Main.OK, run(validate(dir, file)));
        assertEquals(Main.FINDINGS, run(validate(dir, file, "--fail-on", "warning")));
    }

    @Test
    void missingInputIsAUsageError(@TempDir Path dir) {
        assertEquals(Main.USAGE, run(validate(dir, "no-such-folder")));
        assertTrue(stderr().contains("not found"), stderr());
    }

    @Test
    void unknownOptionIsAUsageError() {
        assertEquals(Main.USAGE, run("validate", "samples", "--colour"));
        assertTrue(stderr().contains("unknown option: --colour"));
    }

    @Test
    void invalidFailOnValueIsAUsageError() {
        assertEquals(Main.USAGE, run("validate", "samples", "--fail-on", "sometimes"));
    }

    @Test
    void noArgumentsPrintsUsage() {
        assertEquals(Main.USAGE, run());
        assertTrue(stderr().contains("Usage:"));
    }

    @Test
    void listsTheRules() {
        assertEquals(Main.OK, run("rules"));
        long lines = stdout().lines().count();
        assertEquals(Rules.all().size() + 2, lines);
        assertTrue(stdout().contains("PRI-01"));
    }

    @Test
    void printsTheVersion() {
        assertEquals(Main.OK, run("--version"));
        assertTrue(stdout().startsWith("patent-xml-validator "));
    }
}
