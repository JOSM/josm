// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.xml.sax.SAXException;

public class OsmBzip2Importer extends OsmImporter {

    public OsmBzip2Importer() {
        super(new ExtensionFileFilter("osm.bz2,osm.bz", "osm.bz2", tr("OSM Server Files bzip2 compressed")
                + " (*.osm.bz2 *.osm.bz)"));
    }

    @Override
    public void importData(File file) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        int b = bis.read();
        if (b != 'B') {
            throw new IOException(tr("Invalid bz2 file."));
        }
        b = bis.read();
        if (b != 'Z') {
            throw new IOException(tr("Invalid bz2 file."));
        }
        CBZip2InputStream in = new CBZip2InputStream(bis);

        try {
            importData(in, file);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new IOException(tr("Could not read \"{0}\"", file.getName()));
        }
    }
}
