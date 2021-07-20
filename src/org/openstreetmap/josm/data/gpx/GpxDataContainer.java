// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

/**
 * Object containing a {@link GpxData} instance.
 * @since 18078
 */
public interface GpxDataContainer {

    /**
     * Returns the GPX data.
     * @return the GPX data
     */
    GpxData getGpxData();
}
