// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

/**
 * This interface can be used to register the event listeners for a
 * {@link AbstractRelationEditorAction}.
 * <p>
 * It holds common constants that are often used.
 *
 * @author Michael Zangl
 * @since 14027
 */
@FunctionalInterface
public interface IRelationEditorUpdateOn {
    /**
     * Update when the member table contents change
     */
    IRelationEditorUpdateOn MEMBER_TABLE_CHANGE = (editor, action) -> editor.getMemberTableModel()
            .addTableModelListener(action);
    /**
     * Update upon a member table selection change
     */
    IRelationEditorUpdateOn MEMBER_TABLE_SELECTION = (editor, action) -> editor.getMemberTable().getSelectionModel()
            .addListSelectionListener(action);

    /**
     * Update when a tag of the relation changed
     */
    IRelationEditorUpdateOn TAG_CHANGE = (editor, action) -> editor.getTagModel().addPropertyChangeListener(action);

    /**
     * Update when a relation changed
     */
    IRelationEditorUpdateOn SELECTION_TABLE_CHANGE = (editor, action) -> editor.getSelectionTableModel()
            .addTableModelListener(action);

    /**
     * Registers the given action as listener on the relation editor.
     * @param editor The relation editor to register the listeners on
     * @param action The action that should be registered
     */
    void register(IRelationEditorActionAccess editor, AbstractRelationEditorAction action);
}
