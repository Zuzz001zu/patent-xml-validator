package io.github.zuzz001zu.patentxml;

import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.rules.Finding;
import io.github.zuzz001zu.patentxml.rules.Rule;
import io.github.zuzz001zu.patentxml.rules.Rules;
import io.github.zuzz001zu.patentxml.xml.ParsedXml;
import io.github.zuzz001zu.patentxml.xml.PatentXmlParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Validates files in two passes: structure first (well-formed XML, schema), then the
 * business rules. Rules only run on structurally valid documents, otherwise every
 * missing element would produce a cascade of meaningless findings.
 */
public final class DocumentValidator {

    private final PatentXmlParser parser = new PatentXmlParser();
    private final List<Rule> rules;

    public DocumentValidator() {
        this(Rules.all());
    }

    DocumentValidator(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public ValidationResult validate(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return validate(file.getFileName().toString(), in);
        }
    }

    public ValidationResult validate(String name, InputStream in) throws IOException {
        ParsedXml parsed = parser.parse(in);
        if (parsed.documentIfValid().isEmpty()) {
            return new ValidationResult(name, null, parsed.structuralErrors());
        }
        PatentDocument document = parsed.document();
        List<Finding> findings = new ArrayList<>();
        for (Rule rule : rules) {
            findings.addAll(rule.check(document));
        }
        return new ValidationResult(name, document, findings);
    }

    /** Validates the given files, and every *.xml file inside the given folders. */
    public List<ValidationResult> validateAll(List<Path> paths) throws IOException {
        List<ValidationResult> results = new ArrayList<>();
        for (Path file : expand(paths)) {
            results.add(validate(file));
        }
        return results;
    }

    static List<Path> expand(List<Path> paths) throws IOException {
        List<Path> files = new ArrayList<>();
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                try (Stream<Path> walk = Files.walk(path)) {
                    walk.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".xml"))
                            .sorted()
                            .forEach(files::add);
                }
            } else if (Files.isRegularFile(path)) {
                files.add(path);
            } else {
                throw new NoSuchFileException(path.toString());
            }
        }
        return files;
    }
}
