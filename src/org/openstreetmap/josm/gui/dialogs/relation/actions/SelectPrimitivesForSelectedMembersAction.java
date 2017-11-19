// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Selects primitives in the layer this editor belongs to. The selected primitives are
 * equal to the set of primitives the currently selected relation members refer to.
 * @since 9496
 */
public class SelectPrimitivesForSelectedMembersAction extends AbstractRelationEditorAction {

    /**
     * Select objects for selected relation members.
     * @param memberTable member table
     * @param memberTableModel member table model
     * @param layer layer
     */
    public SelectPrimitivesForSelectedMembersAction(MemberTable memberTable, MemberTableModel memberTableModel, OsmDataLayer layer) {
        super(memberTable, memberTableModel, null, layer, null);
        putValue(SHORT_DESCRIPTION, tr("Select objects for selected relation members"));
        new ImageProvider("dialogs/relation", "selectprimitives").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(memberTable.getSelectedRowCount() > 0);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        layer.data.setSelected(memberTableModel.getSelectedChildPrimitives());
    }
}
