// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryDataSetListener;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialog;
import org.openstreetmap.josm.gui.history.HistoryBrowserDialogManager;
import org.openstreetmap.josm.gui.history.HistoryLoadTask;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmServerHistoryReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * History dialog works like follows:
 *
 * There is a history cache hold in the back for primitives of the last refresh.
 * When the user refreshes, this cache is cleared and all currently selected items
 * are reloaded.
 * If the user has selected at least one primitive not in the cache, the list
 * is not displayed. Elsewhere, the list of all changes of all currently selected
 * objects are displayed.
 *
 * @author imi
 */
public class HistoryDialog extends ToggleDialog implements HistoryDataSetListener {

    /** the table model */
    protected HistoryItemDataModel model;
    /** the table with the history items */
    protected JTable historyTable;

    protected ShowHistoryAction showHistoryAction;
    protected ReloadAction reloadAction;

    /**
     * builds the row with the command buttons
     *
     * @return the rows with the command buttons
     */
    protected JPanel buildButtonRow() {
        JPanel buttons = new JPanel(new GridLayout(1,2));

        SideButton btn = new SideButton(reloadAction = new ReloadAction());
        btn.setName("btn.reload");
        buttons.add(btn);

        btn = new SideButton(showHistoryAction = new ShowHistoryAction());
        btn.setName("btn.showhistory");
        buttons.add(btn);

        return buttons;
    }

