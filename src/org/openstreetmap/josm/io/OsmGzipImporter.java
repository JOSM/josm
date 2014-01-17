// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

public class OsmGzipImporter extends OsmImporter {

    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "osm.gz", "osm.gz", tr("OSM Server Files gzip compressed") + " (*.osm.gz)");

    public OsmGzipImporter() {
        super(FILE_FILTER);
    }

    // compression handling is preformed in super-class

}
