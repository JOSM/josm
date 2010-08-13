// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * One node data, consisting of one world coordinate waypoint.
 *
 * @author imi
 */
public final class Node extends OsmPrimitive {

    private CachedLatLon coor;

    public final void setCoor(LatLon coor) {
        if(coor != null){
            updateCoor(coor, null);
        }
    }

    public final void setEastNorth(EastNorth eastNorth) {
        if(eastNorth != null) {
            updateCoor(null, eastNorth);
        }
    }

    private void updateCoor(LatLon coor, EastNorth eastNorth) {
        if (getDataSet() != null) {
            boolean locked = writeLock();
            try {
                getDataSet().fireNodeMoved(this, coor, eastNorth);
            } finally {
                writeUnlock(locked);
            }
        } else {
            setCoorInternal(coor, eastNorth);
        }
    }

    public final LatLon getCoor() {
        return coor;
    }

    public final EastNorth getEastNorth() {
        return coor != null ? coor.getEastNorth() : null;
    }

    /**
     * To be used only by Dataset.reindexNode
     */
    protected void setCoorInternal(LatLon coor, EastNorth eastNorth) {
        if(this.coor == null) {
            if (eastNorth == null) {
                this.coor = new CachedLatLon(coor);
            } else {
                this.coor = new CachedLatLon(eastNorth);
            }
        } else {
            if (eastNorth == null) {
                this.coor.setCoor(coor);
            } else {
                this.coor.setEastNorth(eastNorth);
            }
        }
    }

    protected Node(long id, boolean allowNegative) {
        super(id, allowNegative);
    }

    /**
     * Create a new local node.
     *
     */
    public Node() {
        this(0, false);
    }

    /**
     * Create an incomplete Node object
     */
    public Node(long id) {
        super(id, false);
    }

    /**
     * Create new node
     * @param id
     * @param version
     */
    public Node(long id, int version) {
        super(id, version, false);
    }

    /**
     *
     * @param clone
     * @param clearId If true, set version to 0 and id to new unique value
     */
    public Node(Node clone, boolean clearId) {
        super(clone.getUniqueId(), true /* allow negative IDs */);
        cloneFrom(clone);
        if (clearId) {
            clearOsmId();
        }
    }

    /**
     * Create an identical clone of the argument (including the id)
     */
    public Node(Node clone) {
        this(clone, false);
    }

    public Node(LatLon latlon) {
        super(0, false);
        setCoor(latlon);
    }

    public Node(EastNorth eastNorth) {
        super(0, false);
        setEastNorth(eastNorth);
    }

    @Override
    void setDataset(DataSet dataSet) {
        super.setDataset(dataSet);
        if (!isIncomplete() && (getCoor() == null || getEastNorth() == null))
            throw new DataIntegrityProblemException("Complete node with null coordinates: " + toString() + get3892DebugInfo());
    }

    @Override public void visit(Visitor visitor) {
        visitor.visit(this);
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        boolean locked = writeLock();
        try {
            super.cloneFrom(osm);
            setCoor(((Node)osm).coor);
        } finally {
            writeUnlock(locked);
        }
    }

    /**
     * Merges the technical and semantical attributes from <code>other</code> onto this.
     *
     * Both this and other must be new, or both must be assigned an OSM ID. If both this and <code>other</code>
     * have an assigend OSM id, the IDs have to be the same.
     *
     * @param other the other primitive. Must not be null.
     * @throws IllegalArgumentException thrown if other is null.
     * @throws DataIntegrityProblemException thrown if either this is new and other is not, or other is new and this is not
     * @throws DataIntegrityProblemException thrown if other is new and other.getId() != this.getId()
     */
    @Override
    public void mergeFrom(OsmPrimitive other) {
        boolean locked = writeLock();
        try {
            super.mergeFrom(other);
            if (!other.isIncomplete()) {
                setCoor(new LatLon(((Node)other).coor));
            }
        } finally {
            writeUnlock(locked);
        }
    }

    @Override public void load(PrimitiveData data) {
        boolean locked = writeLock();
        try {
            super.load(data);
            setCoor(((NodeData)data).getCoor());
        } finally {
            writeUnlock(locked);
        }
    }

    @Override public NodeData save() {
        NodeData data = new NodeData();
        saveCommonAttributes(data);
        if (!isIncomplete()) {
            data.setCoor(getCoor());
        }
        return data;
    }

    @Override public String toString() {
        String coorDesc = coor == null?"":"lat="+coor.lat()+",lon="+coor.lon();
        return "{Node id=" + getUniqueId() + " version=" + getVersion() + " " + getFlagsAsString() + " "  + coorDesc+"}";
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (other == null || ! (other instanceof Node) )
            return false;
        if (! super.hasEqualSemanticAttributes(other))
            return false;
        Node n = (Node)other;
        if (coor == null && n.coor == null)
            return true;
        else if (coor != null && n.coor != null)
            return coor.equalsEpsilon(n.coor);
        else
            return false;
    }

    public int compareTo(OsmPrimitive o) {
        return o instanceof Node ? Long.valueOf(getUniqueId()).compareTo(o.getUniqueId()) : 1;
    }

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }

    @Override
    public BBox getBBox() {
        return new BBox(this);
    }

    @Override
    public void updatePosition() {
        // TODO: replace CachedLatLon with simple doubles and update precalculated EastNorth value here
    }

    public boolean isConnectionNode() {
        return (OsmPrimitive.getFilteredList(getReferrers(), Way.class)).size() > 1;
    }

    public String get3892DebugInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("Unexpected error. Please report it to http://josm.openstreetmap.de/ticket/3892\n");
        builder.append(toString());
        builder.append("\n");
        if (coor == null) {
            builder.append("Coor is null\n");
        } else {
            builder.append(String.format("EastNorth: %s\n", coor.getEastNorth()));
            builder.append(coor.getProjection());
            builder.append("\n");
        }

        return builder.toString();
    }
}
