// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.data.projection.Projections;

/**
 * One node data, consisting of one world coordinate waypoint.
 *
 * @author imi
 */
public final class Node extends OsmPrimitive implements INode {

    /*
     * We "inline" lat/lon rather than using a LatLon-object => reduces memory footprint
     */
    static private final double COORDINATE_NOT_DEFINED = Double.NaN;
    private double lat = COORDINATE_NOT_DEFINED;
    private double lon = COORDINATE_NOT_DEFINED;

    /*
     * the cached projected coordinates
     */
    private double east = Double.NaN;
    private double north = Double.NaN;

    private boolean isLatLonKnown() {
        return lat != COORDINATE_NOT_DEFINED && lon != COORDINATE_NOT_DEFINED;
    }

    @Override
    public final void setCoor(LatLon coor) {
        if(coor != null){
            updateCoor(coor, null);
        }
    }

    @Override
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

    @Override
    public final LatLon getCoor() {
        if (!isLatLonKnown()) return null;
        return new LatLon(lat,lon);
    }

    /**
     * <p>Replies the projected east/north coordinates.</p>
     * 
     * <p>Uses the {@link Main#getProjection() global projection} to project the lan/lon-coordinates.
     * Internally caches the projected coordinates.</p>
     *
     * <p><strong>Caveat:</strong> doesn't listen to projection changes. Clients must
     * {@link #invalidateEastNorthCache() invalidate the internal cache}.</p>
     * 
     * <p>Replies {@code null} if this node doesn't know lat/lon-coordinates, i.e. because it is an incomplete node.
     * 
     * @return the east north coordinates or {@code null}
     * @see #invalidateEastNorthCache()
     * 
     */
    @Override
    public final EastNorth getEastNorth() {
        if (!isLatLonKnown()) return null;
        
        if (getDataSet() == null)
            // there is no dataset that listens for projection changes
            // and invalidates the cache, so we don't use the cache at all
            return Projections.project(new LatLon(lat, lon));
        
        if (Double.isNaN(east) || Double.isNaN(north)) {
            // projected coordinates haven't been calculated yet,
            // so fill the cache of the projected node coordinates
            EastNorth en = Projections.project(new LatLon(lat, lon));
            this.east = en.east();
            this.north = en.north();
        }
        return new EastNorth(east, north);
    }

    /**
     * To be used only by Dataset.reindexNode
     */
    protected void setCoorInternal(LatLon coor, EastNorth eastNorth) {
        if (coor != null) {
            this.lat = coor.lat();
            this.lon = coor.lon();
            invalidateEastNorthCache();
        } else if (eastNorth != null) {
            LatLon ll = Projections.inverseProject(eastNorth);
            this.lat = ll.lat();
            this.lon = ll.lon();
            this.east = eastNorth.east();
            this.north = eastNorth.north();
        } else
            throw new IllegalArgumentException();
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

    @Override public void visit(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        boolean locked = writeLock();
        try {
            super.cloneFrom(osm);
            setCoor(((Node)osm).getCoor());
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
                setCoor(((Node)other).getCoor());
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
        String coorDesc = isLatLonKnown() ? "lat="+lat+",lon="+lon : "";
        return "{Node id=" + getUniqueId() + " version=" + getVersion() + " " + getFlagsAsString() + " "  + coorDesc+"}";
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (other == null || ! (other instanceof Node) )
            return false;
        if (! super.hasEqualSemanticAttributes(other))
            return false;
        Node n = (Node)other;
        LatLon coor = getCoor();
        LatLon otherCoor = n.getCoor();
        if (coor == null && otherCoor == null)
            return true;
        else if (coor != null && otherCoor != null)
            return coor.equalsEpsilon(otherCoor);
        else
            return false;
    }

    @Override
    public int compareTo(OsmPrimitive o) {
        return o instanceof Node ? Long.valueOf(getUniqueId()).compareTo(o.getUniqueId()) : 1;
    }

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }

    @Override
    public BBox getBBox() {
        return new BBox(this);
    }

    @Override
    public void updatePosition() {
    }

    public boolean isConnectionNode() {
        return isReferredByWays(2);
    }

    public String get3892DebugInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("Unexpected error. Please report it to http://josm.openstreetmap.de/ticket/3892\n");
        builder.append(toString());
        builder.append("\n");
        if (isLatLonKnown()) {
            builder.append("Coor is null\n");
        } else {
            builder.append(String.format("EastNorth: %s\n", getEastNorth()));
            builder.append(Main.getProjection());
            builder.append("\n");
        }

        return builder.toString();
    }

    /**
     * Invoke to invalidate the internal cache of projected east/north coordinates.
     * Coordinates are reprojected on demand when the {@link #getEastNorth()} is invoked
     * next time.
     */
    public void invalidateEastNorthCache() {
        this.east = Double.NaN;
        this.north = Double.NaN;
    }
}
