// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Point;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;

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
        table.getTableHeader().setReorderingAllowed(false);
        table.addMouseListener(new InternalPopupMenuLauncher());
        table.addMouseListener(new ShowHistoryAction.DoubleClickAdapter(e -> {
            int row = table.rowAtPoint(e.getPoint());
            return primitiveIdAtRow(tableModel, row);
        }));
        return table;
    }

    private static PrimitiveId primitiveIdAtRow(DiffTableModel model, int row) {
        if (row < 0)
            return null;
        Long id = (Long) model.getValueAt(row, 0).value;
        return id == null ? null : new SimplePrimitiveId(id, OsmPrimitiveType.NODE);
    }

    static class InternalPopupMenuLauncher extends PopupMenuLauncher {
        InternalPopupMenuLauncher() {
            super(new ListPopupMenu(tr("Zoom to node"), tr("Zoom to this node in the current data layer")));
        }

        @Override
        protected int checkTableSelection(JTable table, Point p) {
            int row = super.checkTableSelection(table, p);
            ((ListPopupMenu) menu).prepare(primitiveIdAtRow((DiffTableModel) table.getModel(), row));
            return row;
        }
    }

}
