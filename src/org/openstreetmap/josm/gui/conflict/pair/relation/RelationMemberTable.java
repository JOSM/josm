package org.openstreetmap.josm.gui.conflict.pair.relation;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.actions.ZoomToAction;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTable;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTableModel;

public class RelationMemberTable extends OsmPrimitivesTable {

    public RelationMemberTable(String name, OsmPrimitivesTableModel dm, ListSelectionModel sm) {
        super(dm, new RelationMemberListColumnModel(), sm);
        setName(name);
        setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    @Override
    protected ZoomToAction buildZoomToAction() {
        return new ZoomToAction(this);
    }
}
