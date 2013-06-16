// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.projection.Projections;

public class NodeData extends PrimitiveData implements INode {

    /*
     * we "inline" lat/lon coordinates instead of using a LatLon => reduces memory footprint
     */
    private double lat = Double.NaN;
    private double lon = Double.NaN;

    public NodeData() {}

    public NodeData(NodeData data) {
        super(data);
        setCoor(data.getCoor());
    }

    private boolean isLatLonKnown() {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }

    @Override
    public LatLon getCoor() {
        return isLatLonKnown() ? new LatLon(lat,lon) : null;
    }

    @Override
    public void setCoor(LatLon coor) {
        if (coor == null) {
            this.lat = Double.NaN;
            this.lon = Double.NaN;
        } else {
            this.lat = coor.lat();
            this.lon = coor.lon();
        }
    }

    @Override
    public EastNorth getEastNorth() {
        /*
         * No internal caching of projected coordinates needed. In contrast to getEastNorth()
         * on Node, this method is rarely used. Caching would be overkill.
         */
        return Projections.project(getCoor());
    }

    @Override
    public void setEastNorth(EastNorth eastNorth) {
        LatLon ll = Projections.inverseProject(eastNorth);
        setCoor(ll);
    }

    @Override
    public NodeData makeCopy() {
        return new NodeData(this);
    }

    @Override
    public String toString() {
        return super.toString() + " NODE " + getCoor();
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }

    @Override
    public void accept(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

}
