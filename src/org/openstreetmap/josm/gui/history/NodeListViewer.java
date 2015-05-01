// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.AdjustmentSynchronizer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * NodeListViewer is a UI component which displays the node list of two
 * version of a {@link OsmPrimitive} in a {@link History}.
 *
 * <ul>
 *   <li>on the left, it displays the node list for the version at {@link PointInTimeType#REFERENCE_POINT_IN_TIME}</li>
 *   <li>on the right, it displays the node list for the version at {@link PointInTimeType#CURRENT_POINT_IN_TIME}</li>
 * </ul>
 *
 */
public class NodeListViewer extends JPanel {

    private transient HistoryBrowserModel model;
    private VersionInfoPanel referenceInfoPanel;
    private VersionInfoPanel currentInfoPanel;
    private transient AdjustmentSynchronizer adjustmentSynchronizer;
    private transient SelectionSynchronizer selectionSynchronizer;
    private NodeListPopupMenu popupMenu;

    protected JScrollPane embeddInScrollPane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        adjustmentSynchronizer.participateInSynchronizedScrolling(pane.getVerticalScrollBar());
        return pane;
    }

    protected JTable buildReferenceNodeListTable() {
        final DiffTableModel tableModel = model.getNodeListTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);
        final NodeListTableColumnModel columnModel = new NodeListTableColumnModel();
        final JTable table = new JTable(tableModel, columnModel);
        tableModel.addTableModelListener(newReversedChangeListener(table, columnModel));
        table.setName("table.referencenodelisttable");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.addMouseListener(new InternalPopupMenuLauncher());
        table.addMouseListener(new DoubleClickAdapter(table));
        return table;
    }

    protected JTable buildCurrentNodeListTable() {
        final DiffTableModel tableModel = model.getNodeListTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        final NodeListTableColumnModel columnModel = new NodeListTableColumnModel();
        final JTable table = new JTable(tableModel, columnModel);
        tableModel.addTableModelListener(newReversedChangeListener(table, columnModel));
        table.setName("table.currentnodelisttable");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.addMouseListener(new InternalPopupMenuLauncher());
        table.addMouseListener(new DoubleClickAdapter(table));
        return table;
    }

    protected TableModelListener newReversedChangeListener(final JTable table, final NodeListTableColumnModel columnModel) {
        return new TableModelListener() {
            private Boolean reversed = null;
            private final String nonReversedText = tr("Nodes") + (table.getFont().canDisplay('\u25bc') ? " \u25bc" : " (1-n)");
            private final String reversedText = tr("Nodes") + (table.getFont().canDisplay('\u25b2') ? " \u25b2" : " (n-1)");

            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getSource() instanceof DiffTableModel) {
                    final DiffTableModel model = (DiffTableModel) e.getSource();
                    if (reversed == null || reversed != model.isReversed()) {
                        reversed = model.isReversed();
                        columnModel.getColumn(0).setHeaderValue(reversed ? reversedText : nonReversedText);
                        table.getTableHeader().repaint();
                    }
                }
            }
        };
    }

    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // ---------------------------
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.insets = new Insets(5,5,5,0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        referenceInfoPanel = new VersionInfoPanel(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
        add(referenceInfoPanel,gc);

        gc.gridx = 1;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        currentInfoPanel = new VersionInfoPanel(model, PointInTimeType.CURRENT_POINT_IN_TIME);
        add(currentInfoPanel,gc);

        adjustmentSynchronizer = new AdjustmentSynchronizer();
        selectionSynchronizer = new SelectionSynchronizer();

        popupMenu = new NodeListPopupMenu();

        // ---------------------------
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0.5;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(embeddInScrollPane(buildReferenceNodeListTable()),gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0.5;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(embeddInScrollPane(buildCurrentNodeListTable()),gc);
    }

    public NodeListViewer(HistoryBrowserModel model) {
        setModel(model);
        build();
    }

    protected void unregisterAsObserver(HistoryBrowserModel model) {
        if (currentInfoPanel != null) {
            model.deleteObserver(currentInfoPanel);
        }
        if (referenceInfoPanel != null) {
            model.deleteObserver(referenceInfoPanel);
        }
    }
    protected void registerAsObserver(HistoryBrowserModel model) {
        if (currentInfoPanel != null) {
            model.addObserver(currentInfoPanel);
        }
        if (referenceInfoPanel != null) {
            model.addObserver(referenceInfoPanel);
        }
    }

    public void setModel(HistoryBrowserModel model) {
        if (this.model != null) {
            unregisterAsObserver(model);
        }
        this.model = model;
        if (this.model != null) {
            registerAsObserver(model);
        }
    }

    static class NodeListPopupMenu extends JPopupMenu {
        private final ZoomToNodeAction zoomToNodeAction;
        private final ShowHistoryAction showHistoryAction;

        public NodeListPopupMenu() {
            zoomToNodeAction = new ZoomToNodeAction();
            add(zoomToNodeAction);
            showHistoryAction = new ShowHistoryAction();
            add(showHistoryAction);
        }

        public void prepare(PrimitiveId pid){
            zoomToNodeAction.setPrimitiveId(pid);
            zoomToNodeAction.updateEnabledState();

            showHistoryAction.setPrimitiveId(pid);
            showHistoryAction.updateEnabledState();
        }
    }

    static class ZoomToNodeAction extends AbstractAction {
        private transient PrimitiveId primitiveId;

        /**
         * Constructs a new {@code ZoomToNodeAction}.
         */
        public ZoomToNodeAction() {
            putValue(NAME, tr("Zoom to node"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to this node in the current data layer"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "zoomin"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            OsmPrimitive p = getPrimitiveToZoom();
            if (p != null) {
                OsmDataLayer editLayer = Main.main.getEditLayer();
                if (editLayer != null) {
                    editLayer.data.setSelected(p.getPrimitiveId());
                    AutoScaleAction.autoScale("selection");
                }
            }
        }

        public void setPrimitiveId(PrimitiveId pid) {
            this.primitiveId = pid;
            updateEnabledState();
        }

        protected OsmPrimitive getPrimitiveToZoom() {
            if (primitiveId == null) return null;
            OsmDataLayer editLayer = Main.main.getEditLayer();
            if (editLayer == null) return null;
            return editLayer.data.getPrimitiveById(primitiveId);
        }

        public void updateEnabledState() {
            if (!Main.main.hasEditLayer()) {
                setEnabled(false);
                return;
            }
            setEnabled(getPrimitiveToZoom() != null);
        }
    }

    static class ShowHistoryAction extends AbstractAction {
        private transient PrimitiveId primitiveId;

        /**
         * Constructs a new {@code ShowHistoryAction}.
         */
        public ShowHistoryAction() {
            putValue(NAME, tr("Show history"));
            putValue(SHORT_DESCRIPTION, tr("Open a history browser with the history of this node"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "history"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled()) return;
            run();
        }

        public void setPrimitiveId(PrimitiveId pid) {
            this.primitiveId = pid;
            updateEnabledState();
        }

        public void run() {
            if (HistoryDataSet.getInstance().getHistory(primitiveId) == null) {
                Main.worker.submit(new HistoryLoadTask().add(primitiveId));
            }
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    final History h = HistoryDataSet.getInstance().getHistory(primitiveId);
                    if (h == null)
                        return;
                    GuiHelper.runInEDT(new Runnable() {
                        @Override public void run() {
                            HistoryBrowserDialogManager.getInstance().show(h);
                        }
                    });
                }
            };
            Main.worker.submit(r);
        }

        public void updateEnabledState() {
            setEnabled(primitiveId != null && !primitiveId.isNew());
        }
    }

    private static PrimitiveId primitiveIdAtRow(TableModel model, int row) {
        DiffTableModel castedModel = (DiffTableModel) model;
        Long id = (Long)castedModel.getValueAt(row, 0).value;
        if(id == null) return null;
        return new SimplePrimitiveId(id, OsmPrimitiveType.NODE);
    }

    class InternalPopupMenuLauncher extends PopupMenuLauncher {
        public InternalPopupMenuLauncher() {
            super(popupMenu);
        }

        @Override protected int checkTableSelection(JTable table, Point p) {
            int row = super.checkTableSelection(table, p);
            popupMenu.prepare(primitiveIdAtRow(table.getModel(), row));
            return row;
        }
    }

    static class DoubleClickAdapter extends MouseAdapter {
        private JTable table;
        private ShowHistoryAction showHistoryAction;

        public DoubleClickAdapter(JTable table) {
            this.table = table;
            showHistoryAction = new ShowHistoryAction();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() < 2) return;
            int row = table.rowAtPoint(e.getPoint());
            if(row <= 0) return;
            PrimitiveId pid = primitiveIdAtRow(table.getModel(), row);
            if (pid == null || pid.isNew())
                return;
            showHistoryAction.setPrimitiveId(pid);
            showHistoryAction.run();
        }
    }
}
