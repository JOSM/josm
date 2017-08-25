// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.CellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;

/**
 * The {@link CellEditor} for the role cell in the table. Supports autocompletion.
 */
public class MemberRoleCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final AutoCompletingTextField editor;
    private final transient DataSet ds;
    private final transient Relation relation;

    /** user input is matched against this list of auto completion items */
    private final AutoCompletionList autoCompletionList;

    /**
     * Constructs a new {@code MemberRoleCellEditor}.
     * @param ds the data set. Must not be null
     * @param relation the relation. Can be null
     */
    public MemberRoleCellEditor(DataSet ds, Relation relation) {
        this.ds = ds;
        this.relation = relation;
        editor = new AutoCompletingTextField(0, false);
        editor.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        autoCompletionList = new AutoCompletionList();
        editor.setAutoCompletionList(autoCompletionList);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {

        String role = (String) value;
        editor.setText(role);
        autoCompletionList.clear();
        ds.getAutoCompletionManager().populateWithMemberRoles(autoCompletionList, relation);
        return editor;
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getText();
    }

    /**
     * Returns the edit field for this cell editor.
     * @return the edit field for this cell editor
     */
    public AutoCompletingTextField getEditor() {
        return editor;
    }
}
