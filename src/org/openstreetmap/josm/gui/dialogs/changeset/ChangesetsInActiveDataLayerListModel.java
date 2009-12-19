// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.ChangesetIdChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;

public class ChangesetsInActiveDataLayerListModel extends ChangesetListModel implements DataSetListener  {

    public ChangesetsInActiveDataLayerListModel(DefaultListSelectionModel selectionModel) {
        super(selectionModel);
    }

    public void dataChanged(DataChangedEvent event) {
        initFromPrimitives(event.getPrimitives());
    }

    public void nodeMoved(NodeMovedEvent event) {/* ignored */}

    public void primtivesAdded(PrimitivesAddedEvent event) {
        for (OsmPrimitive primitive:event.getPrimitives()) {
            addChangeset(new Changeset(primitive.getChangesetId()));
        }
    }

    public void primtivesRemoved(PrimitivesRemovedEvent event) {
        for (OsmPrimitive primitive:event.getPrimitives()) {
            removeChangeset(new Changeset(primitive.getChangesetId()));
        }
    }

    public void relationMembersChanged(RelationMembersChangedEvent event) {/* ignored */}

    public void tagsChanged(TagsChangedEvent event) {/* ignored */}

    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        if (event instanceof ChangesetIdChangedEvent) {
            ChangesetIdChangedEvent e = (ChangesetIdChangedEvent) event;
            removeChangeset(new Changeset(e.getOldChangesetId()));
            addChangeset(new Changeset(e.getNewChangesetId()));
        }
    }

    public void wayNodesChanged(WayNodesChangedEvent event) {/* ignored */}

}