    /**
     * builds the GUI
     */
    protected void build() {
        model = new HistoryItemDataModel();
        //setLayout(new BorderLayout());
        historyTable = new JTable(
                model,
                new HistoryTableColumnModel()
        );
        historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyTable.setName("table.historyitems");
        final TableCellRenderer oldRenderer = historyTable.getTableHeader().getDefaultRenderer();
        historyTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComponent c = (JComponent)oldRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!value.equals(""))
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
        historyTable.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            int row = historyTable.rowAtPoint(e.getPoint());
                            History h = model.get(row);
                            showHistory(h);
                        }
                    }
                }
        );

        JScrollPane pane = new JScrollPane(historyTable);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        historyTable.setTableHeader(null);
        pane.setColumnHeaderView(null);
        add(pane, BorderLayout.CENTER);

        add(buildButtonRow(), BorderLayout.SOUTH);

        // wire actions
        //
        historyTable.getSelectionModel().addListSelectionListener(showHistoryAction);
        DataSet.selListeners.add(reloadAction);
    }

    public HistoryDialog() {
        super(tr("History"), "history", tr("Display the history of all selected items."),
                Shortcut.registerShortcut("subwindow:history", tr("Toggle: {0}", tr("History")), KeyEvent.VK_H,
                        Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 150);
        build();
        DataSet.selListeners.add(model);
        HistoryDataSet.getInstance().addHistoryDataSetListener(this);
    }



    public void historyUpdated(HistoryDataSet source, long primitiveId) {
        model.refresh();
    }

    /**
     * shows the {@see HistoryBrowserDialog} for a given {@see History}
     *
     * @param h the history. Must not be null.
     * @exception IllegalArgumentException thrown, if h is null
     */
    protected void showHistory(History h) throws IllegalArgumentException {
        if (h == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "h"));
        if (HistoryBrowserDialogManager.getInstance().existsDialog(h.getId())) {
            HistoryBrowserDialogManager.getInstance().show(h.getId());
        } else {
            HistoryBrowserDialog dialog = new HistoryBrowserDialog(h);
            HistoryBrowserDialogManager.getInstance().show(h.getId(), dialog);
        }
    }


    /**
     * The table model with the history items
     *
     */
    class HistoryItemDataModel extends DefaultTableModel implements SelectionChangedListener{
        private ArrayList<History> data;

        public HistoryItemDataModel() {
            data = new ArrayList<History>();
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

        public void refresh() {
            data.clear();
            if (Main.main.getCurrentDataSet() == null)
                return;
            for (OsmPrimitive primitive: Main.main.getCurrentDataSet().getSelected()) {
                if (primitive.getId() == 0) {
                    continue;
                }
                History h = HistoryDataSet.getInstance().getHistory(primitive.getId());
                if (h !=null) {
                    data.add(h);
                }
            }
            fireTableDataChanged();
        }

        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            refresh();
        }

        public History get(int idx) throws IndexOutOfBoundsException {
            if (idx < 0 || idx >= data.size())
                throw new IndexOutOfBoundsException(tr("Index out of bounds. Got {0}.", idx));
            return data.get(idx);
        }
    }


    /**
     * The table cell renderer for the history items.
     *
     */
    class HistoryTableCellRenderer extends JLabel implements TableCellRenderer {

        public final Color BGCOLOR_SELECTED = new Color(143,170,255);

        private HashMap<OsmPrimitiveType, ImageIcon> icons;

        public HistoryTableCellRenderer() {
            setOpaque(true);
            icons = new HashMap<OsmPrimitiveType, ImageIcon>();
            icons.put(OsmPrimitiveType.NODE, ImageProvider.get("data", "node"));
            icons.put(OsmPrimitiveType.WAY, ImageProvider.get("data", "way"));
            icons.put(OsmPrimitiveType.RELATION, ImageProvider.get("data", "relation"));
        }

        protected void renderIcon(History history) {
            setIcon(icons.get(history.getEarliest().getType()));
        }

        protected void renderText(History h) {
            String msg = "";
            switch(h.getEarliest().getType()) {
                case NODE:  msg = marktr("Node {0}"); break;
                case WAY: msg = marktr("Way {0}"); break;
                case RELATION: msg = marktr("Relation {0}"); break;
            }
            setText(tr(msg,h.getId()));
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            History h = (History)value;
            renderIcon(h);
            renderText(h);
            if (isSelected) {
                setBackground(BGCOLOR_SELECTED);
            } else {
                setBackground(Color.WHITE);
            }
            return this;
        }
    }

    /**
     * The column model
     */
    class HistoryTableColumnModel extends DefaultTableColumnModel {
        protected void createColumns() {
            TableColumn col = null;
            HistoryTableCellRenderer renderer = new HistoryTableCellRenderer();
            // column 0 - History item
            col = new TableColumn(0);
            col.setHeaderValue(tr("History item"));
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
    class ReloadAction extends AbstractAction implements SelectionChangedListener {
        public ReloadAction() {
            putValue(Action.SMALL_ICON, ImageProvider.get("dialogs","refresh"));
            putValue(Action.SHORT_DESCRIPTION, tr("Reload all currently selected objects and refresh the list."));
        }

        public void actionPerformed(ActionEvent e) {
            HistoryLoadTask task = new HistoryLoadTask();
            task.add(Main.main.getCurrentDataSet().getSelected());
            Main.worker.execute(task);
        }

        public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
            if (Main.main.getCurrentDataSet() == null) {
                setEnabled(false);
            } else {
                setEnabled(Main.main.getCurrentDataSet().getSelected().size() > 0);
            }
        }
    }

    /**
     * The action for showing history information of the current history item.
     */
    class ShowHistoryAction extends AbstractAction implements ListSelectionListener {
        public ShowHistoryAction() {
            //putValue(Action.SMALL_ICON, ImageProvider.get("dialogs","refresh"));
            putValue(Action.NAME, tr("Show"));
            putValue(Action.SHORT_DESCRIPTION, tr("Display the history of the selected primitive."));
        }

        public void actionPerformed(ActionEvent e) {
            int row = historyTable.getSelectionModel().getMinSelectionIndex();
            if (row < 0) return;
            History h = model.get(row);
            showHistory(h);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(historyTable.getSelectionModel().getMinSelectionIndex() >= 0);
        }
    }
}
