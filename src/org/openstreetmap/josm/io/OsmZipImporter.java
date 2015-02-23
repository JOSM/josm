// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

/**
 * OSM data importer that uncompresses it from Zip format.
 * @since 6882
 */
public class OsmZipImporter extends OsmImporter {

    /**
     * File filter used to load/save Zip compressed OSM files.
     */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "osm.zip", "osm.zip", tr("OSM Server Files zip compressed") + " (*.osm.zip)");

    /**
     * Constructs a new {@code OsmZipImporter}.
     */
    public OsmZipImporter() {
        super(FILE_FILTER);
    }

    // compression handling is performed in super-class
}
