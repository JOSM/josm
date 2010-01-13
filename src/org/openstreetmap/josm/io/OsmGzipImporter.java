// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class OsmGzipImporter extends OsmImporter {

    public OsmGzipImporter() {
        super(new ExtensionFileFilter("osm.gz", "osm.gz", tr("OSM Server Files gzip compressed") + " (*.osm.gz)"));
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        GZIPInputStream in = new GZIPInputStream(new FileInputStream(file));
        importData(in, file);
    }
}
