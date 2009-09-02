// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Represents a collection of {@see OsmPrimitive}s which should be uploaded to the
 * API.
 * The collection is derived from the modified primitives of an {@see DataSet}.
 * 
 * FIXME: use to optimize the upload order before uploading, see various tickets in trac
 * 
 */
public class APIDataSet {
    private LinkedList<OsmPrimitive> toAdd;
    private LinkedList<OsmPrimitive> toUpdate;
    private LinkedList<OsmPrimitive> toDelete;

    /**
     * creates a new empty data set
     */
    public APIDataSet() {
        toAdd = new LinkedList<OsmPrimitive>();
        toUpdate = new LinkedList<OsmPrimitive>();
        toDelete = new LinkedList<OsmPrimitive>();
    }

    /**
     * initializes the API data set with the modified primitives in <code>ds</ds>
     * 
     * @param ds the data set. Ignore, if null.
     */
    public void init(DataSet ds) {
        if (ds == null) return;
        toAdd.clear();
        toUpdate.clear();
        toDelete.clear();
        if (ds == null)
            return;
        for (OsmPrimitive osm :ds.allPrimitives()) {
            if (osm.get("josm/ignore") != null) {
                continue;
            }
            if (osm.getId() == 0 && !osm.isDeleted()) {
                toAdd.addLast(osm);
            } else if (osm.isModified() && !osm.isDeleted()) {
                toUpdate.addLast(osm);
            } else if (osm.isDeleted() && osm.getId() != 0) {
                toDelete.addFirst(osm);
            }
        }
    }

    /**
     * initializes the API data set with the modified primitives in <code>ds</ds>
     * 
     * @param ds the data set. Ignore, if null.
     */
    public APIDataSet(DataSet ds) {
        this();
        init(ds);
    }

    /**
     * Replies true if there are no primitives to upload
     * 
     * @return true if there are no primitives to upload
     */
    public boolean isEmpty() {
        return toAdd.isEmpty() && toUpdate.isEmpty() && toDelete.isEmpty();
    }

    /**
     * Replies the primitives which should be added to the OSM database
     * 
     * @return the primitives which should be added to the OSM database
     */
    public List<OsmPrimitive> getPrimitivesToAdd() {
        return toAdd;
    }

    /**
     * Replies the primitives which should be updated in the OSM database
     * 
     * @return the primitives which should be updated in the OSM database
     */
    public List<OsmPrimitive> getPrimitivesToUpdate() {
        return toUpdate;
    }

    /**
     * Replies the primitives which should be deleted in the OSM database
     * 
     * @return the primitives which should be deleted in the OSM database
     */
    public List<OsmPrimitive> getPrimitivesToDelete() {
        return toDelete;
    }

    /**
     * Replies all primitives
     * 
     * @return all primitives
     */
    public List<OsmPrimitive> getPrimitives() {
        LinkedList<OsmPrimitive> ret = new LinkedList<OsmPrimitive>();
        ret.addAll(toAdd);
        ret.addAll(toUpdate);
        ret.addAll(toDelete);
        return ret;
    }
}
