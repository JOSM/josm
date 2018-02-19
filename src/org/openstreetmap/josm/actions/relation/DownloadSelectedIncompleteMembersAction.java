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
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Action for downloading incomplete members of selected relations
 * @since 5793
 */
public class DownloadSelectedIncompleteMembersAction extends AbstractRelationAction {

    private transient Collection<OsmPrimitive> incompleteMembers;

    /**
     * Constructs a new <code>DownloadSelectedIncompleteMembersAction</code>.
     */
    public DownloadSelectedIncompleteMembersAction() {
        putValue(SHORT_DESCRIPTION, tr("Download incomplete members of selected relations"));
        new ImageProvider("dialogs/relation", "downloadincompleteselected").getResource().attachImageIcon(this, true);
        putValue(NAME, tr("Download incomplete members"));
    }

    /**
     * Returns the set of incomplete members of the given relations.
     * @param rels The relations to inspect.
     * @return The set of incomplete members of the given relations.
     */
    public static Set<OsmPrimitive> buildSetOfIncompleteMembers(Collection<Relation> rels) {
        Set<OsmPrimitive> ret = new HashSet<>();
        for (Relation r : rels) {
            ret.addAll(SubclassFilteredCollection.filter(r.getIncompleteMembers(), osm -> !osm.isNew()));
        }
        return ret;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty() || !MainApplication.isDisplayingMapView()) return;
        MainApplication.worker.submit(new DownloadRelationMemberTask(
                relations,
                incompleteMembers,
                MainApplication.getLayerManager().getEditLayer()));
    }

    @Override
    public void setPrimitives(Collection<? extends OsmPrimitive> primitives) {
        // selected relations with incomplete members
        this.relations = SubclassFilteredCollection.filter(getRelations(primitives), Relation::hasIncompleteMembers);
        this.incompleteMembers = buildSetOfIncompleteMembers(relations);
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(!relations.isEmpty() && !incompleteMembers.isEmpty() && !Main.isOffline(OnlineResource.OSM_API)
                && !relations.iterator().next().getDataSet().isReadOnly());
    }
}
