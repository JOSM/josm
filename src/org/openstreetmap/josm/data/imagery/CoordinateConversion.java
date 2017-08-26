// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.Projected;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.IProjected;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Allows easy conversion between JMapViewer coordinate types and JOSM coordinate types.
 * @since 12669
 */
public final class CoordinateConversion {

    private CoordinateConversion() {
        // Hide default constructor for utility classes
    }

    /**
     * Converts an {@link EastNorth} to an {@link IProjected} instance.
     * @param en east/north coordinate
     * @return {@code IProjected} instance
     */
    public static IProjected enToProj(EastNorth en) {
        return new Projected(en.east(), en.north());
    }

    /**
     * Converts an {@link IProjected} to an {@link EastNorth} instance.
     * @param p projected coordinate
     * @return {@code EastNorth} instance
     */
    public static EastNorth projToEn(IProjected p) {
        return new EastNorth(p.getEast(), p.getNorth());
    }

    /**
     * Converts a {@link LatLon} to an {@link ICoordinate} instance.
     * @param ll latitude/longitude coordinate
     * @return {@code ICoordinate} instance
     */
    public static ICoordinate llToCoor(LatLon ll) {
        return new Coordinate(ll.lat(), ll.lon());
    }

    /**
     * Converts an {@link ICoordinate} to a {@link LatLon} instance.
     * @param c coordinate
     * @return {@code LatLon} instance
     */
    public static LatLon coorToLL(ICoordinate c) {
        return new LatLon(c.getLat(), c.getLon());
    }
}
