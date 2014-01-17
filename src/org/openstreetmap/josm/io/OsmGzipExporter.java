// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * OSM data exporter that compresses it in GZip format.
 */
public class OsmGzipExporter extends OsmExporter {

    /**
     * Constructs a new {@code OsmGzipExporter}.
     */
    public OsmGzipExporter() {
        super(OsmGzipImporter.FILE_FILTER);
    }

    // compression handling is performed in super-class

}
