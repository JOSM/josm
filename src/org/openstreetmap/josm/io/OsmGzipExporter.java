// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
public class OsmGzipExporter extends OsmExporter {

    public OsmGzipExporter() {
        super(new ExtensionFileFilter("osm.gz", "osm.gz", tr("OSM Server Files gzip compressed") + " (*.osm.gz)"));
    }

    @Override
    protected OutputStream getOutputStream(File file) throws FileNotFoundException, IOException {
        OutputStream out = new FileOutputStream(file);
        return new GZIPOutputStream(out);
    }
}
