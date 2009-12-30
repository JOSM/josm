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
import org.openstreetmap.josm.gui.MapView.EditLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This is the list model for the list of changeset in the current edit layer.
 *
 */
public class ChangesetsInActiveDataLayerListModel extends ChangesetListModel implements DataSetListener, EditLayerChangeListener {

    public ChangesetsInActiveDataLayerListModel(DefaultListSelectionModel selectionModel) {
        super(selectionModel);
    }

    /* ------------------------------------------------------------------------------ */
    /* interface DataSetListener                                                      */
    /* ------------------------------------------------------------------------------ */
    public void dataChanged(DataChangedEvent event) {
        initFromDataSet(event.getDataset());
    }

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

    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        if (event instanceof ChangesetIdChangedEvent) {
            ChangesetIdChangedEvent e = (ChangesetIdChangedEvent) event;
            removeChangeset(new Changeset(e.getOldChangesetId()));
            addChangeset(new Changeset(e.getNewChangesetId()));
        }
    }

    public void nodeMoved(NodeMovedEvent event) {/* ignored */}

    public void relationMembersChanged(RelationMembersChangedEvent event) {/* ignored */}

    public void tagsChanged(TagsChangedEvent event) {/* ignored */}

    public void wayNodesChanged(WayNodesChangedEvent event) {/* ignored */}

    /* ------------------------------------------------------------------------------ */
    /* interface EditLayerListener                                                    */
    /* ------------------------------------------------------------------------------ */
    public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
        // just init the model content. Don't register as DataSetListener. The mode
        // is already registered to receive DataChangedEvents from the current
        // edit layer
        if (newLayer != null) {
            initFromDataSet(newLayer.data);
        } else {
            initFromDataSet(null);
        }
    }
}
