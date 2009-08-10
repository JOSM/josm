// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
public class OsmBzip2Exporter extends OsmExporter {

    public OsmBzip2Exporter() {
        super(new ExtensionFileFilter("osm.bz2, osm.bz", "osm.bz2", tr("OSM Server Files bzip2 compressed")
                + " (*.osm.bz2 *.osm.bz)"));
    }

    @Override
    protected OutputStream getOutputStream(File file) throws FileNotFoundException, IOException {
        OutputStream out = new FileOutputStream(file);
        out.write('B');
        out.write('Z');
        out = new CBZip2OutputStream(out);
        return out;
    }
}
