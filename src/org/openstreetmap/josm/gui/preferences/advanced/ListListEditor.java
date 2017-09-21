// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.spi.preferences.ListListSetting;

/**
 * Editor for List of Lists preference entries.
 * @since 4634
 */
public class ListListEditor extends AbstractTableListEditor<List<String>> {

    private final transient List<List<String>> data;

    /**
     * Constructs a new {@code ListListEditor}.
     * @param gui The parent component
     * @param entry preference entry
     * @param setting list of lists setting
     */
    public ListListEditor(final JComponent gui, PrefEntry entry, ListListSetting setting) {
        super(gui, tr("Change list of lists setting"), entry);
        List<List<String>> orig = setting.getValue();
        data = new ArrayList<>();
        if (orig != null) {
            for (List<String> l : orig) {
                data.add(new ArrayList<>(l));
            }
        }
    }

    @Override
    public List<List<String>> getData() {
        return data;
    }

    @Override
    protected final JPanel build() {
        table.setTableHeader(null);
        return super.build();
    }

    private class EntryListModel extends AbstractEntryListModel {

        @Override
        public String getElementAt(int index) {
            return (index+1) + ": " + data.get(index);
        }

        @Override
        public int getSize() {
            return data.size();
        }

        @Override
        public void add() {
            data.add(new ArrayList<String>());
            fireIntervalAdded(this, getSize() - 1, getSize() - 1);
        }

        @Override
        public void remove(int idx) {
            data.remove(idx);
            fireIntervalRemoved(this, idx, idx);
        }
    }

    private class ListTableModel extends AbstractTableModel {

        private List<String> data() {
            return entryIdx == null ? Collections.<String>emptyList() : data.get(entryIdx);
        }

        private int size() {
            return data().size();
        }

        @Override
        public int getRowCount() {
            return entryIdx == null ? 0 : size() + 1;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return size() == row ? "" : data().get(row);
        }

        @Override
        public void setValueAt(Object o, int row, int column) {
            String s = (String) o;
            if (row == size()) {
                data().add(s);
                fireTableRowsInserted(row+1, row+1);
            } else {
                data().set(row, s);
                fireTableCellUpdated(row, column);
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }
    }

    @Override
    protected AbstractEntryListModel newEntryListModel() {
        return new EntryListModel();
    }

    @Override
    protected AbstractTableModel newTableModel() {
        return new ListTableModel();
    }
}
