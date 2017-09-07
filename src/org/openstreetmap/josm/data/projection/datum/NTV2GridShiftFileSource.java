// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.datum;

import java.io.InputStream;

/**
 * Source of NTV2 grid shift files (local directory, download, etc.).
 * @since 12777
 */
public interface NTV2GridShiftFileSource {

    /**
     * Locate grid file with given name.
     * @param gridFileName the name of the grid file
     * @return an input stream for the file data
     */
    InputStream getNTV2GridShiftFile(String gridFileName);

}
