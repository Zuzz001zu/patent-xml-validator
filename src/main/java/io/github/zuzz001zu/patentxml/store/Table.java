package io.github.zuzz001zu.patentxml.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Rows returned by a report query, ready to be printed or rendered. */
public record Table(List<String> columns, List<List<Object>> rows) {

    public Table {
        columns = List.copyOf(columns);
        rows = rows.stream().map(r -> Collections.unmodifiableList(new ArrayList<>(r))).toList();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /** Value of a column in a row, by column name (case-insensitive). */
    public Object value(int row, String column) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).equalsIgnoreCase(column)) {
                return rows.get(row).get(i);
            }
        }
        throw new IllegalArgumentException("no column " + column);
    }
}
