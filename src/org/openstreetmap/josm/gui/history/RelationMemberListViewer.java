// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * RelationMemberListViewer is a UI component which displays the  list of relation members of two
 * version of a {@link org.openstreetmap.josm.data.osm.Relation} in a {@link org.openstreetmap.josm.data.osm.history.History}.
 *
 * <ul>
 *   <li>on the left, it displays the list of relation members for the version at {@link PointInTimeType#REFERENCE_POINT_IN_TIME}</li>
 *   <li>on the right, it displays the list of relation members for the version at {@link PointInTimeType#CURRENT_POINT_IN_TIME}</li>
 * </ul>
 * @since 1709
 */
public class RelationMemberListViewer extends HistoryViewerPanel {

    protected static class MemberModelChanged implements TableModelListener {
        private final JTable table;

        protected MemberModelChanged(JTable table) {
            this.table = table;
        }

        @Override
        public void tableChanged(TableModelEvent e) {
            Rectangle rect = table.getCellRect(((DiffTableModel) e.getSource()).getFirstChange(), 0, true);
            table.scrollRectToVisible(rect);
        }
    }

    @Override
    protected JTable buildReferenceTable() {
        JTable table = new JTable(
                model.getRelationMemberTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME),
                new RelationMemberTableColumnModel()
                );
        table.setName("table.referencememberlisttable");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.getModel().addTableModelListener(new MemberModelChanged(table));
        return table;
    }

    @Override
    protected JTable buildCurrentTable() {
        JTable table = new JTable(
                model.getRelationMemberTableModel(PointInTimeType.CURRENT_POINT_IN_TIME),
                new RelationMemberTableColumnModel()
                );
        table.setName("table.currentmemberlisttable");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        table.getModel().addTableModelListener(new MemberModelChanged(table));
        return table;
    }

    /**
     * Constructs a new {@code RelationMemberListViewer}.
     * @param model The history browsing model
     */
    public RelationMemberListViewer(HistoryBrowserModel model) {
        super(model);
    }
}
