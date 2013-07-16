// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class OsmGzipImporter extends OsmImporter {

    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "osm.gz", "osm.gz", tr("OSM Server Files gzip compressed") + " (*.osm.gz)");

    public OsmGzipImporter() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        importData(getGZipInputStream(new FileInputStream(file)), file, progressMonitor);
    }
}
