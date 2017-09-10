// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.awt.geom.Area;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.projection.Projecting;
import org.openstreetmap.josm.tools.CheckParameterUtil;
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
    /**
     * The cache key to use for {@link #east} and {@link #north}.
     */
    private Object eastNorthCacheKey;

    /**
     * Determines if this node has valid coordinates.
     * @return {@code true} if this node has valid coordinates
     * @since 7828
     */
    @Override
    public boolean isLatLonKnown() {
        // We cannot use default implementation - if we remove this implementation, we will break binary compatibility.
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }

    @Override
    public void setCoor(LatLon coor) {
        updateCoor(coor, null);
    }

    @Override
    public void setEastNorth(EastNorth eastNorth) {
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

    /**
     * Returns lat/lon coordinates of this node, or {@code null} unless {@link #isLatLonKnown()}
     * @return lat/lon coordinates of this node, or {@code null} unless {@link #isLatLonKnown()}
     */
    @Override
    public LatLon getCoor() {
        if (!isLatLonKnown()) {
            return null;
        } else {
            return new LatLon(lat, lon);
        }
    }

    @Override
    public double lat() {
        return lat;
    }

    @Override
    public double lon() {
        return lon;
    }

    /**
     * Replies the projected east/north coordinates.
     * <p>
     * Uses the {@link Main#getProjection() global projection} to project the lat/lon-coordinates.
     * <p>
     * Method {@link org.openstreetmap.josm.data.coor.ILatLon#getEastNorth()} of
     * implemented interface <code>ILatLon</code> is deprecated, but this method is not.
     * @return the east north coordinates or {@code null} if {@link #isLatLonKnown()}
     * is false.
     */
    @SuppressWarnings("deprecation")
    @Override
    public EastNorth getEastNorth() {
        return getEastNorth(Main.getProjection());
    }

    @Override
    public EastNorth getEastNorth(Projecting projection) {
        if (!isLatLonKnown()) return null;

        if (Double.isNaN(east) || Double.isNaN(north) || !Objects.equals(projection.getCacheKey(), eastNorthCacheKey)) {
            // projected coordinates haven't been calculated yet,
            // so fill the cache of the projected node coordinates
            EastNorth en = projection.latlon2eastNorth(this);
            this.east = en.east();
            this.north = en.north();
            this.eastNorthCacheKey = projection.getCacheKey();
        }
        return new EastNorth(east, north);
    }

    /**
     * To be used only by Dataset.reindexNode
     * @param coor lat/lon
     * @param eastNorth east/north
     */
    void setCoorInternal(LatLon coor, EastNorth eastNorth) {
        if (coor != null) {
            this.lat = coor.lat();
            this.lon = coor.lon();
            invalidateEastNorthCache();
        } else if (eastNorth != null) {
            LatLon ll = Main.getProjection().eastNorth2latlon(eastNorth);
            this.lat = ll.lat();
            this.lon = ll.lon();
            this.east = eastNorth.east();
            this.north = eastNorth.north();
            this.eastNorthCacheKey = Main.getProjection().getCacheKey();
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
    public Node(long id) {
        super(id, false);
    }

    /**
     * Constructs a new {@code Node} with the given id and version.
     * @param id The id. Must be &gt;= 0
     * @param version The version
     * @throws IllegalArgumentException if id &lt; 0
     */
    public Node(long id, int version) {
        super(id, version, false);
    }

    /**
     * Constructs an identical clone of the argument.
     * @param clone The node to clone
     * @param clearMetadata If {@code true}, clears the OSM id and other metadata as defined by {@link #clearOsmMetadata}.
     * If {@code false}, does nothing
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
        if (!isIncomplete() && isVisible() && !isLatLonKnown())
            throw new DataIntegrityProblemException("Complete node with null coordinates: " + toString());
    }

    /**
     * @deprecated no longer supported
     */
    @Override
    @Deprecated
    public void accept(org.openstreetmap.josm.data.osm.visitor.Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(OsmPrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void cloneFrom(OsmPrimitive osm) {
        if (!(osm instanceof Node))
            throw new IllegalArgumentException("Not a node: " + osm);
        boolean locked = writeLock();
        try {
            super.cloneFrom(osm);
            setCoor(((Node) osm).getCoor());
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
     * @throws IllegalArgumentException if other is null.
     * @throws DataIntegrityProblemException if either this is new and other is not, or other is new and this is not
     * @throws DataIntegrityProblemException if other is new and other.getId() != this.getId()
     */
    @Override
    public void mergeFrom(OsmPrimitive other) {
        if (!(other instanceof Node))
            throw new IllegalArgumentException("Not a node: " + other);
        boolean locked = writeLock();
        try {
            super.mergeFrom(other);
            if (!other.isIncomplete()) {
                setCoor(((Node) other).getCoor());
            }
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public void load(PrimitiveData data) {
        if (!(data instanceof NodeData))
            throw new IllegalArgumentException("Not a node data: " + data);
        boolean locked = writeLock();
        try {
            super.load(data);
            setCoor(((NodeData) data).getCoor());
        } finally {
            writeUnlock(locked);
        }
    }

    @Override
    public NodeData save() {
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
        return "{Node id=" + getUniqueId() + " version=" + getVersion() + ' ' + getFlagsAsString() + ' ' + coorDesc+'}';
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other, boolean testInterestingTagsOnly) {
        return (other instanceof Node)
                && hasEqualSemanticFlags(other)
                && hasEqualCoordinates((Node) other)
                && super.hasEqualSemanticAttributes(other, testInterestingTagsOnly);
    }

    private boolean hasEqualCoordinates(Node other) {
        final LatLon c1 = getCoor();
        final LatLon c2 = other.getCoor();
        return (c1 == null && c2 == null) || (c1 != null && c2 != null && c1.equalsEpsilon(c2));
    }

    @Override
    public int compareTo(OsmPrimitive o) {
        return o instanceof Node ? Long.compare(getUniqueId(), o.getUniqueId()) : 1;
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
        return new BBox(lon, lat);
    }

    @Override
    protected void addToBBox(BBox box, Set<PrimitiveId> visited) {
        box.add(lon, lat);
    }

    @Override
    public void updatePosition() {
        // Do nothing
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
        this.eastNorthCacheKey = null;
    }

    @Override
    public boolean concernsArea() {
        // A node cannot be an area
        return false;
    }

    /**
     * Tests whether {@code this} node is connected to {@code otherNode} via at most {@code hops} nodes
     * matching the {@code predicate} (which may be {@code null} to consider all nodes).
     * @param otherNodes other nodes
     * @param hops number of hops
     * @param predicate predicate to match
     * @return {@code true} if {@code this} node mets the conditions
     */
    public boolean isConnectedTo(final Collection<Node> otherNodes, final int hops, Predicate<Node> predicate) {
        CheckParameterUtil.ensureParameterNotNull(otherNodes);
        CheckParameterUtil.ensureThat(!otherNodes.isEmpty(), "otherNodes must not be empty!");
        CheckParameterUtil.ensureThat(hops >= 0, "hops must be non-negative!");
        return hops == 0
                ? isConnectedTo(otherNodes, hops, predicate, null)
                : isConnectedTo(otherNodes, hops, predicate, new TreeSet<>());
    }

    private boolean isConnectedTo(final Collection<Node> otherNodes, final int hops, Predicate<Node> predicate, Set<Node> visited) {
        if (otherNodes.contains(this)) {
            return true;
        }
        if (hops > 0 && visited != null) {
            visited.add(this);
            for (final Way w : Utils.filteredCollection(this.getReferrers(), Way.class)) {
                for (final Node n : w.getNodes()) {
                    final boolean containsN = visited.contains(n);
                    visited.add(n);
                    if (!containsN && (predicate == null || predicate.test(n))
                            && n.isConnectedTo(otherNodes, hops - 1, predicate, visited)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isOutsideDownloadArea() {
        if (isNewOrUndeleted() || getDataSet() == null)
            return false;
        Area area = getDataSet().getDataSourceArea();
        if (area == null)
            return false;
        LatLon coor = getCoor();
        return coor != null && !coor.isIn(area);
    }

    /**
     * Replies the set of referring ways.
     * @return the set of referring ways
     * @since 12031
     */
    public List<Way> getParentWays() {
        return getFilteredList(getReferrers(), Way.class);
    }
}
