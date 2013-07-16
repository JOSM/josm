// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class OsmBzip2Importer extends OsmImporter {

    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "osm.bz2,osm.bz", "osm.bz2", tr("OSM Server Files bzip2 compressed") + " (*.osm.bz2 *.osm.bz)");

    public OsmBzip2Importer() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        importData(getBZip2InputStream(new FileInputStream(file)), file, progressMonitor);
    }
}
