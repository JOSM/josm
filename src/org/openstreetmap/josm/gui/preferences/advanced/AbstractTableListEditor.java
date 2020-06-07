// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.DefaultCellEditor;
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

import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Abstract superclass of {@link ListListEditor} and {@link MapListEditor}.
 * @param <T> type of elements
 * @since 9505
 */
public abstract class AbstractTableListEditor<T> extends AbstractListEditor<T> {

    protected final AbstractEntryListModel entryModel;
    protected final JList<String> entryList;

    protected final JTable table;
    protected final AbstractTableModel tableModel;

    protected Integer entryIdx;

    /**
     * Constructs a new {@code AbstractListEditor}.
     * @param parent       The parent element that will be used for position and maximum size
     * @param title        The text that will be shown in the window titlebar
     * @param entry        Preference entry
     */
    protected AbstractTableListEditor(Component parent, String title, PrefEntry entry) {
        super(parent, title, entry);
        entryModel = newEntryListModel();
        entryList = new JList<>(entryModel);
        entryList.getSelectionModel().addListSelectionListener(new EntryListener());
        tableModel = newTableModel();
        table = new JTable(tableModel);
        setContent(build(), false);
    }

    protected abstract static class AbstractEntryListModel extends AbstractListModel<String> {

        abstract void add();

        abstract void remove(int idx);
    }

    protected final class NewEntryAction extends AbstractAction {
        NewEntryAction() {
            putValue(NAME, tr("New"));
            putValue(SHORT_DESCRIPTION, tr("add entry"));
            new ImageProvider("dialogs", "add").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            entryModel.add();
        }
    }

    protected final class RemoveEntryAction extends AbstractAction implements ListSelectionListener {
        RemoveEntryAction() {
            putValue(NAME, tr("Remove"));
            putValue(SHORT_DESCRIPTION, tr("Remove the selected entry"));
            new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
            updateEnabledState();
        }

        private void updateEnabledState() {
            setEnabled(entryList.getSelectedIndices().length == 1);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            entryModel.remove(entryList.getSelectedIndices()[0]);
        }
    }

    private class EntryListener implements ListSelectionListener {
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
            tableModel.fireTableDataChanged();
        }
    }

    @Override
    protected JPanel build() {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Key: {0}", entry.getKey())), GBC.std(0, 0).span(2).weight(1, 0).insets(0, 0, 5, 10));

        JPanel left = new JPanel(new GridBagLayout());

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

        p.add(left, GBC.std(0, 1).fill().weight(0.3, 1.0));

        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        DefaultCellEditor editor = new DefaultCellEditor(new JosmTextField());
        editor.setClickCountToStart(1);
        table.setDefaultEditor(table.getColumnClass(0), editor);

        JScrollPane pane = new JScrollPane(table);
        pane.setPreferredSize(new Dimension(140, 0));
        p.add(pane, GBC.std(1, 1).insets(5, 0, 0, 0).fill().weight(0.7, 1.0));
        return p;
    }

    protected abstract AbstractEntryListModel newEntryListModel();

    protected abstract AbstractTableModel newTableModel();
}
