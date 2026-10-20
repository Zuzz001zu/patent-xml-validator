package io.github.zuzz001zu.patentxml.report;

import io.github.zuzz001zu.patentxml.ValidationResult;
import io.github.zuzz001zu.patentxml.ValidationResult.Status;
import io.github.zuzz001zu.patentxml.rules.Finding;
import io.github.zuzz001zu.patentxml.rules.Rule;
import io.github.zuzz001zu.patentxml.rules.Rules;
import io.github.zuzz001zu.patentxml.rules.Severity;
import io.github.zuzz001zu.patentxml.store.Table;
import io.github.zuzz001zu.patentxml.xml.PatentXmlParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Self-contained HTML report (no external CSS or scripts), easy to attach to an e-mail. */
public final class HtmlReport {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String CSS = """
            body { font-family: -apple-system, "Segoe UI", Roboto, Arial, sans-serif; margin: 0;
                   color: #1f2933; background: #f5f7fa; }
            main { max-width: 1100px; margin: 0 auto; padding: 32px 24px 64px; }
            h1 { font-size: 24px; margin: 0 0 4px; color: #102a43; }
            h2 { font-size: 17px; margin: 36px 0 12px; color: #102a43; }
            .meta { color: #627d98; font-size: 13px; }
            .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
                     gap: 12px; margin-top: 24px; }
            .card { background: #fff; border: 1px solid #d9e2ec; border-radius: 8px; padding: 14px 16px; }
            .card .n { font-size: 26px; font-weight: 600; }
            .card .l { font-size: 12px; color: #627d98; text-transform: uppercase; letter-spacing: .04em; }
            table { width: 100%; border-collapse: collapse; background: #fff; font-size: 13px;
                    border: 1px solid #d9e2ec; }
            th { text-align: left; background: #102a43; color: #fff; font-weight: 600; padding: 8px 10px; }
            td { padding: 7px 10px; border-top: 1px solid #e4e7eb; vertical-align: top; }
            td.num, th.num { text-align: right; font-variant-numeric: tabular-nums; }
            .badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px;
                     font-weight: 600; letter-spacing: .03em; }
            .VALID { background: #e3f9e5; color: #1f7a33; }
            .WARNING { background: #fff3c4; color: #8d6b00; }
            .INVALID, .ERROR { background: #ffe3e3; color: #a61b1b; }
            details { background: #fff; border: 1px solid #d9e2ec; border-radius: 8px; margin: 8px 0; }
            summary { cursor: pointer; padding: 10px 14px; display: flex; gap: 12px; align-items: center; }
            summary .file { font-family: ui-monospace, Consolas, monospace; font-size: 13px; }
            summary .pub { color: #627d98; font-size: 13px; margin-left: auto; }
            details table { border: none; border-top: 1px solid #d9e2ec; }
            .ok { padding: 10px 14px; color: #627d98; font-size: 13px; border-top: 1px solid #d9e2ec; }
            footer { margin-top: 40px; color: #9fb3c8; font-size: 12px; }
            """;

    private final Map<String, String> descriptions = new HashMap<>();

    public HtmlReport() {
        for (Rule rule : Rules.all()) {
            descriptions.put(rule.id(), rule.description());
        }
        descriptions.put(PatentXmlParser.NOT_WELL_FORMED, "The file is well-formed XML");
        descriptions.put(PatentXmlParser.SCHEMA_VIOLATION, "The document is valid against the schema");
    }

    public Path write(RunReport report, Path file) throws IOException {
        Files.writeString(file, render(report), StandardCharsets.UTF_8);
        return file;
    }

