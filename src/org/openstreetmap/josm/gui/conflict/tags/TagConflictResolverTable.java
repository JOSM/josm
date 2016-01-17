// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTable;

public class TagConflictResolverTable extends JosmTable implements MultiValueCellEditor.NavigationListener {

    /**
     * Constructs a new {@code TagConflictResolverTable}.
     * @param model table model
     */
    public TagConflictResolverTable(TagConflictResolverModel model) {
        super(model, new TagConflictResolverColumnModel());

        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        installCustomNavigation(2);

        ((MultiValueCellEditor) getColumnModel().getColumn(2).getCellEditor()).addNavigationListener(this);

        setRowHeight((int) new JosmComboBox<String>().getPreferredSize().getHeight());
    }

    @Override
    public void gotoNextDecision() {
        selectNextColumnCellAction.actionPerformed(null);
    }

    @Override
    public void gotoPreviousDecision() {
        selectPreviousColumnCellAction.actionPerformed(null);
    }
}
