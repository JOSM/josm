// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter.GpxImporterData;
import org.openstreetmap.josm.io.ozi.OziWptReader;

/**
 * File importer allowing to import OziExplorer Waypoint files (*.wpt files).
 * @since 18179
 */
public class OziWptImporter extends GpxLikeImporter<OziWptReader> {

    /**
     * The OziExplorer Waypoint file filter (*.wpt files).
     */
    public static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter.newFilterWithArchiveExtensions(
            "wpt", "wpt", tr("OziExplorer Waypoint Files"), false);

    /**
     * Constructs a new {@code OziWptImporter}.
     */
    public OziWptImporter() {
        super(FILE_FILTER, OziWptReader.class);
    }

    /**
     * Replies the new GPX and marker layers corresponding to the specified wpt file.
     * @param is input stream to data
     * @param associatedFile wpt file
     * @param gpxLayerName The GPX layer name
     * @return the new GPX and marker layers corresponding to the specified wpt file
     * @throws IOException if an I/O error occurs
     */
    public static GpxImporterData loadLayers(InputStream is, final File associatedFile,
                                             final String gpxLayerName) throws IOException {
        final OziWptReader r = buildAndParse(is, OziWptReader.class);
        final boolean parsedProperly = r.getNumberOfCoordinates() > 0;
        r.getGpxData().storageFile = associatedFile;
        return GpxImporter.loadLayers(r.getGpxData(), parsedProperly, gpxLayerName);
    }
}
