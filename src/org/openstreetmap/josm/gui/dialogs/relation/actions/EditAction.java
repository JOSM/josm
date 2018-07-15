// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Action for editing the currently selected relation.
 * @since 9496
 */
public class EditAction extends AbstractRelationEditorAction {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code EditAction}.
     * @param editorAccess An interface to access the relation editor contents.
     */
    public EditAction(IRelationEditorActionAccess editorAccess) {
        super(editorAccess, IRelationEditorUpdateOn.MEMBER_TABLE_SELECTION);
        putValue(SHORT_DESCRIPTION, tr("Edit the relation the currently selected relation member refers to"));
        new ImageProvider("dialogs", "edit").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(editorAccess.getMemberTable().getSelectedRowCount() == 1
                && editorAccess.getMemberTableModel()
                        .isEditableRelation(editorAccess.getMemberTable().getSelectedRow()));
    }

    protected Collection<RelationMember> getMembersForCurrentSelection(Relation r) {
        Collection<RelationMember> members = new HashSet<>();
        Collection<OsmPrimitive> selection = getLayer().data.getSelected();
        for (RelationMember member: r.getMembers()) {
            if (selection.contains(member.getMember())) {
                members.add(member);
            }
        }
        return members;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        int idx = editorAccess.getMemberTable().getSelectedRow();
        if (idx < 0)
            return;
        OsmPrimitive primitive = editorAccess.getMemberTableModel().getReferredPrimitive(idx);
        if (!(primitive instanceof Relation))
            return;
        Relation r = (Relation) primitive;
        if (r.isIncomplete())
            return;

        RelationEditor.getEditor(getLayer(), r, getMembersForCurrentSelection(r)).setVisible(true);
    }
}
