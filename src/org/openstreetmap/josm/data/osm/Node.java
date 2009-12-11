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
            if(this.coor == null) {
                this.coor = new CachedLatLon(coor);
            } else {
                this.coor.setCoor(coor);
            }
            if (getDataSet() != null) {
                getDataSet().fireNodeMoved(this);
            }
        }
    }

    public final LatLon getCoor() {
        return coor;
    }

    public final void setEastNorth(EastNorth eastNorth) {
        if(eastNorth != null)
        {
            if(coor != null) {
                coor.setEastNorth(eastNorth);
            } else {
                coor = new CachedLatLon(eastNorth);
            }
            if (getDataSet() != null) {
                getDataSet().fireNodeMoved(this);
            }
        }
    }

    public final EastNorth getEastNorth() {
        return coor != null ? coor.getEastNorth() : null;
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
     *
     * @param clone
     * @param clearId If true, set version to 0 and id to new unique value
     */
    public Node(Node clone, boolean clearId) {
        super(clone.getUniqueId(), true);
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

    @Override public void visit(Visitor visitor) {
        visitor.visit(this);
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        super.cloneFrom(osm);
        setCoor(((Node)osm).coor);
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
        super.mergeFrom(other);
        if (!other.isIncomplete()) {
            setCoor(new LatLon(((Node)other).coor));
        }
    }

    @Override public void load(PrimitiveData data) {
        super.load(data);
        setCoor(((NodeData)data).getCoor());
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
        if (coor == null)
            return new BBox(0, 0, 0, 0);
        else
            return new BBox(coor, coor);
    }

    @Override
    public void updatePosition() {
        // Do nothing for now, but in future replace CachedLatLon with simple doubles and update precalculated EastNorth value here
    }
}
