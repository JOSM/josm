// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.AutoScaleAction.AutoScaleMode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.gui.MainApplication;
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
 * @since 1709
 */
public class NodeListViewer extends HistoryViewerPanel {

    /**
     * Constructs a new {@code NodeListViewer}.
     * @param model history browser model
     */
    public NodeListViewer(HistoryBrowserModel model) {
        super(model);
    }

    @Override
    protected JTable buildTable(PointInTimeType pointInTimeType) {
        final DiffTableModel tableModel = model.getNodeListTableModel(pointInTimeType);
        final NodeListTableColumnModel columnModel = new NodeListTableColumnModel();
        final JTable table = new JTable(tableModel, columnModel);
        tableModel.addTableModelListener(new ReversedChangeListener(table, columnModel, tr("The nodes of this way are in reverse order")));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.addMouseListener(new InternalPopupMenuLauncher());
        table.addMouseListener(new ShowHistoryAction.DoubleClickAdapter(e -> {
            int row = table.rowAtPoint(e.getPoint());
            return row < 0 ? null : primitiveIdAtRow(tableModel, row);
        }));
        return table;
    }

    static class NodeListPopupMenu extends JPopupMenu {
        private final ZoomToNodeAction zoomToNodeAction;
        private final ShowHistoryAction showHistoryAction;

        NodeListPopupMenu() {
            zoomToNodeAction = new ZoomToNodeAction();
            add(zoomToNodeAction);
            showHistoryAction = new ShowHistoryAction();
            add(showHistoryAction);
        }

        public void prepare(PrimitiveId pid) {
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
        ZoomToNodeAction() {
            putValue(NAME, tr("Zoom to node"));
            putValue(SHORT_DESCRIPTION, tr("Zoom to this node in the current data layer"));
            new ImageProvider("dialogs", "zoomin").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            IPrimitive p = getPrimitiveToZoom();
            if (p != null) {
                OsmData<?, ?, ?, ?> ds = MainApplication.getLayerManager().getActiveData();
                if (ds != null) {
                    ds.setSelected(p.getPrimitiveId());
                    AutoScaleAction.autoScale(AutoScaleMode.SELECTION);
                }
            }
        }

        public void setPrimitiveId(PrimitiveId pid) {
            this.primitiveId = pid;
            updateEnabledState();
        }

        protected IPrimitive getPrimitiveToZoom() {
            if (primitiveId == null)
                return null;
            OsmData<?, ?, ?, ?> ds = MainApplication.getLayerManager().getActiveData();
            if (ds == null)
                return null;
            return ds.getPrimitiveById(primitiveId);
        }

        public void updateEnabledState() {
            setEnabled(MainApplication.getLayerManager().getActiveData() != null && getPrimitiveToZoom() != null);
        }
    }

    private static PrimitiveId primitiveIdAtRow(DiffTableModel model, int row) {
        Long id = (Long) model.getValueAt(row, 0).value;
        return id == null ? null : new SimplePrimitiveId(id, OsmPrimitiveType.NODE);
    }

    static class InternalPopupMenuLauncher extends PopupMenuLauncher {
        InternalPopupMenuLauncher() {
            super(new NodeListPopupMenu());
        }

        @Override
        protected int checkTableSelection(JTable table, Point p) {
            int row = super.checkTableSelection(table, p);
            ((NodeListPopupMenu) menu).prepare(primitiveIdAtRow((DiffTableModel) table.getModel(), row));
            return row;
        }
    }

}
