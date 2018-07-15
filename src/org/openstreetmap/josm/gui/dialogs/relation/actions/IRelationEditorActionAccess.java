package org.openstreetmap.josm.gui.dialogs.relation.actions;

import javax.swing.Action;

import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTable;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;

/**
 * This interface provides access to the relation editor for actions.
 * <p>
 * 
 * @author Michael Zangl
 * @since 14027
 */
public interface IRelationEditorActionAccess {

    /**
     * Adds a keyboard action to the member table.
     * @param actionMapKey The key to use
     * @param action The action to map for that key.
     */
    default void addMemberTableAction(String actionMapKey, Action action) {
        getMemberTable().getActionMap().put(actionMapKey, action);
    }

    /**
     * Get the member table that is used by the dialog.
     * @return The member table
     */
    MemberTable getMemberTable();
    
    /**
     * Get the model the member table is using.
     * @return That model
     */
    MemberTableModel getMemberTableModel();
    
    /**
     * Get the table that displays the current user selection
     * @return That table
     */
    SelectionTable getSelectionTable();
    
    /**
     * Get the model that the selection table is based on.
     * @return The model
     */
    SelectionTableModel getSelectionTableModel();
    
    IRelationEditor getEditor();
    TagEditorModel getTagModel();
    
    /**
     * Get the text field that is used to edit the role.
     * @return The role text field.
     */
    AutoCompletingTextField getTextFieldRole();
}
