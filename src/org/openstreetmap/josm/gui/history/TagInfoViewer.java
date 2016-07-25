// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

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
    private static final class RepaintOnFocusChange implements FocusListener {
        @Override
        public void focusLost(FocusEvent e) {
            repaintSelected(e);
        }

        @Override
        public void focusGained(FocusEvent e) {
            repaintSelected(e);
        }

        private static void repaintSelected(FocusEvent e) {
            // we would only need the selected rows, but this is easier:
            e.getComponent().repaint();
        }
    }

    /**
     * Constructs a new {@code TagInfoViewer}.
     * @param model The history browsing model
     */
    public TagInfoViewer(HistoryBrowserModel model) {
        super(model);
    }

    @Override
    protected JTable buildReferenceTable() {
        JTable table = new JTable(
                model.getTagTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME),
                new TagTableColumnModel()
        );
        table.setName("table.referencetagtable");
        setUpDataTransfer(table);
        return table;
    }

    @Override
    protected JTable buildCurrentTable() {
        JTable table = new JTable(
                model.getTagTableModel(PointInTimeType.CURRENT_POINT_IN_TIME),
                new TagTableColumnModel()
        );
        table.setName("table.currenttagtable");
        setUpDataTransfer(table);
        return table;
    }

    private void setUpDataTransfer(JTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.setTransferHandler(new TagInfoTransferHandler());
        table.addFocusListener(new RepaintOnFocusChange());
    }
}
