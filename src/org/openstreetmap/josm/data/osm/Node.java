// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.LatLon.CoordinateFormat;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.data.osm.Node;

/**
 * One node data, consisting of one world coordinate waypoint.
 *
 * @author imi
 */
public final class Node extends OsmPrimitive {

    private CachedLatLon coor;

    public final void setCoor(LatLon coor) {
        if(coor != null)
        {
            if(this.coor == null)
                this.coor = new CachedLatLon(coor);
            else
                this.coor.setCoor(coor);
        }
    }

    public final LatLon getCoor() {
        return coor;
    }

    public final void setEastNorth(EastNorth eastNorth) {
        if(eastNorth != null)
        {
            if(coor != null)
                coor.setEastNorth(eastNorth);
            else
                coor = new CachedLatLon(eastNorth);
        }
    }

    public final EastNorth getEastNorth() {
        return coor != null ? coor.getEastNorth() : null;
    }

    private static CoordinateFormat mCord;

    static public CoordinateFormat getCoordinateFormat()
    {
        return mCord;
    }

    static public void setCoordinateFormat()
    {
        try {
            mCord = LatLon.CoordinateFormat.valueOf(Main.pref.get("coordinates"));
        } catch (IllegalArgumentException iae) {
            mCord = LatLon.CoordinateFormat.DECIMAL_DEGREES;
        }
    }

    static {
        setCoordinateFormat();
    }

    /**
     * Create an incomplete Node object
     */
    public Node(long id) {
        this.id = id;
        incomplete = true;
    }

    /**
     * Create an identical clone of the argument (including the id)
     */
    public Node(Node clone) {
        cloneFrom(clone);
    }

    public Node(LatLon latlon) {
        setCoor(latlon);
    }

    public Node(EastNorth eastNorth) {
        setEastNorth(eastNorth);
    }

    @Override public void visit(Visitor visitor) {
        visitor.visit(this);
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        super.cloneFrom(osm);
        setCoor(((Node)osm).coor);
    }

    @Override public String toString() {
        if (coor == null) return "{Node id="+id+"}";
        return "{Node id="+id+",version="+version+",lat="+coor.lat()+",lon="+coor.lon()+"}";
    }

    /**
     * @deprecated
     * @see #hasEqualSemanticAttributes(OsmPrimitive)
     * @see #hasEqualTechnicalAttributes(OsmPrimitive)
     */
    @Deprecated
    @Override public boolean realEqual(OsmPrimitive osm, boolean semanticOnly) {
        if (osm instanceof Node) {
            if (super.realEqual(osm, semanticOnly)) {
                if ((coor == null) && ((Node)osm).coor == null)
                    return true;
                if (coor != null)
                    return coor.equals(((Node)osm).coor);
            }
        }
        return false;
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
        return o instanceof Node ? Long.valueOf(id).compareTo(o.id) : 1;
    }

    @Override
    public String getName() {
        String name;
        if (incomplete) {
            name = tr("incomplete");
        } else {
            name = get("name");
            if (name == null) {
                name = id == 0 ? tr("node") : ""+id;
            }
            name += " (" + coor.latToString(mCord) + ", " + coor.lonToString(mCord) + ")";
        }
        return name;
    }

}
