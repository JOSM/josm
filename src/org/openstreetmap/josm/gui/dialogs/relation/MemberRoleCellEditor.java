// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;
import java.util.logging.Logger;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.gui.tagging.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionCache;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionItemPritority;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionListItem;

public class MemberRoleCellEditor extends AbstractCellEditor implements TableCellEditor {

    /** the logger object */
    static private Logger logger = Logger.getLogger(MemberRoleCellEditor.class.getName());

    private AutoCompletingTextField editor = null;

    /** the cache of auto completion items derived from the current JOSM data set */
    private AutoCompletionCache acCache = null;

    /** user input is matched against this list of auto completion items */
    private AutoCompletionList autoCompletionList = null;

    /**
     * constructor
     */
    public MemberRoleCellEditor() {
        editor = new AutoCompletingTextField();
        acCache = new AutoCompletionCache();
    }

    /**
     * initializes the autocompletion editor with the list of member roles in
     * the current dataset
     * 
     */
    protected void initAutoCompletionListRoles() {
        if (autoCompletionList == null) {
            logger.warning("autoCompletionList is null. Make sure an instance of AutoCompletionList is injected into MemberRoleCellEditor.");
            return;
        }
        autoCompletionList.clear();

        // add the list of keys in the current data set
        //
        for (String key : acCache.getMemberRoles()) {
            autoCompletionList.add(
                    new AutoCompletionListItem(key, AutoCompletionItemPritority.IS_IN_DATASET)
            );
        }
        autoCompletionList.fireTableDataChanged();
    }

    /**
     * replies the table cell editor
     */
    public Component getTableCellEditorComponent(JTable table,
            Object value, boolean isSelected, int row, int column) {

        String role = (String)value;
        editor.setText(role);
        initAutoCompletionListRoles();
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

    /**
     * replies the {@link AutoCompletionList} this table cell editor synchronizes with
     * 
     * @return the auto completion list
     */
    public AutoCompletionList getAutoCompletionList() {
        return autoCompletionList;
    }

    /**
     * sets the {@link AutoCompletionList} this table cell editor synchronizes with
     * @param autoCompletionList the auto completion list
     */
    public void setAutoCompletionList(AutoCompletionList autoCompletionList) {
        this.autoCompletionList = autoCompletionList;
        editor.setAutoCompletionList(autoCompletionList);
    }

    public void setAutoCompletionCache(AutoCompletionCache acCache) {
        this.acCache = acCache;
    }

    public AutoCompletingTextField getEditor() {
        return editor;
    }
}
