// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
public class OsmGzipExporter extends OsmExporter {

    public OsmGzipExporter() {
        super(OsmGzipImporter.FILE_FILTER);
    }

    @Override
    protected OutputStream getOutputStream(File file) throws FileNotFoundException, IOException {
        OutputStream out = new FileOutputStream(file);
        return new GZIPOutputStream(out);
    }
}
