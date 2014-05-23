// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

/**
 * OSM data importer that uncompresses it from Gzip format.
 */
public class OsmGzipImporter extends OsmImporter {

    /**
     * File filter used to load/save Gzip compressed OSM files.
     */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "osm.gz", "osm.gz", tr("OSM Server Files gzip compressed") + " (*.osm.gz)");

    /**
     * Constructs a new {@code OsmGzipImporter}.
     */
    public OsmGzipImporter() {
        super(FILE_FILTER);
    }

    // compression handling is performed in super-class

}
