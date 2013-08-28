// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

/**
 * TagInfoViewer is a UI component which displays the list of tags of two
 * version of a {@link org.openstreetmap.josm.data.osm.OsmPrimitive} in a {@link org.openstreetmap.josm.data.osm.history.History}.
 *
 * <ul>
 *   <li>on the left, it displays the list of tags for the version at {@link PointInTimeType#REFERENCE_POINT_IN_TIME}</li>
 *   <li>on the right, it displays the list of tags for the version at {@link PointInTimeType#CURRENT_POINT_IN_TIME}</li>
 * </ul>
 *
 */
public class TagInfoViewer extends HistoryViewerPanel {

    protected JTable buildReferenceTable() {
        JTable table = new JTable(
                model.getTagTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME),
                new TagTableColumnModel()
        );
        table.setName("table.referencetagtable");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        return table;
    }

    protected JTable buildCurrentTable() {
        JTable table = new JTable(
                model.getTagTableModel(PointInTimeType.CURRENT_POINT_IN_TIME),
                new TagTableColumnModel()
        );
        table.setName("table.currenttagtable");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        return table;
    }

    /**
     * Constructs a new {@code TagInfoViewer}.
     * @param model The history browsing model
     */
    public TagInfoViewer(HistoryBrowserModel model) {
        super(model);
    }
}
