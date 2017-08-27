// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

/**
 * OSM data exporter that compresses it in Bzip2 format.
 */
public class OsmBzip2Exporter extends OsmExporter {

    /**
     * Constructs a new {@code OsmBzip2Exporter}.
     */
    public OsmBzip2Exporter() {
        super(new ExtensionFileFilter(
            "osm.bz2,osm.bz", "osm.bz2", tr("OSM Server Files bzip2 compressed") + " (*.osm.bz2, *.osm.bz)"));
    }

    // compression handling is performed in super-class

}
