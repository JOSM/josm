// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.GeoJSONWriter;

/**
 * Exporter to write map data to a GeoJSON file.
 * @since 4886
 */
public class GeoJSONExporter extends FileExporter {

    /** File extension filter for .geojson files */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "geojson,json", "geojson", tr("GeoJSON Files") + " (*.geojson *.json)");

    /**
     * Constructs a new {@code GeoJSONExporter} with WGS84 projection.
     */
    public GeoJSONExporter() {
        super(FILE_FILTER);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        if (layer instanceof OsmDataLayer) {
            try (Writer out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                out.write(new GeoJSONWriter((OsmDataLayer) layer).write());
            }
        } else {
            throw new IllegalArgumentException(tr("Layer ''{0}'' not supported", layer.getClass().toString()));
        }
    }
}
