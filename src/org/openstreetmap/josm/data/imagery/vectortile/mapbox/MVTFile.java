// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Items that MAY be used to figure out if a file or server response MAY BE a Mapbox Vector Tile
 * @author Taylor Smock
 * @since 17862
 */
public final class MVTFile {
    /**
     * Extensions for Mapbox Vector Tiles.
     * {@code mvt} is a SHOULD, <i>not</i> a MUST.
     */
    public static final List<String> EXTENSION = Collections.unmodifiableList(Arrays.asList("mvt", "pbf"));

    /**
     * mimetypes for Mapbox Vector Tiles
     * This {@code application/vnd.mapbox-vector-tile}is a SHOULD, <i>not</i> a MUST.
     */
    public static final List<String> MIMETYPE = Collections.unmodifiableList(Arrays.asList("application/vnd.mapbox-vector-tile",
            "application/x-protobuf"));

    /**
     * The default projection. This is Web Mercator, per specification.
     */
    public static final String DEFAULT_PROJECTION = "EPSG:3857";

    private MVTFile() {
        // Hide the constructor
    }
}
