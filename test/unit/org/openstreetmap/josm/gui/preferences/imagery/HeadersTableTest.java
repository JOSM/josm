// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link HeadersTable}.
 */
class HeadersTableTest {

    private static TableModel getModel(HeadersTable headersTable) {
        JScrollPane scrollPane = (JScrollPane) headersTable.getComponent(0);
        return ((JTable) scrollPane.getViewport().getView()).getModel();
    }

    /**
     * Adds a row by typing into the empty last row of the table.
     */
    private static void addRow(TableModel model, String name, String value) {
        int row = model.getRowCount() - 1;
        model.setValueAt(name, row, 0);
        model.setValueAt(value, row, 1);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/24989">#24989</a>:
     * a header name entered twice must not throw "Duplicate key".
     */
    @Test
    void testTicket24989() {
        HeadersTable headersTable = new HeadersTable();
        TableModel model = getModel(headersTable);
        addRow(model, "referer", "https://nakarte.me/");
        addRow(model, "referer", "");

        assertEquals(Collections.singletonMap("referer", "https://nakarte.me/"), headersTable.getHeaders());
    }

    /**
     * If a header name is used in several complete rows, the last one wins and the table order is kept.
     */
    @Test
    void testDuplicateNameLastWins() {
        HeadersTable headersTable = new HeadersTable();
        TableModel model = getModel(headersTable);
        addRow(model, "User-Agent", "first");
        addRow(model, "Referer", "https://example.org/");
        addRow(model, "User-Agent", "second");

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("User-Agent", "second");
        expected.put("Referer", "https://example.org/");
        assertEquals(expected, headersTable.getHeaders());
        assertEquals("[User-Agent, Referer]", headersTable.getHeaders().keySet().toString());
    }

    /**
     * Rows without a name or without a value are not headers.
     */
    @Test
    void testIncompleteRowsIgnored() {
        HeadersTable headersTable = new HeadersTable();
        TableModel model = getModel(headersTable);
        addRow(model, "", "value without name");
        addRow(model, "Name-Without-Value", " ");
        addRow(model, "Accept", "image/png");

        assertEquals(Collections.singletonMap("Accept", "image/png"), headersTable.getHeaders());
    }
}
