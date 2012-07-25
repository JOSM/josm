// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.bzip2.CBZip2OutputStream;
public class OsmBzip2Exporter extends OsmExporter {

    public OsmBzip2Exporter() {
        super(OsmBzip2Importer.FILE_FILTER);
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
