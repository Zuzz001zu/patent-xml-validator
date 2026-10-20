package io.github.zuzz001zu.patentxml.rules;

import io.github.zuzz001zu.patentxml.model.Classification;
import io.github.zuzz001zu.patentxml.model.Field;
import io.github.zuzz001zu.patentxml.model.PatentDocument;
import io.github.zuzz001zu.patentxml.model.Party;
import io.github.zuzz001zu.patentxml.model.PriorityClaim;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

/** sequence attributes number the items of a list 1, 2, 3... without gaps or repeats. */
final class SequenceRule implements Rule {

    @Override
    public String id() {
        return "SEQ-01";
    }

    @Override
    public String description() {
        return "Sequence numbers of priority claims, classifications, applicants and inventors run 1..n";
    }

    @Override
    public List<Finding> check(PatentDocument document) {
        List<Finding> findings = new ArrayList<>();
        checkList("priority claims", sequences(document.priorityClaims(), PriorityClaim::sequence), findings);
        checkList("classifications", sequences(document.classifications(), Classification::sequence), findings);
        checkList("applicants", sequences(document.applicants(), Party::sequence), findings);
        checkList("inventors", sequences(document.inventors(), Party::sequence), findings);
        return findings;
    }

    private void checkList(String name, List<Field> sequences, List<Finding> findings) {
        if (sequences.isEmpty()) {
            return;
        }
        List<Integer> actual = sequences.stream().map(f -> Integer.parseInt(f.text())).sorted().toList();
        List<Integer> expected = IntStream.rangeClosed(1, sequences.size()).boxed().toList();
        if (!actual.equals(expected)) {
            findings.add(error(sequences.getFirst().line(),
                    name + " are numbered " + join(actual) + ", expected " + join(expected)));
        }
    }

    private static <T> List<Field> sequences(List<T> items, Function<T, Field> sequence) {
        return items.stream().map(sequence).toList();
    }

    private static String join(List<Integer> numbers) {
        return String.join(", ", numbers.stream().map(String::valueOf).toList());
    }
}
