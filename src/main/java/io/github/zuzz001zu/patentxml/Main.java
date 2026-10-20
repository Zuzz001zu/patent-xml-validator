package io.github.zuzz001zu.patentxml;

import io.github.zuzz001zu.patentxml.report.CsvExport;
import io.github.zuzz001zu.patentxml.report.HtmlReport;
import io.github.zuzz001zu.patentxml.report.RunReport;
import io.github.zuzz001zu.patentxml.rules.Rule;
import io.github.zuzz001zu.patentxml.rules.Rules;
import io.github.zuzz001zu.patentxml.rules.Severity;
import io.github.zuzz001zu.patentxml.store.ResultStore;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Command line entry point. */
public final class Main {

    static final int OK = 0;
    static final int FINDINGS = 1;
    static final int USAGE = 2;

    private static final String USAGE_TEXT = """
            Usage:
              patent-xml-validator validate <file-or-folder>... [options]
              patent-xml-validator rules
              patent-xml-validator --version

            Options for validate:
              --db <path>        H2 database file for the results (default: validation-results)
              --report <file>    HTML report (default: validation-report.html)
              --csv <file>       also write the findings as CSV
              --fail-on <level>  error | warning | never (default: error)

            Exit codes: 0 no blocking findings, 1 findings at or above --fail-on, 2 usage or I/O error.
            """;

    private final PrintStream out;
    private final PrintStream err;

    Main(PrintStream out, PrintStream err) {
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        System.exit(new Main(System.out, System.err).run(args));
    }

    int run(String[] args) {
        if (args.length == 0) {
            err.print(USAGE_TEXT);
            return USAGE;
        }
        try {
            return switch (args[0]) {
                case "validate" -> validate(Options.parse(List.of(args).subList(1, args.length)));
                case "rules" -> listRules();
                case "--version", "-V" -> {
                    out.println("patent-xml-validator " + version());
                    yield OK;
                }
                case "--help", "-h" -> {
                    out.print(USAGE_TEXT);
                    yield OK;
                }
                default -> throw new IllegalArgumentException("unknown command: " + args[0]);
            };
        } catch (IllegalArgumentException e) {
            err.println("error: " + e.getMessage());
            err.println();
            err.print(USAGE_TEXT);
            return USAGE;
        } catch (NoSuchFileException e) {
            err.println("error: file or folder not found: " + e.getMessage());
            return USAGE;
        } catch (IOException | SQLException e) {
            err.println("error: " + e.getMessage());
            return USAGE;
        }
    }

    private int validate(Options options) throws IOException, SQLException {
        LocalDateTime startedAt = LocalDateTime.now();
        List<ValidationResult> results = new DocumentValidator().validateAll(options.inputs());
        if (results.isEmpty()) {
            throw new IllegalArgumentException("no XML files found in " + options.inputs());
        }

        String source = options.inputs().stream().map(Path::toString).collect(Collectors.joining(", "));
        RunReport report;
        try (ResultStore store = ResultStore.open(ResultStore.h2File(options.database()))) {
            long runId = store.save(source, version(), results);
            report = new RunReport(runId, startedAt, source, version(), results,
                    store.report("findings_by_rule", runId),
                    store.report("by_receiving_office", runId),
                    store.report("run_history", runId));
        }

        new HtmlReport().write(report, options.report());
        if (options.csv() != null) {
            CsvExport.write(results, options.csv());
        }
        printSummary(report, options);
        return exitCode(results, options.failOn());
    }

    private void printSummary(RunReport report, Options options) {
        for (ValidationResult r : report.results()) {
            out.printf(Locale.ROOT, "%-8s %-40s %d error(s), %d warning(s)%n",
                    r.status(), r.fileName(), r.count(Severity.ERROR), r.count(Severity.WARNING));
        }
        out.printf(Locale.ROOT, "%n%d documents: %d valid, %d with warnings, %d invalid (run %d)%n",
                report.results().size(),
                report.count(ValidationResult.Status.VALID),
                report.count(ValidationResult.Status.WARNING),
                report.count(ValidationResult.Status.INVALID),
                report.runId());
        out.println("Report: " + options.report().toAbsolutePath());
        if (options.csv() != null) {
            out.println("CSV:    " + options.csv().toAbsolutePath());
        }
    }

    static int exitCode(List<ValidationResult> results, String failOn) {
        boolean fail = switch (failOn) {
            case "never" -> false;
            case "warning" -> results.stream().anyMatch(r -> !r.findings().isEmpty());
            default -> results.stream().anyMatch(r -> r.status() == ValidationResult.Status.INVALID);
        };
        return fail ? FINDINGS : OK;
    }

    private int listRules() {
        out.printf("%-7s %s%n", "XML-01", "The file is well-formed XML");
        out.printf("%-7s %s%n", "XSD-01", "The document is valid against the schema");
        for (Rule rule : Rules.all()) {
            out.printf("%-7s %s%n", rule.id(), rule.description());
        }
        return OK;
    }

    static String version() {
        String version = Main.class.getPackage().getImplementationVersion();
        return version == null ? "dev" : version;
    }

    record Options(List<Path> inputs, Path database, Path report, Path csv, String failOn) {

        static Options parse(List<String> args) {
            List<Path> inputs = new ArrayList<>();
            Path database = Path.of("validation-results");
            Path report = Path.of("validation-report.html");
            Path csv = null;
            String failOn = "error";

            for (int i = 0; i < args.size(); i++) {
                String arg = args.get(i);
                switch (arg) {
                    case "--db" -> database = Path.of(value(args, ++i, arg));
                    case "--report" -> report = Path.of(value(args, ++i, arg));
                    case "--csv" -> csv = Path.of(value(args, ++i, arg));
                    case "--fail-on" -> {
                        failOn = value(args, ++i, arg);
                        if (!List.of("error", "warning", "never").contains(failOn)) {
                            throw new IllegalArgumentException("--fail-on must be error, warning or never");
                        }
                    }
                    default -> {
                        if (arg.startsWith("--")) {
                            throw new IllegalArgumentException("unknown option: " + arg);
                        }
                        inputs.add(Path.of(arg));
                    }
                }
            }
            if (inputs.isEmpty()) {
                throw new IllegalArgumentException("give at least one XML file or folder");
            }
            return new Options(inputs, database, report, csv, failOn);
        }

        private static String value(List<String> args, int index, String option) {
            if (index >= args.size()) {
                throw new IllegalArgumentException(option + " needs a value");
            }
            return args.get(index);
        }
    }
}
