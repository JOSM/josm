// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import javax.swing.Action;

import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTable;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
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

    /**
     * Get the current relation editor
     * @return The relation editor object.
     */
    IRelationEditor getEditor();

    /**
     * Gets the model for the tag table.
     * @return The tag editor model.
     */
    TagEditorModel getTagModel();

    /**
     * Get the changed relation
     * @return The changed relation (note: will not be part of a dataset). This should never be {@code null}.
     * @since 18413
     */
    default IRelation<?> getChangedRelation() {
        final Relation newRelation;
        final Relation oldRelation = getEditor().getRelation();
        boolean isUploadInProgress = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class)
                .stream().anyMatch(OsmDataLayer::isUploadInProgress);
        if (isUploadInProgress || (oldRelation != null && oldRelation.getDataSet() != null && oldRelation.getDataSet().isLocked())) {
            // If the dataset is locked, then we cannot change the relation. See JOSM #22024.
            // We should also avoid changing the relation if there is an upload in progress. See JOSM #22268/#22398.
            // There appears to be a race condition where a dataset might not be locked in the check, then is locked while using the
            // copy relation constructor.
            // This is due to the `setMembers` -> `addReferrer` call chain requires that the dataset is not read only.
            return oldRelation;
        } else if (oldRelation != null) {
            newRelation = new Relation(oldRelation);
        } else {
            newRelation = new Relation();
        }
        getTagModel().applyToPrimitive(newRelation);
        getMemberTableModel().applyToRelation(newRelation);
        return newRelation;
    }

    /**
     * Get the text field that is used to edit the role.
     * @return The role text field.
     */
    AutoCompletingTextField getTextFieldRole();

    /**
     * Tells the member table editor to stop editing and accept any partially edited value as the value of the editor.
     * The editor returns false if editing was not stopped; this is useful for editors that validate and can not accept invalid entries.
     * @return {@code true} if editing was stopped; {@code false} otherwise
     * @since 18118
     */
    default boolean stopMemberCellEditing() {
        return getMemberTable().isEditing() && getMemberTable().getCellEditor().stopCellEditing();
    }
}
