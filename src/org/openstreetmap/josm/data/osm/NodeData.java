// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class NodeData extends PrimitiveData {

    private final CachedLatLon coor = new CachedLatLon(0, 0);

    public NodeData() {

    }

    public NodeData(double lat, double lon, String... keys) {
        setCoor(new LatLon(lat, lon));
        setKeysAsList(keys);
    }

    public NodeData(String... keys) {
        setKeysAsList(keys);
    }

    public NodeData(NodeData data) {
        super(data);
        setCoor(data.getCoor());
    }

    public LatLon getCoor() {
        return coor;
    }

    public void setCoor(LatLon coor) {
        this.coor.setCoor(coor);
    }

    public EastNorth getEastNorth() {
        return this.coor.getEastNorth();
    }

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

    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }

}
