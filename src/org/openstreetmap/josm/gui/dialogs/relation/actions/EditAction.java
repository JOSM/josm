// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Action for editing the currently selected relation.
 * @since 9496
 */
public class EditAction extends AbstractRelationEditorAction {

    /**
     * Constructs a new {@code EditAction}.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param layer layer
     */
    public EditAction(MemberTable memberTable, MemberTableModel memberTableModel, OsmDataLayer layer) {
        super(memberTable, memberTableModel, null, layer, null);
        putValue(SHORT_DESCRIPTION, tr("Edit the relation the currently selected relation member refers to"));
        new ImageProvider("dialogs", "edit").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(memberTable.getSelectedRowCount() == 1
                && memberTableModel.isEditableRelation(memberTable.getSelectedRow()));
    }

    protected Collection<RelationMember> getMembersForCurrentSelection(Relation r) {
        Collection<RelationMember> members = new HashSet<>();
        Collection<OsmPrimitive> selection = layer.data.getSelected();
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
        int idx = memberTable.getSelectedRow();
        if (idx < 0)
            return;
        OsmPrimitive primitive = memberTableModel.getReferredPrimitive(idx);
        if (!(primitive instanceof Relation))
            return;
        Relation r = (Relation) primitive;
        if (r.isIncomplete())
            return;

        RelationEditor.getEditor(layer, r, getMembersForCurrentSelection(r)).setVisible(true);
    }
}
