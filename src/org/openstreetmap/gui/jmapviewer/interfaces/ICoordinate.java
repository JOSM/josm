// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.interfaces;

/**
 * Latitude/Longitude coordinates.
 */
public interface ICoordinate {

    /**
     * Returns latitude.
     * @return latitude in degrees
     */
    double getLat();

    /**
     * Sets latitude.
     * @param lat latitude in degrees
     */
    void setLat(double lat);

    /**
     * Returns longitude.
     * @return longitude in degrees
     */
    double getLon();

    /**
     * Sets longitude.
     * @param lon longitude in degrees
     */
    void setLon(double lon);
}
