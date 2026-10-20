package io.github.zuzz001zu.patentxml.report;

import io.github.zuzz001zu.patentxml.ValidationResult;
import io.github.zuzz001zu.patentxml.rules.Finding;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** One line per finding, for Excel or for loading into a ticketing tool. */
public final class CsvExport {

    private static final String HEADER = "file;publication_number;application_number;rule_id;severity;line;message";

    private CsvExport() {
    }

    public static Path write(List<ValidationResult> results, Path file) throws IOException {
        try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write('﻿'); // BOM so that Excel opens UTF-8 correctly
            out.write(HEADER + "\n");
            for (ValidationResult r : results) {
                for (Finding f : r.findings()) {
                    out.write(String.join(";",
                            cell(r.fileName()),
                            cell(r.publicationNumber().orElse("")),
                            cell(r.applicationNumber().orElse("")),
                            cell(f.ruleId()),
                            cell(f.severity().name()),
                            f.line() > 0 ? String.valueOf(f.line()) : "",
                            cell(f.message())));
                    out.write("\n");
                }
            }
        }
        return file;
    }

    static String cell(String value) {
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
