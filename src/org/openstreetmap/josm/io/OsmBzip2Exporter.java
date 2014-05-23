// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * OSM data exporter that compresses it in Bzip2 format.
 */
public class OsmBzip2Exporter extends OsmExporter {

    /**
     * Constructs a new {@code OsmBzip2Exporter}.
     */
    public OsmBzip2Exporter() {
        super(OsmBzip2Importer.FILE_FILTER);
    }

    // compression handling is performed in super-class

}
