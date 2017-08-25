// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import org.openstreetmap.josm.gui.tagging.TagTableColumnModelBuilder;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTable;

/**
 * This table presents the tags that are conflicting to the user.
 */
public class TagConflictResolverTable extends JosmTable implements MultiValueCellEditor.NavigationListener {

    /**
     * Constructs a new {@code TagConflictResolverTable}.
     * @param model table model
     */
    public TagConflictResolverTable(TagConflictResolverModel model) {
        super(model, new TagTableColumnModelBuilder(new MultiValueCellRenderer(), "", tr("Key"), tr("Value"))
                .setWidth(20, 0).setPreferredWidth(20, 0).setMaxWidth(30, 0)
                .setCellEditor(new MultiValueCellEditor(), 2).build());

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
