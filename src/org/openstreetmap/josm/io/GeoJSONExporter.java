// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Utils;

public class GeoJSONExporter extends FileExporter {

    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "json,geojson", "json", tr("GeoJSON Files") + " (*.json *.geojson)");

    public GeoJSONExporter() {
        super(FILE_FILTER);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        if (layer instanceof OsmDataLayer) {
            String json = new GeoJSONWriter((OsmDataLayer) layer).write();
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Utils.UTF_8));
            try {
                out.write(json);
            } finally {
                Utils.close(out);
            }
        } else {
            throw new IllegalArgumentException(tr("Layer ''{0}'' not supported", layer.getClass().toString()));
        }
    }
}
