// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

public class OsmGzipExporter extends OsmExporter {

    public OsmGzipExporter() {
        super(OsmGzipImporter.FILE_FILTER);
    }

    // compression handling is preformed in super-class

}
