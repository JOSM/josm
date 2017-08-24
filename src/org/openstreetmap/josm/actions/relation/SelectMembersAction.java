// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Sets the current selection to the list of relations selected in this dialog
 * @since 5793
 */
public class SelectMembersAction extends AbstractRelationAction {

    private final boolean add;

    /**
     * Constructs a new <code>SelectMembersAction</code>.
     * @param add if <code>true</code>, the members will be added to current selection.
     * If <code>false</code>, the members will replace the current selection.
     */
    public SelectMembersAction(boolean add) {
        putValue(SHORT_DESCRIPTION, add ? tr("Add the members of all selected relations to current selection")
                : tr("Select the members of all selected relations"));
        putValue(SMALL_ICON, ImageProvider.get("selectall"));
        putValue(NAME, add ? tr("Select members (add)") : tr("Select members"));
        this.add = add;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty() || !MainApplication.isDisplayingMapView()) return;

        Set<OsmPrimitive> members = new HashSet<>();
        for (Relation r: relations) {
            members.addAll(r.getMemberPrimitivesList());
        }
        if (add) {
            MainApplication.getLayerManager().getEditLayer().data.addSelected(members);
        } else {
            MainApplication.getLayerManager().getEditLayer().data.setSelected(members);
        }
    }
}
