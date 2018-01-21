// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

/**
 * OSM data exporter that compresses it in XZ format.
 * @since 13352
 */
public class OsmXzExporter extends OsmExporter {

    /**
     * Constructs a new {@code OsmXzExporter}.
     */
    public OsmXzExporter() {
        super(new ExtensionFileFilter(
            "osm.xz", "osm.xz", tr("OSM Server Files XZ compressed") + " (*.osm.xz)"));
    }

    // compression handling is performed in super-class

}
