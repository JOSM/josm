// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;

public class MemberRoleCellEditor extends AbstractCellEditor implements TableCellEditor {
    private AutoCompletingTextField editor = null;
    private DataSet ds;

    /** user input is matched against this list of auto completion items */
    private AutoCompletionList autoCompletionList = null;

    /**
     * constructor
     */
    public MemberRoleCellEditor(DataSet ds) {
        this.ds = ds;
        editor = new AutoCompletingTextField();
        editor.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        autoCompletionList = new AutoCompletionList();
        editor.setAutoCompletionList(autoCompletionList);
    }

    /**
     * replies the table cell editor
     */
    @Override
    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {

        String role = (String)value;
        editor.setText(role);
        autoCompletionList.clear();
        ds.getAutoCompletionManager().populateWithMemberRoles(autoCompletionList);
        return editor;
    }

    @Override
    public Object getCellEditorValue() {
        return editor.getText();
    }

    /** Returns the edit field for this cell editor. */
    public AutoCompletingTextField getEditor() {
        return editor;
    }
}
