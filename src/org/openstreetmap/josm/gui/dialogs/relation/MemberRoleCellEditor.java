// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.tagging.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionCache;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;

public class MemberRoleCellEditor extends AbstractCellEditor implements TableCellEditor {

    /** the logger object */
    static private Logger logger = Logger.getLogger(MemberRoleCellEditor.class.getName());

    private AutoCompletingTextField editor = null;

    /** user input is matched against this list of auto completion items */
    private AutoCompletionList autoCompletionList = null;

    /**
     * constructor
     */
    public MemberRoleCellEditor() {
        editor = new AutoCompletingTextField();
        autoCompletionList = new AutoCompletionList();
        editor.setAutoCompletionList(autoCompletionList);
    }

    /**
     * replies the table cell editor
     */
    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {

        String role = (String)value;
        editor.setText(role);
        AutoCompletionCache.getCacheForLayer(Main.main.getEditLayer()).populateWithMemberRoles(autoCompletionList);
        return editor;
    }

    public Object getCellEditorValue() {
        return editor.getText();
    }

    @Override
    public void cancelCellEditing() {
        super.cancelCellEditing();
    }

    @Override
    public boolean stopCellEditing() {
        return super.stopCellEditing();
    }
}
