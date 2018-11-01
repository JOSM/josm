// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationTask;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * The action for downloading members of relations
 * @since 5793
 */
public class DownloadMembersAction extends AbstractRelationAction {

    /**
     * Constructs a new <code>DownloadMembersAction</code>.
     */
    public DownloadMembersAction() {
        putValue(SHORT_DESCRIPTION, tr("Download all members of the selected relations"));
        putValue(NAME, tr("Download members"));
        new ImageProvider("dialogs", "downloadincomplete").getResource().attachImageIcon(this, true);
        setHelpId(ht("/Dialog/RelationList#DownloadMembers"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty() || !MainApplication.isDisplayingMapView()) return;
        MainApplication.worker.submit(new DownloadRelationTask(
                Utils.filteredCollection(relations, Relation.class), MainApplication.getLayerManager().getEditLayer()));
    }

    @Override
    public void setPrimitives(Collection<? extends IPrimitive> primitives) {
        // selected non-new relations
        this.relations = SubclassFilteredCollection.filter(getRelations(primitives), r -> !r.isNew());
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(canDownload());
    }
}
