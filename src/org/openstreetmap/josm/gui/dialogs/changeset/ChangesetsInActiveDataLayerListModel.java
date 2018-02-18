// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;

/**
 * This is the list model for the list of changeset in the current edit layer.
 *
 */
public class ChangesetsInActiveDataLayerListModel extends ChangesetListModel implements DataSetListener, ActiveLayerChangeListener {

    /**
     * Creates a new {@link ChangesetsInActiveDataLayerListModel}
     * @param selectionModel The selection model for the list
     */
    public ChangesetsInActiveDataLayerListModel(DefaultListSelectionModel selectionModel) {
        super(selectionModel);
    }

    /* ------------------------------------------------------------------------------ */
    /* interface DataSetListener                                                      */
    /* ------------------------------------------------------------------------------ */
    @Override
    public void dataChanged(DataChangedEvent event) {
        initFromDataSet(event.getDataset());
    }

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        // ignored
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        // ignored
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        // ignored
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        // ignored
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        // ignored
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        // ignored
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        // ignored
    }

    /* ------------------------------------------------------------------------------ */
    /* interface ActiveLayerChangeListener                                                    */
    /* ------------------------------------------------------------------------------ */
    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        // just init the model content. Don't register as DataSetListener. The mode
        // is already registered to receive DataChangedEvents from the current edit layer
        DataSet ds = e.getSource().getActiveDataSet();
        if (ds != null) {
            initFromDataSet(ds);
        } else {
            initFromDataSet(null);
        }
    }
}
