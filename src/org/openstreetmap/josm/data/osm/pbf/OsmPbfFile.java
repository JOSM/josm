// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.pbf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A class used to determine whether or not a file may be an OSM PBF file
 * @since xxx
 */
public final class OsmPbfFile {
    /**
     * Extensions for OSM PBF files.
     * {@code "osm.pbf"} is a SHOULD, <i>not</i> a MUST.
     */
    public static final List<String> EXTENSION = Collections.unmodifiableList(Arrays.asList("osm.pbf", "pbf"));

    /**
     * mimetypes for OSM PBF files
     */
    public static final List<String> MIMETYPE = Collections.emptyList();

    private OsmPbfFile() {
        // Hide the constructor
    }
}
