// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationAware;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Apply the current updates.
 * @since 9496
 */
public class ApplyAction extends SavingAction {

    /**
     * Constructs a new {@code ApplyAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param layer OSM data layer
     * @param editor relation editor
     * @param tagModel tag editor model
     */
    public ApplyAction(MemberTable memberTable, MemberTableModel memberTableModel, TagEditorModel tagModel, OsmDataLayer layer,
            RelationAware editor) {
        super(memberTable, memberTableModel, tagModel, layer, editor, null);
        putValue(SHORT_DESCRIPTION, tr("Apply the current updates"));
        putValue(SMALL_ICON, ImageProvider.get("save"));
        putValue(NAME, tr("Apply"));
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (editor.getRelation() == null) {
            applyNewRelation(tagModel);
        } else if (!memberTableModel.hasSameMembersAs(editor.getRelationSnapshot()) || tagModel.isDirty()) {
            if (editor.isDirtyRelation()) {
                if (confirmClosingBecauseOfDirtyState()) {
                    if (layer.getConflicts().hasConflictForMy(editor.getRelation())) {
                        warnDoubleConflict();
                        return;
                    }
                    applyExistingConflictingRelation(tagModel);
                    if (editor instanceof Component) {
                        ((Component) editor).setVisible(false);
                    }
                }
            } else {
                applyExistingNonConflictingRelation(tagModel);
            }
        }
    }
}
