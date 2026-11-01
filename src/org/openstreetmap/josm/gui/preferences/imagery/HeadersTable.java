// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * Simple table for editing HTTP headers
 * @author Wiktor Niesiobedzki
 *
 */
public class HeadersTable extends JPanel {

    private final class HeaderTableModel extends AbstractTableModel {
        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return tr("Header name");
            case 1:
                return tr("Header value");
            default:
                return "";
            }
        }

        @Override
        public int getRowCount() {
            return headers.size() + 1;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row < headers.size()) {
                return headers.get(row)[col];
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (row < headers.size()) {
                String[] headerRow = headers.get(row);
                headerRow[col] = (String) value;
                if ("".equals(headerRow[0]) && "".equals(headerRow[1])) {
                    headers.remove(row);
                    fireTableRowsDeleted(row, row);
                }

            } else if (row == headers.size()) {
                String[] entry = {"", ""};
                entry[col] = (String) value;
                headers.add(entry);
                fireTableRowsInserted(row + 1, row + 1);
            }
            fireTableCellUpdated(row, col);
        }
    }

    private final List<String[]> headers;

    /**
     * Creates empty table
     */
    public HeadersTable() {
        this(new ConcurrentHashMap<>());
    }

    /**
     * Create table prefilled with headers
     * @param headers contents of table
     */
    public HeadersTable(Map<String, String> headers) {
        super(new GridBagLayout());
        this.headers = getHeadersAsVector(headers);
        JTable table = new JTable(new HeaderTableModel());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        add(new JScrollPane(table), GBC.eol().fill());
    }

    private static List<String[]> getHeadersAsVector(Map<String, String> headers) {
        return headers.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> new String[] {e.getKey(), e.getValue()}).collect(Collectors.toList());
    }

    /**
     * Returns headers provided by user.
     * Rows without a name or without a value are ignored. If several rows use the same name, the last one wins.
     * @return headers provided by user, in table order
     */
    public Map<String, String> getHeaders() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String[] header : headers) {
            if (!Utils.isStripEmpty(header[0]) && !Utils.isStripEmpty(header[1])) {
                result.put(header[0], header[1]);
            }
        }
        return result;
    }

}
