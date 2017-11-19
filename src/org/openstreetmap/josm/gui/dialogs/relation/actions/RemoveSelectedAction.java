// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Remove all members referring to one of the selected objects.
 * @since 9496
 */
public class RemoveSelectedAction extends AddFromSelectionAction {

    /**
     * Constructs a new {@code RemoveSelectedAction}.
     * @param memberTableModel member table model
     * @param selectionTableModel selection table model
     * @param layer OSM data layer
     */
    public RemoveSelectedAction(MemberTableModel memberTableModel, SelectionTableModel selectionTableModel, OsmDataLayer layer) {
        super(null, memberTableModel, null, selectionTableModel, null, layer, null);
        putValue(SHORT_DESCRIPTION, tr("Remove all members referring to one of the selected objects"));
        new ImageProvider("dialogs/relation", "deletemembers").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        DataSet ds = layer.data;
        if (ds == null || ds.selectionEmpty()) {
            setEnabled(false);
            return;
        }
        // only enable the action if we have members referring to the selected primitives
        setEnabled(memberTableModel.hasMembersReferringTo(ds.getSelected()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        memberTableModel.removeMembersReferringTo(selectionTableModel.getSelection());
    }
}
