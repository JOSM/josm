// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter.GpxImporterData;
import org.openstreetmap.josm.io.nmea.NmeaReader;

/**
 * File importer allowing to import NMEA-0183 files (*.nmea/nme/nma/log/txt files).
 * @since 1637
 */
public class NMEAImporter extends GpxLikeImporter<NmeaReader> {

    /**
     * The NMEA file filter (*.nmea *.nme *.nma *.log *.txt files).
     */
    public static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter.newFilterWithArchiveExtensions(
            "nmea,nme,nma,log,txt", "nmea", tr("NMEA-0183 Files"), false);

    /**
     * Constructs a new {@code NMEAImporter}.
     */
    public NMEAImporter() {
        super(FILE_FILTER, NmeaReader.class);
    }

    @Override
    protected void appendInfoboxContent(StringBuilder msg, boolean success, NmeaReader r) {
        msg.append(tr("Malformed sentences: {0}", r.getParserMalformed())).append("<br>")
           .append(tr("Checksum errors: {0}", r.getParserChecksumErrors())).append("<br>");
        if (!success) {
            msg.append(tr("Unknown sentences: {0}", r.getParserUnknown())).append("<br>");
        }
        msg.append(tr("Zero coordinates: {0}", r.getParserZeroCoordinates()));
    }

    /**
     * Replies the new GPX and marker layers corresponding to the specified NMEA file.
     * @param is input stream to NMEA 0183 data
     * @param associatedFile NMEA file
     * @param gpxLayerName The GPX layer name
     * @return the new GPX and marker layers corresponding to the specified NMEA file
     * @throws IOException if an I/O error occurs
     */
    public static GpxImporterData loadLayers(InputStream is, final File associatedFile,
                                             final String gpxLayerName) throws IOException {
        final NmeaReader r = buildAndParse(is, NmeaReader.class);
        final boolean parsedProperly = r.getNumberOfCoordinates() > 0;
        r.getGpxData().storageFile = associatedFile;
        return GpxImporter.loadLayers(r.getGpxData(), parsedProperly, gpxLayerName);
    }
}