    public String render(RunReport r) {
        StringBuilder html = new StringBuilder(16_384);
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Validation report – run ").append(r.runId()).append("</title>\n")
                .append("<style>").append(CSS).append("</style>\n</head>\n<body>\n<main>\n");

        html.append("<h1>Patent XML validation report</h1>\n")
                .append("<div class=\"meta\">Run ").append(r.runId()).append(" · ")
                .append(r.startedAt().format(TIMESTAMP)).append(" · ").append(esc(r.source()))
                .append(" · patent-xml-validator ").append(esc(r.toolVersion())).append("</div>\n");

        long errors = r.results().stream().mapToLong(x -> x.count(Severity.ERROR)).sum();
        long warnings = r.results().stream().mapToLong(x -> x.count(Severity.WARNING)).sum();
        html.append("<div class=\"cards\">\n");
        card(html, r.results().size(), "documents");
        card(html, r.count(Status.VALID), "valid");
        card(html, r.count(Status.WARNING), "with warnings");
        card(html, r.count(Status.INVALID), "invalid");
        card(html, errors, "errors");
        card(html, warnings, "warnings");
        html.append("</div>\n");

        html.append("<h2>Findings by rule</h2>\n");
        if (r.findingsByRule().isEmpty()) {
            html.append("<p class=\"meta\">No findings.</p>\n");
        } else {
            html.append("<table><tr><th>Rule</th><th>Check</th><th>Severity</th>")
                    .append("<th class=\"num\">Occurrences</th><th class=\"num\">Documents</th></tr>\n");
            Table t = r.findingsByRule();
            for (int i = 0; i < t.rows().size(); i++) {
                String rule = String.valueOf(t.value(i, "rule_id"));
                html.append("<tr><td>").append(esc(rule)).append("</td><td>")
                        .append(esc(descriptions.getOrDefault(rule, ""))).append("</td><td>")
                        .append(badge(String.valueOf(t.value(i, "severity")))).append("</td>")
                        .append(num(t.value(i, "occurrences"))).append(num(t.value(i, "documents")))
                        .append("</tr>\n");
            }
            html.append("</table>\n");
        }

        html.append("<h2>By receiving office</h2>\n");
        table(html, r.byReceivingOffice(),
                new String[] {"Receiving office", "Documents", "Invalid", "With warnings", "Valid %"});

        html.append("<h2>Documents</h2>\n");
        for (ValidationResult result : r.results()) {
            document(html, result);
        }

        if (r.history().rows().size() > 1) {
            html.append("<h2>Previous runs</h2>\n");
            table(html, r.history(), new String[] {"Run", "Started", "Documents", "Invalid", "Errors"});
        }

        html.append("<footer>Line numbers refer to the original XML files.</footer>\n")
                .append("</main>\n</body>\n</html>\n");
        return html.toString();
    }

    private void document(StringBuilder html, ValidationResult result) {
        boolean open = result.status() == Status.INVALID;
        html.append(open ? "<details open>" : "<details>").append("<summary>")
                .append(badge(result.status().name()))
                .append("<span class=\"file\">").append(esc(result.fileName())).append("</span>")
                .append("<span class=\"pub\">")
                .append(esc(result.publicationNumber().map(n -> "WO " + n).orElse("")))
                .append(result.applicationNumber().map(n -> " · " + esc(n)).orElse(""))
                .append("</span></summary>\n");

        if (result.findings().isEmpty()) {
            html.append("<div class=\"ok\">All checks passed.</div>\n");
        } else {
            html.append("<table><tr><th class=\"num\">Line</th><th>Rule</th>")
                    .append("<th>Severity</th><th>Message</th></tr>\n");
            for (Finding f : result.findings()) {
                html.append("<tr>").append(num(f.line() > 0 ? f.line() : "")).append("<td>")
                        .append(esc(f.ruleId())).append("</td><td>").append(badge(f.severity().name()))
                        .append("</td><td>").append(esc(f.message())).append("</td></tr>\n");
            }
            html.append("</table>\n");
        }
        html.append("</details>\n");
    }

    private static void table(StringBuilder html, Table t, String[] headers) {
        html.append("<table><tr>");
        List<Object> first = t.isEmpty() ? List.of() : t.rows().getFirst();
        for (int i = 0; i < headers.length; i++) {
            boolean numeric = i < first.size() && first.get(i) instanceof Number;
            html.append(numeric ? "<th class=\"num\">" : "<th>").append(esc(headers[i])).append("</th>");
        }
        html.append("</tr>\n");
        for (var row : t.rows()) {
            html.append("<tr>");
            for (Object value : row) {
                html.append(value instanceof Number ? num(value) : "<td>" + esc(format(value)) + "</td>");
            }
            html.append("</tr>\n");
        }
        html.append("</table>\n");
    }

    private static void card(StringBuilder html, long n, String label) {
        html.append("<div class=\"card\"><div class=\"n\">").append(n)
                .append("</div><div class=\"l\">").append(label).append("</div></div>\n");
    }

    private static String badge(String status) {
        return "<span class=\"badge " + esc(status) + "\">" + esc(status) + "</span>";
    }

    private static String num(Object value) {
        return "<td class=\"num\">" + esc(format(value)) + "</td>";
    }

    private static String format(Object value) {
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().format(TIMESTAMP);
        }
        return value == null ? "" : String.valueOf(value);
    }

    static String esc(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
