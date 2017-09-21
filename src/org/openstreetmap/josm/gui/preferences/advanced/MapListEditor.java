// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.spi.preferences.MapListSetting;

/**
 * Editor for List of Maps preference entries.
 * @since 4634
 */
public class MapListEditor extends AbstractTableListEditor<Map<String, String>> {

    private final transient List<List<String>> dataKeys;
    private final transient List<List<String>> dataValues;

    /**
     * Constructs a new {@code MapListEditor}.
     * @param gui The parent component
     * @param entry preference entry
     * @param setting list of maps setting
     */
    public MapListEditor(JComponent gui, PrefEntry entry, MapListSetting setting) {
        super(gui, tr("Change list of maps setting"), entry);
        List<Map<String, String>> orig = setting.getValue();

        dataKeys = new ArrayList<>();
        dataValues = new ArrayList<>();
        if (orig != null) {
            for (Map<String, String> m : orig) {
                List<String> keys = new ArrayList<>();
                List<String> values = new ArrayList<>();
                for (Entry<String, String> e : m.entrySet()) {
                    keys.add(e.getKey());
                    values.add(e.getValue());
                }
                dataKeys.add(keys);
                dataValues.add(values);
            }
        }
    }

    @Override
    public List<Map<String, String>> getData() {
        List<Map<String, String>> data = new ArrayList<>();
        for (int i = 0; i < dataKeys.size(); ++i) {
            Map<String, String> m = new LinkedHashMap<>();
            for (int j = 0; j < dataKeys.get(i).size(); ++j) {
                m.put(dataKeys.get(i).get(j), dataValues.get(i).get(j));
            }
            data.add(m);
        }
        return data;
    }

    @Override
    protected final JPanel build() {
        table.getTableHeader().getColumnModel().getColumn(0).setHeaderValue(tr("Key"));
        table.getTableHeader().getColumnModel().getColumn(1).setHeaderValue(tr("Value"));
        return super.build();
    }

    private class EntryListModel extends AbstractEntryListModel {

        @Override
        public String getElementAt(int index) {
            return tr("Entry {0}", index+1);
        }

        @Override
        public int getSize() {
            return dataKeys.size();
        }

        @Override
        public void add() {
            dataKeys.add(new ArrayList<String>());
            dataValues.add(new ArrayList<String>());
            fireIntervalAdded(this, getSize() - 1, getSize() - 1);
        }

        @Override
        public void remove(int idx) {
            dataKeys.remove(idx);
            dataValues.remove(idx);
            fireIntervalRemoved(this, idx, idx);
        }
    }

    private class MapTableModel extends AbstractTableModel {

        private List<List<String>> data() {
            return entryIdx == null ? Collections.<List<String>>emptyList() : Arrays.asList(dataKeys.get(entryIdx), dataValues.get(entryIdx));
        }

        private int size() {
            return entryIdx == null ? 0 : dataKeys.get(entryIdx).size();
        }

        @Override
        public int getRowCount() {
            return entryIdx == null ? 0 : size() + 1;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == 0 ? tr("Key") : tr("Value");
        }

        @Override
        public Object getValueAt(int row, int column) {
            return size() == row ? "" : data().get(column).get(row);
        }

        @Override
        public void setValueAt(Object o, int row, int column) {
            String s = (String) o;
            if (row == size()) {
                data().get(0).add("");
                data().get(1).add("");
                data().get(column).set(row, s);
                fireTableRowsInserted(row+1, row+1);
            } else {
                data().get(column).set(row, s);
                fireTableCellUpdated(row, column);
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }
    }

    @Override
    protected final AbstractEntryListModel newEntryListModel() {
        return new EntryListModel();
    }

    @Override
    protected final AbstractTableModel newTableModel() {
        return new MapTableModel();
    }
}
