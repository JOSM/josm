// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;

public class NodeData extends PrimitiveData implements INode {

    private final CachedLatLon coor = new CachedLatLon(0, 0);

    public NodeData() {

    }

    public NodeData(NodeData data) {
        super(data);
        setCoor(data.getCoor());
    }

    @Override
    public LatLon getCoor() {
        return coor;
    }

    @Override
    public void setCoor(LatLon coor) {
        this.coor.setCoor(coor);
    }

    @Override
    public EastNorth getEastNorth() {
        return this.coor.getEastNorth();
    }

    @Override
    public void setEastNorth(EastNorth eastNorth) {
        this.coor.setEastNorth(eastNorth);
    }

    @Override
    public NodeData makeCopy() {
        return new NodeData(this);
    }

    @Override
    public String toString() {
        return super.toString() + " NODE " + coor;
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }
    
    @Override 
    public void visit(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

}
