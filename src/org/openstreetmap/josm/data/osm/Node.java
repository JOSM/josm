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
     * Create an identical clone of the argument (including the id)
     */
    public Node(Node clone) {
        super(clone.getUniqueId(), true);
        cloneFrom(clone);
    }

    public Node(LatLon latlon) {
        super(0, false);
        setCoor(latlon);
    }

    public Node(EastNorth eastNorth) {
        super(0, false);
        setEastNorth(eastNorth);
    }

    public Node(NodeData data, DataSet dataSet) {
        super(data);
        load(data, dataSet);
    }

    @Override public void visit(Visitor visitor) {
        visitor.visit(this);
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        super.cloneFrom(osm);
        setCoor(((Node)osm).coor);
    }

    @Override public void load(PrimitiveData data, DataSet dataSet) {
        super.load(data, dataSet);
        setCoor(((NodeData)data).getCoor());
    }

    @Override public NodeData save() {
        NodeData data = new NodeData();
        saveCommonAttributes(data);
        data.setCoor(getCoor());
        return data;
    }

    @Override public String toString() {
        if (coor == null) return "{Node id="+getId()+"}";
        return "{Node id="+getId()+",version="+getVersion()+",lat="+coor.lat()+",lon="+coor.lon()+"}";
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
            return coor.equals(n.coor);
        else
            return false;
    }

    public int compareTo(OsmPrimitive o) {
        return o instanceof Node ? Long.valueOf(getId()).compareTo(o.getId()) : 1;
    }

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }
}
