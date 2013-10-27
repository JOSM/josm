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
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

/**
 * Action for downloading incomplete members of selected relations
 * @since 5793
 */
public class DownloadSelectedIncompleteMembersAction extends AbstractRelationAction {

    /**
     * Constructs a new <code>DownloadSelectedIncompleteMembersAction</code>.
     */
    public DownloadSelectedIncompleteMembersAction() {
        putValue(SHORT_DESCRIPTION, tr("Download incomplete members of selected relations"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs/relation", "downloadincompleteselected"));
        putValue(NAME, tr("Download incomplete members"));
    }

    /**
     * Returns the set of incomplete members of the given relations.
     * @param rels The relations to inspect.
     * @return The set of incomplete members of the given relations.
     */
    public Set<OsmPrimitive> buildSetOfIncompleteMembers(Collection<Relation> rels) {
        Set<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        for (Relation r : rels) {
            ret.addAll(r.getIncompleteMembers());
        }
        return ret;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty() || !Main.isDisplayingMapView()) return;
        Main.worker.submit(new DownloadRelationMemberTask(
                relations,
                buildSetOfIncompleteMembers(relations),
                Main.main.getEditLayer()));
    }

    @Override
    public void setPrimitives(Collection<? extends OsmPrimitive> primitives) {
        // selected relations with incomplete members
        this.relations = Utils.filter(getRelations(primitives), new Predicate<Relation>(){
            @Override public boolean evaluate(Relation r) {
                return !r.isNew() && r.hasIncompleteMembers();
            }});
        updateEnabledState();
    }
}
