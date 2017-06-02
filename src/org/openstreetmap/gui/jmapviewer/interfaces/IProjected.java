// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer.interfaces;

/**
 * Projected coordinates (east / north space).
 *
 * For most projections, one unit in projected space is roughly one meter, but
 * can also be degrees or feet.
 */
public interface IProjected {
    /**
     * Returns easting.
     * @return easting
     */
    double getEast();

    /**
     * Returns northing.
     * @return northing
     */
    double getNorth();
}
