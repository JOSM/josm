// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

/**
 * OSM data importer that uncompresses it from Bzip2 format.
 */
public class OsmBzip2Importer extends OsmImporter {

    /**
     * File filter used to load/save Bzip2 compressed OSM files.
     */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "osm.bz2,osm.bz", "osm.bz2", tr("OSM Server Files bzip2 compressed") + " (*.osm.bz2 *.osm.bz)");

    /**
     * Constructs a new {@code OsmBzip2Importer}.
     */
    public OsmBzip2Importer() {
        super(FILE_FILTER);
    }

    // compression handling is performed in super-class

}
