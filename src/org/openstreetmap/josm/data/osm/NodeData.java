// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;

/**
 * The data on a single node (tags and position) that is stored in the database
 */
public class NodeData extends PrimitiveData implements INode {

    private static final long serialVersionUID = 5626323599550908773L;
    /*
     * we "inline" lat/lon coordinates instead of using a LatLon => reduces memory footprint
     */
    private double lat = Double.NaN;
    private double lon = Double.NaN;

    /**
     * Constructs a new {@code NodeData}.
     */
    public NodeData() {
        // contents can be set later with setters
    }

    /**
     * Constructs a new {@code NodeData} with given id.
     * @param id id
     * @since 12017
     */
    public NodeData(long id) {
        super(id);
    }

    /**
     * Constructs a new {@code NodeData}.
     * @param data node data to copy
     */
    public NodeData(NodeData data) {
        super(data);
        setCoor(data.getCoor());
    }

    @Override
    public double lat() {
        return lat;
    }

    @Override
    public double lon() {
        return lon;
    }

    @Override
    public boolean isLatLonKnown() {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }

    @Override
    public LatLon getCoor() {
        return isLatLonKnown() ? new LatLon(lat, lon) : null;
    }

    @Override
    public final void setCoor(LatLon coor) {
        if (coor == null) {
            this.lat = Double.NaN;
            this.lon = Double.NaN;
        } else {
            this.lat = coor.lat();
            this.lon = coor.lon();
        }
    }

    @Override
    public void setEastNorth(EastNorth eastNorth) {
        setCoor(ProjectionRegistry.getProjection().eastNorth2latlon(eastNorth));
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

    @Override
    public BBox getBBox() {
        return new BBox(lon, lat);
    }

    @Override
    public boolean isReferredByWays(int n) {
        return false;
    }
}
