// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.Preferences.ListSetting;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.advanced.AdvancedPreference.PrefEntry;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.WindowGeometry;

public class ListEditor extends ExtendedDialog {

    List<String> data;
    PrefEntry entry;

    public ListEditor(final PreferenceTabbedPane gui, PrefEntry entry, ListSetting setting) {
        super(gui, tr("Change list setting"), new String[] {tr("OK"), tr("Cancel")});
        this.entry = entry;
        List<String> orig = setting.getValue();
        if (orig != null) {
            data = new ArrayList<String>(orig);
        } else {
            data = new ArrayList<String>();
        }
        setButtonIcons(new String[] {"ok.png", "cancel.png"});
        setRememberWindowGeometry(getClass().getName() + ".geometry", WindowGeometry.centerInWindow(gui, new Dimension(300, 350)));
        setContent(build(), false);
    }

    public List<String> getData() {
        return data;
    }

    protected JPanel build() {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Key: {0}", entry.getKey())), GBC.eol().insets(0,0,5,0));
        ListSettingTableModel listModel = new ListSettingTableModel();
        JTable table = new JTable(listModel);
        table.putClientProperty("terminateEditOnFocusLost", true);
        table.setTableHeader(null);

        DefaultCellEditor editor = new DefaultCellEditor(new JTextField());
        editor.setClickCountToStart(1);
        table.setDefaultEditor(table.getColumnClass(0), editor);

        JScrollPane pane = new JScrollPane(table);
        p.add(pane, GBC.eol().insets(5,10,0,0).fill());
        return p;
    }

    class ListSettingTableModel extends AbstractTableModel {

        public List<String> getData() {
            return data;
        }

        @Override
        public int getRowCount() {
            return data.size() + 1;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return data.size() == row ? "" : data.get(row);
        }

        @Override
        public void setValueAt(Object o, int row, int column) {
            String s = (String)o;
            if(row == data.size()) {
                data.add(s);
                fireTableRowsInserted(row+1, row+1);
            } else {
                data.set(row, s);
            }
            fireTableCellUpdated(row, column);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }
    }

}
