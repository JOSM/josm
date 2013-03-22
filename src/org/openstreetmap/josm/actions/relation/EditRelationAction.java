package org.openstreetmap.josm.actions.relation;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The action for editing a relation 
 */
public class EditRelationAction extends AbstractRelationAction  {
    public EditRelationAction() {
        putValue(NAME, tr("Edit"));
        putValue(SHORT_DESCRIPTION, tr("Call relation editor for selected relation"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
    }

    public static Collection<RelationMember> getMembersForCurrentSelection(Relation r) {
        Collection<RelationMember> members = new HashSet<RelationMember>();
        Collection<OsmPrimitive> selection = Main.map.mapView.getEditLayer().data.getSelected();
        for (RelationMember member: r.getMembers()) {
            if (selection.contains(member.getMember())) {
                members.add(member);
            }
        }
        return members;
    }

    public static void launchEditor(Relation toEdit) {
        if (toEdit == null) return;
        RelationEditor.getEditor(Main.map.mapView.getEditLayer(), toEdit,
                getMembersForCurrentSelection(toEdit)).setVisible(true);
    }

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
