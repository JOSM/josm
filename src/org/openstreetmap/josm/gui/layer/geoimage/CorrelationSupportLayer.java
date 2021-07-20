// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxDataContainer;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.data.osm.UploadPolicy;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.gpx.ConvertFromGpxLayerAction;

/**
 * A support layer meant to be modified by the user to provide real-time images correlation.
 * @since 18078
 */
public final class CorrelationSupportLayer extends OsmDataLayer implements GpxDataContainer {

    private static final String CORRELATION_PREFIX = "correlation:";

    private final GpxData gpxData = new GpxData();

    /**
     * Constructs a new {@code CorrelationSupportLayer} with a default name
     * @param fauxGpxData Faux GPX data to be modified using OSM editing tools
     */
    public CorrelationSupportLayer(GpxData fauxGpxData) {
        this(fauxGpxData, tr("Correlation support layer"));
    }

    /**
     * Constructs a new {@code CorrelationSupportLayer} with a given name
     * @param fauxGpxData Faux GPX data to be modified using OSM editing tools
     * @param name layer name
     */
    public CorrelationSupportLayer(GpxData fauxGpxData, String name) {
        super(ConvertFromGpxLayerAction.convert(fauxGpxData, "list", CORRELATION_PREFIX), name, null);
        data.setDownloadPolicy(DownloadPolicy.BLOCKED);
        data.setUploadPolicy(UploadPolicy.BLOCKED);
        rebuildGpxData();
    }

    private void rebuildGpxData() {
        gpxData.beginUpdate();
        try {
            gpxData.clear();
            fillGpxData(gpxData, data, null, CORRELATION_PREFIX);
        } finally {
            gpxData.endUpdate();
        }
    }

    @Override
    public GpxData getGpxData() {
        return gpxData;
    }

    @Override
    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        invalidate();
        rebuildGpxData();
    }

    @Override
    protected void setRequiresSaveToFile(boolean newValue) {
        // Do nothing
    }

    @Override
    protected void setRequiresUploadToServer(boolean newValue) {
        // Do nothing
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public boolean isDownloadable() {
        return false;
    }

    @Override
    public boolean isUploadable() {
        return false;
    }

    @Override
    public boolean requiresUploadToServer() {
        return false;
    }

    @Override
    public boolean requiresSaveToFile() {
        return false;
    }

    @Override
    public boolean isSavable() {
        return false;
    }
}
