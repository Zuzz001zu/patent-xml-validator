package io.github.zuzz001zu.patentxml.store;

import io.github.zuzz001zu.patentxml.TestXml;
import io.github.zuzz001zu.patentxml.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultStoreTest {

    private static final ValidationResult VALID = TestXml.validate(TestXml.VALID);
    private static final ValidationResult BAD_KIND =
            TestXml.validate(TestXml.with("<kind>A1</kind>", "<kind>B1</kind>"));
    private static final ValidationResult BAD_IPC_FROM_EP = TestXml.validate(
            TestXml.replace(TestXml.with("<text>A61K 31/496</text>", "<text>A61K31/496</text>"),
                    "PCT/CH2025/050211", "PCT/EP2025/050211"));
    private static final ValidationResult NOT_XML = TestXml.validate("<patent-document>");

    private static ResultStore inMemory() throws SQLException {
        return ResultStore.open("jdbc:h2:mem:" + UUID.randomUUID());
    }

    @Test
    void countsFindingsByRule() throws SQLException {
        try (ResultStore store = inMemory()) {
            long run = store.save("test", "1.0", List.of(VALID, BAD_KIND, BAD_KIND, BAD_IPC_FROM_EP));

            Table byRule = store.report("findings_by_rule", run);

            assertEquals(2, byRule.rows().size());
            assertEquals("PUB-03", byRule.value(0, "rule_id"));
            assertEquals(2L, ((Number) byRule.value(0, "occurrences")).longValue());
            assertEquals("CLS-01", byRule.value(1, "rule_id"));
        }
    }

    @Test
    void summarisesByReceivingOffice() throws SQLException {
        try (ResultStore store = inMemory()) {
            long run = store.save("test", "1.0", List.of(VALID, BAD_KIND, BAD_IPC_FROM_EP, NOT_XML));

            Table offices = store.report("by_receiving_office", run);

            assertEquals(List.of("CH", "--", "EP"),
                    List.of(offices.value(0, "receiving_office"), offices.value(1, "receiving_office"),
                            offices.value(2, "receiving_office")));
            assertEquals(2L, ((Number) offices.value(0, "documents")).longValue());
            assertEquals(1L, ((Number) offices.value(0, "invalid")).longValue());
            assertEquals(50.0, ((Number) offices.value(0, "valid_pct")).doubleValue(), 0.001);
        }
    }

    @Test
    void reportsOnlyTheRequestedRun() throws SQLException {
        try (ResultStore store = inMemory()) {
            store.save("first", "1.0", List.of(BAD_KIND));
            long second = store.save("second", "1.0", List.of(VALID));

            assertTrue(store.report("findings_by_rule", second).isEmpty());
            Table history = store.report("run_history", second);
            assertEquals(2, history.rows().size());
            assertEquals(second, ((Number) history.value(0, "id")).longValue());
            assertEquals(1L, ((Number) history.value(1, "errors")).longValue());
        }
    }

    @Test
    void reopeningAFileDatabaseKeepsEarlierRuns(@TempDir Path dir) throws SQLException {
        String url = ResultStore.h2File(dir.resolve("results"));
        try (ResultStore store = ResultStore.open(url)) {
            store.save("first", "1.0", List.of(VALID));
        }
        try (ResultStore store = ResultStore.open(url)) {
            long run = store.save("second", "1.0", List.of(VALID));
            assertEquals(2, store.report("run_history", run).rows().size());
        }
    }

    @Test
    void splitsTheSchemaScript() {
        List<String> statements = ResultStore.Sql.statements("""
                -- comment
                CREATE TABLE a (id INT);

                CREATE TABLE b (id INT);
                """);
        assertEquals(List.of("CREATE TABLE a (id INT)", "CREATE TABLE b (id INT)"), statements);
    }
}
