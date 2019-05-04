// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * Selection table of relation editor.
 * @since 2563
 */
public class SelectionTable extends JTable {

    private final SelectionTableModel model;
    private final MemberTableModel memberTableModel;

    /**
     * Constructs a new {@code SelectionTable}.
     * @param model table model
     * @param memberTableModel member table model
     */
    public SelectionTable(SelectionTableModel model, MemberTableModel memberTableModel) {
        super(model, new SelectionTableColumnModel(memberTableModel));
        this.model = model;
        this.memberTableModel = memberTableModel;
        build();
    }

    protected void build() {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addMouseListener(new DoubleClickAdapter());
        memberTableModel.addTableModelListener(e -> repaint());
    }

    class DoubleClickAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent evt) {
            if (!(SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() > 1))
                return;
            int row = rowAtPoint(evt.getPoint());
            memberTableModel.selectMembersReferringTo(Collections.singleton(model.getPrimitive(row)));
        }
    }
}
