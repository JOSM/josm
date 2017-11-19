// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Selects  members in the relation editor which refer to primitives in the current selection of the context layer.
 * @since 9496
 */
public class SelectedMembersForSelectionAction extends AddFromSelectionAction {

    /**
     * Constructs a new {@code SelectedMembersForSelectionAction}.
     * @param memberTableModel member table model
     * @param selectionTableModel selection table model
     * @param layer OSM data layer
     */
    public SelectedMembersForSelectionAction(MemberTableModel memberTableModel, SelectionTableModel selectionTableModel, OsmDataLayer layer) {
        super(null, memberTableModel, null, selectionTableModel, null, layer, null);
        putValue(SHORT_DESCRIPTION, tr("Select relation members which refer to objects in the current selection"));
        new ImageProvider("dialogs/relation", "selectmembers").getResource().attachImageIcon(this, true);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        boolean enabled = selectionTableModel.getRowCount() > 0
        && !memberTableModel.getChildPrimitives(layer.data.getSelected()).isEmpty();

        if (enabled) {
            putValue(SHORT_DESCRIPTION, tr("Select relation members which refer to {0} objects in the current selection",
                    memberTableModel.getChildPrimitives(layer.data.getSelected()).size()));
        } else {
            putValue(SHORT_DESCRIPTION, tr("Select relation members which refer to objects in the current selection"));
        }
        setEnabled(enabled);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        memberTableModel.selectMembersReferringTo(layer.data.getSelected());
    }
}
