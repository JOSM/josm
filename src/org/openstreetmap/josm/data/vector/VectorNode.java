// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.vector;

import java.util.List;

import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.UniqueIdGenerator;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.tools.Utils;

/**
 * The "Node" type of a vector layer
 *
 * @since 17862
 */
public class VectorNode extends VectorPrimitive implements INode {
    private static final UniqueIdGenerator ID_GENERATOR = new UniqueIdGenerator();
    private double lon = Double.NaN;
    private double lat = Double.NaN;

    /**
     * Create a new vector node
     * @param layer The layer for the vector node
     */
    public VectorNode(String layer) {
        super(layer);
    }

    @Override
    public double lon() {
        return this.lon;
    }

    @Override
    public double lat() {
        return this.lat;
    }

    @Override
    public UniqueIdGenerator getIdGenerator() {
        return ID_GENERATOR;
    }

    @Override
    public LatLon getCoor() {
        return new LatLon(this.lat, this.lon);
    }

    @Override
    public void setCoor(LatLon coordinates) {
        this.lat = coordinates.lat();
        this.lon = coordinates.lon();
    }

    /**
     * Set the coordinates of this node
     *
     * @param coordinates The coordinates to set
     * @see #setCoor(LatLon)
     */
    public void setCoor(ICoordinate coordinates) {
        this.lat = coordinates.getLat();
        this.lon = coordinates.getLon();
    }

    @Override
    public void setEastNorth(EastNorth eastNorth) {
        final LatLon ll = ProjectionRegistry.getProjection().eastNorth2latlon(eastNorth);
        this.lat = ll.lat();
        this.lon = ll.lon();
    }

    @Override
    public boolean isReferredByWays(int n) {
        // Count only referrers that are members of the same dataset (primitive can have some fake references, for example
        // when way is cloned
        List<? extends IPrimitive> referrers = super.getReferrers();
        if (Utils.isEmpty(referrers))
            return false;
        if (referrers instanceof IPrimitive)
            return n <= 1 && referrers instanceof IWay && ((IPrimitive) referrers).getDataSet() == getDataSet();
        else {
            int counter = 0;
            for (IPrimitive o : referrers) {
                if (getDataSet() == o.getDataSet() && o instanceof IWay && ++counter >= n)
                    return true;
            }
            return false;
        }
    }

    @Override
    public void accept(PrimitiveVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public BBox getBBox() {
        return new BBox(this.lon, this.lat).toImmutable();
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.NODE;
    }
}
