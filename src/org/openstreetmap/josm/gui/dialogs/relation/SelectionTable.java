// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class SelectionTable extends JTable {

    private SelectionTableModel model;
    private MemberTableModel memberTableModel;

    protected void build() {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new DoubleClickAdapter());
    }

    public SelectionTable(SelectionTableModel model, SelectionTableColumnModel columnModel) {
        super(model, columnModel);
        this.model = model;
        build();
    }

    public void setMemberTableModel(MemberTableModel memberTableModel) {
        this.memberTableModel = memberTableModel;
    }

    class DoubleClickAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent evt) {
            if (! (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() > 1))
                return;
            int row = rowAtPoint(evt.getPoint());
            OsmPrimitive primitive = model.getPrimitive(row);
            memberTableModel.selectMembersReferringTo(Collections.singleton(primitive));
        }
    }
}
