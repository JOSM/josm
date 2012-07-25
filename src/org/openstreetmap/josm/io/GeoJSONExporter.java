// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import static org.openstreetmap.josm.tools.I18n.tr;

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
            FileWriter out = new FileWriter(file);
            out.write(json);
            out.close();
        } else {
            throw new IllegalArgumentException(tr("Layer ''{0}'' not supported", layer.getClass().toString()));
        }
    }
}
