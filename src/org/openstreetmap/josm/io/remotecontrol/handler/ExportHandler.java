// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Export map data into .osm format
 * For instance, {@code /export]}.
 * @since 19425
 */
public class ExportHandler extends RequestHandler {

    /**
     * The remote control command name used to export data from JOSM.
     */
    public static final String command = "export";

    @Override
    public String[] getMandatoryParams() {
        return new String[]{};
    }

    @Override
    public String[] getOptionalParams() {
        return new String[] {};
    }

    @Override
    public String getUsage() {
        return "export data from JOSM";
    }

    @Override
    public String[] getUsageExamples() {
        return new String[] {"/export"};
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        Layer layer = MainApplication.getLayerManager().getActiveLayer();
        if (!(layer instanceof OsmDataLayer)) {
            content = "";
            return;
        }
        OsmDataLayer osmLayer = (OsmDataLayer) layer;
        StringWriter sw = new StringWriter();
        OsmWriter w = OsmWriterFactory.createOsmWriter(new PrintWriter(sw), false, osmLayer.data.getVersion());
        osmLayer.data.getReadLock().lock();
        try {
            w.write(osmLayer.data);
        } finally {
            osmLayer.data.getReadLock().unlock();
        }
        contentType = "application/xml";
        content = sw.toString();
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to export data from JOSM");
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return PermissionPrefWithDefault.EXPORT_DATA;
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {}
}
