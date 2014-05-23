// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

/**
 * One node data, consisting of one world coordinate waypoint.
 *
 * @author imi
 */
public final class Node extends OsmPrimitive implements INode {

    /*
     * We "inline" lat/lon rather than using a LatLon-object => reduces memory footprint
     */
    private double lat = Double.NaN;
    private double lon = Double.NaN;

    /*
     * the cached projected coordinates
     */
    private double east = Double.NaN;
    private double north = Double.NaN;

    private boolean isLatLonKnown() {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }

    @Override
    public final void setCoor(LatLon coor) {
        updateCoor(coor, null);
    }

    @Override
    public final void setEastNorth(EastNorth eastNorth) {
        updateCoor(null, eastNorth);
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
        } else {
            this.lat = Double.NaN;
            this.lon = Double.NaN;
            invalidateEastNorthCache();
            if (isVisible()) {
                setIncomplete(true);
            }
        }
    }

    protected Node(long id, boolean allowNegative) {
        super(id, allowNegative);
    }

    /**
     * Constructs a new local {@code Node} with id 0.
     */
    public Node() {
        this(0, false);
    }

    /**
     * Constructs an incomplete {@code Node} object with the given id.
     * @param id The id. Must be &gt;= 0
     * @throws IllegalArgumentException if id &lt; 0
     */
    public Node(long id) throws IllegalArgumentException {
        super(id, false);
    }

    /**
     * Constructs a new {@code Node} with the given id and version.
     * @param id The id. Must be &gt;= 0
     * @param version The version
     * @throws IllegalArgumentException if id &lt; 0
     */
    public Node(long id, int version) throws IllegalArgumentException {
        super(id, version, false);
    }

    /**
     * Constructs an identical clone of the argument.
     * @param clone The node to clone
     * @param clearMetadata If {@code true}, clears the OSM id and other metadata as defined by {@link #clearOsmMetadata}. If {@code false}, does nothing
     */
    public Node(Node clone, boolean clearMetadata) {
        super(clone.getUniqueId(), true /* allow negative IDs */);
        cloneFrom(clone);
        if (clearMetadata) {
            clearOsmMetadata();
        }
    }

    /**
     * Constructs an identical clone of the argument (including the id).
     * @param clone The node to clone, including its id
     */
    public Node(Node clone) {
        this(clone, false);
    }

    /**
     * Constructs a new {@code Node} with the given lat/lon with id 0.
     * @param latlon The {@link LatLon} coordinates
     */
    public Node(LatLon latlon) {
        super(0, false);
        setCoor(latlon);
    }

    /**
     * Constructs a new {@code Node} with the given east/north with id 0.
     * @param eastNorth The {@link EastNorth} coordinates
     */
    public Node(EastNorth eastNorth) {
        super(0, false);
        setEastNorth(eastNorth);
    }

    @Override
    void setDataset(DataSet dataSet) {
        super.setDataset(dataSet);
        if (!isIncomplete() && isVisible() && (getCoor() == null || getEastNorth() == null))
            throw new DataIntegrityProblemException("Complete node with null coordinates: " + toString());
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void cloneFrom(OsmPrimitive osm) {
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

    @Override
    public String toString() {
        String coorDesc = isLatLonKnown() ? "lat="+lat+",lon="+lon : "";
        return "{Node id=" + getUniqueId() + " version=" + getVersion() + " " + getFlagsAsString() + " "  + coorDesc+"}";
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (!(other instanceof Node))
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

    @Override
    public boolean isDrawable() {
        // Not possible to draw a node without coordinates.
        return super.isDrawable() && isLatLonKnown();
    }

    /**
     * Check whether this node connects 2 ways.
     *
     * @return true if isReferredByWays(2) returns true
     * @see #isReferredByWays(int)
     */
    public boolean isConnectionNode() {
        return isReferredByWays(2);
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

    @Override
    public boolean concernsArea() {
        // A node cannot be an area
        return false;
    }

    /**
     * Tests whether {@code this} node is connected to {@code otherNode} via at most {@code hops} nodes
     * matching the {@code predicate} (which may be {@code null} to consider all nodes).
     */
    public boolean isConnectedTo(final Collection<Node> otherNodes, final int hops, Predicate<Node> predicate) {
        CheckParameterUtil.ensureParameterNotNull(otherNodes);
        CheckParameterUtil.ensureThat(!otherNodes.isEmpty(), "otherNodes must not be empty!");
        CheckParameterUtil.ensureThat(hops >= 0, "hops must be non-negative!");
        return hops == 0
                ? isConnectedTo(otherNodes, hops, predicate, null)
                : isConnectedTo(otherNodes, hops, predicate, new TreeSet<Node>());
    }

    private boolean isConnectedTo(final Collection<Node> otherNodes, final int hops, Predicate<Node> predicate, Set<Node> visited) {
        if (otherNodes.contains(this)) {
            return true;
        }
        if (hops > 0) {
            visited.add(this);
            for (final Way w : Utils.filteredCollection(this.getReferrers(), Way.class)) {
                for (final Node n : w.getNodes()) {
                    final boolean containsN = visited.contains(n);
                    visited.add(n);
                    if (!containsN && (predicate == null || predicate.evaluate(n)) && n.isConnectedTo(otherNodes, hops - 1, predicate, visited)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isOutsideDownloadArea() {
        return !isNewOrUndeleted() && getDataSet() != null && getDataSet().getDataSourceArea() != null
                && !getCoor().isIn(getDataSet().getDataSourceArea());
    }
}
