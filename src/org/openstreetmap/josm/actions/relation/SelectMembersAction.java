package org.openstreetmap.josm.actions.relation;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.tools.ImageProvider;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
* Sets the current selection to the list of relations selected in this dialog
*/
public class SelectMembersAction extends AbstractRelationAction {
    boolean add;
    public SelectMembersAction(boolean add) {
        putValue(SHORT_DESCRIPTION,add ? tr("Add the members of all selected relations to current selection")
                : tr("Select the members of all selected relations"));
        putValue(SMALL_ICON, ImageProvider.get("selectall"));
        putValue(NAME, add ? tr("Select members (add)") : tr("Select members"));
        this.add = add;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty()) return;
        
        HashSet<OsmPrimitive> members = new HashSet<OsmPrimitive>();
        for(Relation r: relations) {
            members.addAll(r.getMemberPrimitives());
        }
        if(add) {
            Main.map.mapView.getEditLayer().data.addSelected(members);
        } else {
            Main.map.mapView.getEditLayer().data.setSelected(members);
        }
    }
}