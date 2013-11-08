// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryDataSetListener;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.gui.history.HistoryLoadTask;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * HistoryDialog displays a list of the currently selected primitives and provides
 * two actions for (1) (re)loading the history of the selected primitives and (2)
 * for launching a history browser for each selected primitive.
 *
 */
public class HistoryDialog extends ToggleDialog implements HistoryDataSetListener {

    /** the table model */
    protected HistoryItemTableModel model;
    /** the table with the history items */
    protected JTable historyTable;

    protected ShowHistoryAction showHistoryAction;
    protected ReloadAction reloadAction;

    /**
     * Constructs a new {@code HistoryDialog}.
     */
    public HistoryDialog() {
        super(tr("History"), "history", tr("Display the history of all selected items."),
                Shortcut.registerShortcut("subwindow:history", tr("Toggle: {0}", tr("History")), KeyEvent.VK_H,
                        Shortcut.ALT_SHIFT), 150);
        build();
        HelpUtil.setHelpContext(this, HelpUtil.ht("/Dialog/History"));
    }

    /**
     * builds the GUI
     */
    protected void build() {
        DefaultListSelectionModel selectionModel = new DefaultListSelectionModel();
        historyTable = new JTable(
                model = new HistoryItemTableModel(selectionModel),
                new HistoryTableColumnModel(),
                selectionModel
        );
        historyTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final TableCellRenderer oldRenderer = historyTable.getTableHeader().getDefaultRenderer();
        historyTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComponent c = (JComponent)oldRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!"".equals(value))
                    return c;
                JLabel l = new JLabel(ImageProvider.get("misc","showhide"));
                l.setForeground(c.getForeground());
                l.setBackground(c.getBackground());
                l.setFont(c.getFont());
                l.setBorder(c.getBorder());
                l.setOpaque(true);
                return l;
            }
        });
        historyTable.addMouseListener(new ShowHistoryMouseAdapter());
        historyTable.setTableHeader(null);

        createLayout(historyTable, true, Arrays.asList(new SideButton[] {
            new SideButton(reloadAction = new ReloadAction()),
            new SideButton(showHistoryAction = new ShowHistoryAction())
        }));

        // wire actions
        //
        historyTable.getSelectionModel().addListSelectionListener(showHistoryAction);
        historyTable.getSelectionModel().addListSelectionListener(reloadAction);

        // Show history dialog on Enter/Spacebar
        InputMapUtils.addEnterAction(historyTable, showHistoryAction);
        InputMapUtils.addSpacebarAction(historyTable, showHistoryAction);
    }

    @Override
    public void showNotify() {
        HistoryDataSet.getInstance().addHistoryDataSetListener(this);
        DataSet.addSelectionListener(model);
        if (Main.main.getCurrentDataSet() == null) {
            model.selectionChanged(null);
        } else {
            model.selectionChanged(Main.main.getCurrentDataSet().getAllSelected());
        }
    }

    @Override
    public void hideNotify() {
        HistoryDataSet.getInstance().removeHistoryDataSetListener(this);
        DataSet.removeSelectionListener(model);
    }

    /* ----------------------------------------------------------------------------- */
    /* interface HistoryDataSetListener                                              */
    /* ----------------------------------------------------------------------------- */
    @Override
    public void historyUpdated(HistoryDataSet source, PrimitiveId primitiveId) {
        model.refresh();
    }

    @Override
    public void historyDataSetCleared(HistoryDataSet source) {
        model.refresh();
    }

    /**
     * The table model with the history items
     *
     */
    static class HistoryItemTableModel extends DefaultTableModel implements SelectionChangedListener{
        private List<OsmPrimitive> data;
        private DefaultListSelectionModel selectionModel;

        public HistoryItemTableModel(DefaultListSelectionModel selectionModel) {
            data = new ArrayList<OsmPrimitive>();
            this.selectionModel = selectionModel;
        }

        @Override
        public int getRowCount() {
            if (data == null)
                return 0;
            return data.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return data.get(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        protected List<OsmPrimitive> getSelectedPrimitives() {
            List<OsmPrimitive> ret = new ArrayList<OsmPrimitive>();
            for (int i=0; i< data.size(); i++) {
                if (selectionModel.isSelectedIndex(i)) {
                    ret.add(data.get(i));
                }
            }
            return ret;
        }

        protected void selectPrimitives(Collection<OsmPrimitive> primitives) {
            for (OsmPrimitive p: primitives) {
                int idx = data.indexOf(p);
                if (idx < 0) {
                    continue;
                }
                selectionModel.addSelectionInterval(idx, idx);
            }
        }

        public void refresh() {
            List<OsmPrimitive> selectedPrimitives = getSelectedPrimitives();
            data.clear();
            if (Main.main.getCurrentDataSet() == null)
                return;
            for (OsmPrimitive primitive: Main.main.getCurrentDataSet().getAllSelected()) {
                if (primitive.isNew()) {
                    continue;
                }
                data.add(primitive);
            }
            fireTableDataChanged();
            selectPrimitives(selectedPrimitives);
        }

        @Override
        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            data.clear();
            selectionModel.clearSelection();
            if (newSelection != null && !newSelection.isEmpty()) {
                for (OsmPrimitive primitive: newSelection) {
                    if (primitive.isNew()) {
                        continue;
                    }
                    data.add(primitive);
                }
            }
            fireTableDataChanged();
            selectionModel.addSelectionInterval(0, data.size()-1);
        }

        public List<OsmPrimitive> getPrimitives(int [] rows) {
            if (rows == null || rows.length == 0) return Collections.emptyList();
            List<OsmPrimitive> ret = new ArrayList<OsmPrimitive>(rows.length);
            for (int row: rows) {
                ret.add(data.get(row));
            }
            return ret;
        }

        public OsmPrimitive getPrimitive(int row) {
            return data.get(row);
        }
    }

    /**
     * The column model
     */
    static class HistoryTableColumnModel extends DefaultTableColumnModel {
        protected void createColumns() {
            TableColumn col = null;
            OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();
            // column 0 - History item
            col = new TableColumn(0);
            col.setHeaderValue(tr("Object with history"));
            col.setCellRenderer(renderer);
            addColumn(col);
        }

        public HistoryTableColumnModel() {
            createColumns();
        }
    }

    /**
     * The action for reloading history information of the currently selected primitives.
     *
     */
    class ReloadAction extends AbstractAction implements ListSelectionListener {

        public ReloadAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs","refresh"));
            putValue(Action.NAME, tr("Reload"));
            putValue(Action.SHORT_DESCRIPTION, tr("Reload all currently selected objects and refresh the list."));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int [] rows = historyTable.getSelectedRows();
            if (rows == null || rows.length == 0) return;

            List<OsmPrimitive> selectedItems = model.getPrimitives(rows);
            HistoryLoadTask task = new HistoryLoadTask();
            task.add(selectedItems);
            Main.worker.execute(task);
        }

        protected void updateEnabledState() {
            setEnabled(historyTable.getSelectedRowCount() > 0);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class ShowHistoryMouseAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                int row = historyTable.rowAtPoint(e.getPoint());
                HistoryBrowserDialogManager.getInstance().showHistory(Collections.singletonList(model.getPrimitive(row)));
            }
        }
    }

    /**
     * The action for showing history information of the current history item.
     */
    class ShowHistoryAction extends AbstractAction implements ListSelectionListener {
        public ShowHistoryAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs","history"));
            putValue(Action.NAME, tr("Show"));
            putValue(Action.SHORT_DESCRIPTION, tr("Display the history of the selected objects."));
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int [] rows = historyTable.getSelectedRows();
            if (rows == null || rows.length == 0) return;
            HistoryBrowserDialogManager.getInstance().showHistory(model.getPrimitives(rows));
        }

        protected void updateEnabledState() {
            setEnabled(historyTable.getSelectedRowCount() > 0);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }
}
