// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.tools.Utils;

public class GeoJSONExporter extends FileExporter {

    protected final Projection projection;
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "geojson,json", "geojson", tr("GeoJSON Files") + " (*.geojson *.json)");
    public static final ExtensionFileFilter FILE_FILTER_PROJECTED = new ExtensionFileFilter(
            "proj.geojson", "proj.geojson", tr("Projected GeoJSON Files") + " (*.proj.geojson)");

    /**
     * A GeoJSON exporter which obtains the current map projection when exporting ({@link #exportData(File, Layer)}).
     */
    public static class CurrentProjection extends GeoJSONExporter {
        public CurrentProjection() {
            super(FILE_FILTER_PROJECTED, null);
        }
    }

    /**
     * Constructs a new {@code GeoJSONExporter} with WGS84 projection.
     */
    public GeoJSONExporter() {
        this(FILE_FILTER, ProjectionPreference.wgs84.getProjection());
    }

    private GeoJSONExporter(ExtensionFileFilter fileFilter, Projection projection) {
        super(fileFilter);
        this.projection = projection;
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        if (layer instanceof OsmDataLayer) {
            String json = new GeoJSONWriter((OsmDataLayer) layer, Utils.firstNonNull(projection, Main.getProjection())).write();
            try (Writer out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                out.write(json);
            }
        } else {
            throw new IllegalArgumentException(tr("Layer ''{0}'' not supported", layer.getClass().toString()));
        }
    }
}
