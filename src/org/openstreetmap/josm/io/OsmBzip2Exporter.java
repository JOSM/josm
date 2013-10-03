// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.openstreetmap.josm.tools.Utils;

public class OsmBzip2Exporter extends OsmExporter {

    /**
     * Constructs a new {@code OsmBzip2Exporter}.
     */
    public OsmBzip2Exporter() {
        super(OsmBzip2Importer.FILE_FILTER);
    }

    @Override
    protected OutputStream getOutputStream(File file) throws FileNotFoundException, IOException {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write('B');
            out.write('Z');
            return new CBZip2OutputStream(out);
        } catch (IOException e) {
            Utils.close(out);
            throw e;
        }
    }
}
