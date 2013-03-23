// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action for editing a relation 
 * @since 5793
 */
public class EditRelationAction extends AbstractRelationAction  {

    /**
     * Constructs a new <code>EditRelationAction</code>.
     */
    public EditRelationAction() {
        putValue(NAME, tr("Edit"));
        putValue(SHORT_DESCRIPTION, tr("Call relation editor for selected relation"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
    }

    /**
     * Returns the set of currently selected relation members for the given relation. 
     * @param r The relation to inspect
     * @return The set of currently selected relation members for the given relation.
     */
    public static Set<RelationMember> getMembersForCurrentSelection(Relation r) {
        Set<RelationMember> members = new HashSet<RelationMember>();
        if (Main.map != null && Main.map.mapView != null) {
            OsmDataLayer editLayer = Main.map.mapView.getEditLayer();
            if (editLayer != null && editLayer.data != null) {
                Collection<OsmPrimitive> selection = editLayer.data.getSelected();
                for (RelationMember member: r.getMembers()) {
                    if (selection.contains(member.getMember())) {
                        members.add(member);
                    }
                }
            }
        }
        return members;
    }

    /**
     * Launches relation editor for the given relation.
     * @param toEdit The relation to edit
     */
    public static void launchEditor(Relation toEdit) {
        if (toEdit == null || Main.map==null || Main.map.mapView==null) return;
        RelationEditor.getEditor(Main.map.mapView.getEditLayer(), toEdit,
                getMembersForCurrentSelection(toEdit)).setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.size()!=1) return;
        launchEditor(relations.iterator().next());
    }

    @Override
    protected void updateEnabledState() {
        // only one selected relation can be edited
        setEnabled( relations.size()==1 );
    }
}
