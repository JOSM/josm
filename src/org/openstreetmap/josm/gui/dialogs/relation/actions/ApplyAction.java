// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Apply the current updates.
 * @since 9496
 */
public class ApplyAction extends SavingAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code ApplyAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param layer OSM data layer
     * @param editor relation editor
     * @param tagModel tag editor model
     */
    public ApplyAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_CHANGE, IRelationEditorUpdateOn.TAG_CHANGE);
        putValue(SHORT_DESCRIPTION, tr("Apply the current updates"));
        new ImageProvider("save").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Apply"));
        updateEnabledState();        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (applyChanges()) {
            editorAccess.getEditor().reloadDataFromRelation();
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(isEditorDirty());
    }
}
