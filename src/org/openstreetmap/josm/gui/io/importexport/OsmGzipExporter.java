// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

/**
 * OSM data exporter that compresses it in GZip format.
 */
public class OsmGzipExporter extends OsmExporter {

    /**
     * Constructs a new {@code OsmGzipExporter}.
     */
    public OsmGzipExporter() {
        super(new ExtensionFileFilter(
            "osm.gz", "osm.gz", tr("OSM Server Files gzip compressed") + " (*.osm.gz)"));
    }

    // compression handling is performed in super-class

}
