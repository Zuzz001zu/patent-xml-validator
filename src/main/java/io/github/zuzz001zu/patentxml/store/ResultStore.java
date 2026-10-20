package io.github.zuzz001zu.patentxml.store;

import io.github.zuzz001zu.patentxml.ValidationResult;
import io.github.zuzz001zu.patentxml.rules.Finding;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores validation results through plain JDBC. The default is an H2 database file,
 * but any JDBC URL works as long as the driver is on the classpath.
 */
public final class ResultStore implements AutoCloseable {

    private final Connection connection;

    private ResultStore(Connection connection) {
        this.connection = connection;
    }

    public static ResultStore open(String jdbcUrl) throws SQLException {
        ResultStore store = new ResultStore(DriverManager.getConnection(jdbcUrl));
        store.createSchema();
        return store;
    }

    /** H2 database stored next to the given path (H2 adds the .mv.db extension). */
    public static String h2File(Path path) {
        return "jdbc:h2:file:" + path.toAbsolutePath();
    }

    /** Saves a run with all its documents and findings in one transaction. */
    public long save(String source, String toolVersion, List<ValidationResult> results) throws SQLException {
        connection.setAutoCommit(false);
        try {
            long runId = insertRun(source, toolVersion);
            for (ValidationResult result : results) {
                long documentId = insertDocument(runId, result);
                insertFindings(documentId, result.findings());
            }
            connection.commit();
            return runId;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /** Runs a query from db/queries/ whose only parameter is the run id. */
    public Table report(String query, long runId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(Sql.load("queries/" + query + ".sql"))) {
            if (statement.getParameterMetaData().getParameterCount() > 0) {
                statement.setLong(1, runId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return toTable(rs);
            }
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    private void createSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : Sql.statements(Sql.load("schema.sql"))) {
                statement.execute(sql);
            }
        }
    }

    private long insertRun(String source, String toolVersion) throws SQLException {
        String sql = "INSERT INTO validation_run (started_at, source, tool_version) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, source);
            ps.setString(3, toolVersion);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    private long insertDocument(long runId, ValidationResult result) throws SQLException {
        String sql = """
                INSERT INTO checked_document
                    (run_id, file_name, publication_number, application_number, receiving_office, status)
                VALUES (?, ?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, runId);
            ps.setString(2, result.fileName());
            setNullable(ps, 3, result.publicationNumber().orElse(null));
            setNullable(ps, 4, result.applicationNumber().orElse(null));
            setNullable(ps, 5, result.receivingOffice().orElse(null));
            ps.setString(6, result.status().name());
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    private void insertFindings(long documentId, List<Finding> findings) throws SQLException {
        if (findings.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO finding (document_id, rule_id, severity, line_number, message)
                VALUES (?, ?, ?, ?, ?)""";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Finding f : findings) {
                ps.setLong(1, documentId);
                ps.setString(2, f.ruleId());
                ps.setString(3, f.severity().name());
                if (f.line() > 0) {
                    ps.setInt(4, f.line());
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.setString(5, truncate(f.message(), 1000));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void setNullable(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private static long generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("no generated key returned");
            }
            return keys.getLong(1);
        }
    }

    private static Table toTable(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            columns.add(meta.getColumnLabel(i).toLowerCase());
        }
        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columns.size(); i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }
        return new Table(columns, rows);
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    /** Loads SQL files from the classpath. */
    static final class Sql {

        private Sql() {
        }

        static String load(String name) {
            String path = "/db/" + name;
            try (InputStream in = ResultStore.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalArgumentException("SQL file not found: " + path);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /** Splits a script on ';' and drops comment lines. Enough for our own schema file. */
        static List<String> statements(String script) {
            StringBuilder withoutComments = new StringBuilder();
            for (String line : script.split("\\R")) {
                if (!line.strip().startsWith("--")) {
                    withoutComments.append(line).append('\n');
                }
            }
            List<String> result = new ArrayList<>();
            for (String part : withoutComments.toString().split(";")) {
                if (!part.isBlank()) {
                    result.add(part.strip());
                }
            }
            return result;
        }
    }
}
