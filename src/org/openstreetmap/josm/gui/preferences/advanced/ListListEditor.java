// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.data.Preferences.ListListSetting;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * Editor for List of Lists preference entries.
 */
public class ListListEditor extends ExtendedDialog {

    EntryListModel entryModel;
    List<List<String>> data;
    PrefEntry entry;

    JList entryList;
    Integer entryIdx;
    JTable table;

    ListTableModel tableModel;

    /**
     * Constructs a new {@code ListListEditor}.
     * @param gui The parent component
     */
    public ListListEditor(final JComponent gui, PrefEntry entry, ListListSetting setting) {
        super(gui, tr("Change list of lists setting"), new String[] {tr("OK"), tr("Cancel")});
        this.entry = entry;
        List<List<String>> orig = setting.getValue();
        data = new ArrayList<List<String>>();
        if (orig != null) {
            for (List<String> l : orig) {
                data.add(new ArrayList<String>(l));
            }
        }
        setButtonIcons(new String[] {"ok.png", "cancel.png"});
        setRememberWindowGeometry(getClass().getName() + ".geometry", WindowGeometry.centerInWindow(gui, new Dimension(500, 350)));
        setContent(build(), false);
    }

    /**
     * Returns the data.
     * @return the preference data
     */
    public List<List<String>> getData() {
        return data;
    }

    protected JPanel build() {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Key: {0}", entry.getKey())), GBC.std(0,0).span(2).weight(1, 0).insets(0,0,5,10));

        JPanel left = new JPanel(new GridBagLayout());

        entryModel = new EntryListModel();
        entryList = new JList(entryModel);
        entryList.getSelectionModel().addListSelectionListener(new EntryListener());
        JScrollPane scroll = new JScrollPane(entryList);
        left.add(scroll, GBC.eol().fill());

        JToolBar sideButtonTB = new JToolBar(JToolBar.HORIZONTAL);
        sideButtonTB.setBorderPainted(false);
        sideButtonTB.setOpaque(false);
        sideButtonTB.add(new NewEntryAction());
        RemoveEntryAction removeEntryAction = new RemoveEntryAction();
        entryList.getSelectionModel().addListSelectionListener(removeEntryAction);
        sideButtonTB.add(removeEntryAction);
        left.add(sideButtonTB, GBC.eol());

        left.setPreferredSize(new Dimension(80, 0));

        p.add(left, GBC.std(0,1).fill().weight(0.3, 1.0));

        tableModel = new ListTableModel();
        table = new JTable(tableModel);
        table.putClientProperty("terminateEditOnFocusLost", true);
        table.setTableHeader(null);

        DefaultCellEditor editor = new DefaultCellEditor(new JosmTextField());
        editor.setClickCountToStart(1);
        table.setDefaultEditor(table.getColumnClass(0), editor);

        JScrollPane pane = new JScrollPane(table);
        pane.setPreferredSize(new Dimension(140, 0));
        p.add(pane, GBC.std(1,1).insets(5,0,0,0).fill().weight(0.7, 1.0));
        return p;
    }

    class EntryListModel extends AbstractListModel {
        @Override
        public Object getElementAt(int index) {
            return (index+1) + ": " + data.get(index).toString();
        }

        @Override
        public int getSize() {
            return data.size();
        }

        public void add(List<String> l) {
            data.add(l);
            fireIntervalAdded(this, data.size() - 1, data.size() - 1);
        }

        public void remove(int idx) {
            data.remove(idx);
            fireIntervalRemoved(this, idx, idx);
        }
    }

    class NewEntryAction extends AbstractAction {
        public NewEntryAction() {
            putValue(NAME, tr("New"));
            putValue(SHORT_DESCRIPTION, tr("add entry"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            entryModel.add(new ArrayList<String>());
        }
    }

    class RemoveEntryAction extends AbstractAction implements ListSelectionListener {
        public RemoveEntryAction() {
            putValue(NAME, tr("Remove"));
            putValue(SHORT_DESCRIPTION, tr("Remove the selected entry"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            updateEnabledState();
        }

        protected void updateEnabledState() {
            setEnabled(entryList.getSelectedIndices().length == 1);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int idx = entryList.getSelectedIndices()[0];
            entryModel.remove(idx);
        }
    }

    class EntryListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                ((DefaultCellEditor) editor).stopCellEditing();
            }
            if (entryList.getSelectedIndices().length != 1) {
                entryIdx = null;
                table.setEnabled(false);
            } else {
                entryIdx = entryList.getSelectedIndices()[0];
                table.setEnabled(true);
            }
            tableModel.fireTableStructureChanged();
        }
    }

    class ListTableModel extends AbstractTableModel {

        private List<String> data() {
            if (entryIdx == null) return Collections.emptyList();
            return data.get(entryIdx);
        }

        @Override
        public int getRowCount() {
            return entryIdx == null ? 0 : data().size() + 1;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return data().size() == row ? "" : data().get(row);
        }

        @Override
        public void setValueAt(Object o, int row, int column) {
            String s = (String)o;
            if (row == data().size()) {
                data().add(s);
                fireTableRowsInserted(row+1, row+1);
            } else {
                data().set(row, s);
            }
            fireTableCellUpdated(row, column);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }
    }
}
