// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.GpxRouteLayer;
import org.openstreetmap.josm.gui.layer.ImageryLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * File importer allowing to import GPX files (*.gpx/gpx.gz files).
 *
 */
public class GpxImporter extends FileImporter {

    /**
     * Utility class containing imported GPX and marker layers, and a task to run after they are added to MapView.
     */
    public static class GpxImporterData {
        /**
         * The imported GPX layer. May be null if no GPX data.
         */
        private final GpxLayer gpxLayer;
        /**
         * The imported GPX route layer. May be null if no marker.
         */
        private final GpxRouteLayer gpxRouteLayer;
        /**
         * The imported marker layer. May be null if no marker.
         */
        private final MarkerLayer markerLayer;
        /**
         * The task to run after GPX and/or marker layer has been added to MapView.
         */
        private final Runnable postLayerTask;

        /**
         * Constructs a new {@code GpxImporterData}.
         * @param gpxLayer The imported GPX layer. May be null if no GPX data.
         * @param markerLayer The imported marker layer. May be null if no marker.
         * @param postLayerTask The task to run after GPX and/or marker layer has been added to MapView.
         */
        public GpxImporterData(GpxLayer gpxLayer, GpxRouteLayer gpxRouteLayer, MarkerLayer markerLayer, Runnable postLayerTask) {
            this.gpxLayer = gpxLayer;
            this.gpxRouteLayer = gpxRouteLayer;
            this.markerLayer = markerLayer;
            this.postLayerTask = postLayerTask;
        }

        /**
         * Returns the imported GPX layer. May be null if no GPX data.
         * @return the imported GPX layer. May be null if no GPX data.
         */
        public GpxLayer getGpxLayer() {
            return gpxLayer;
        }

        /**
         * Returns the imported GPX route layer. May be null if no GPX data.
         * @return the imported GPX route layer. May be null if no GPX data.
         */
        public GpxRouteLayer getGpxRouteLayer() {
            return gpxRouteLayer;
        }

        /**
         * Returns the imported marker layer. May be null if no marker.
         * @return the imported marker layer. May be null if no marker.
         */
        public MarkerLayer getMarkerLayer() {
            return markerLayer;
        }

        /**
         * Returns the task to run after GPX and/or marker layer has been added to MapView.
         * @return the task to run after GPX and/or marker layer has been added to MapView.
         */
        public Runnable getPostLayerTask() {
            return postLayerTask;
        }
    }

    /**
     * Constructs a new {@code GpxImporter}.
     */
    public GpxImporter() {
        super(getFileFilter());
    }

    /**
     * Returns a GPX file filter (*.gpx and *.gpx.gz files).
     * @return a GPX file filter
     */
    public static ExtensionFileFilter getFileFilter() {
        return ExtensionFileFilter.newFilterWithArchiveExtensions("gpx",
                Config.getPref().get("save.extension.gpx", "gpx"), tr("GPX Files"), true);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException {
        final String fileName = file.getName();

        try (InputStream is = Compression.getUncompressedFileInputStream(file)) {
            GpxReader r = new GpxReader(is);
            boolean parsedProperly = r.parse(true);
            r.getGpxData().storageFile = file;
            addLayers(loadLayers(r.getGpxData(), parsedProperly, fileName));
        } catch (SAXException e) {
            Logging.error(e);
            throw new IOException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Adds the specified GPX and marker layers to Map.main
     * @param data The layers to add
     * @see #loadLayers
     */
    public static void addLayers(final GpxImporterData data) {
        // FIXME: remove UI stuff from the IO subsystem
        GuiHelper.runInEDT(() -> {
            if (data.markerLayer != null) {
                MainApplication.getLayerManager().addLayer(data.markerLayer);
            }
            if (data.gpxRouteLayer != null) {
                MainApplication.getLayerManager().addLayer(data.gpxRouteLayer);
            }
            if (data.gpxLayer != null) {
                MainApplication.getLayerManager().addLayer(data.gpxLayer);
            }
            data.postLayerTask.run();
        });
    }

    /**
     * Replies the new GPX and marker layers corresponding to the specified GPX data.
     * @param data The GPX data
     * @param parsedProperly True if GPX data has been properly parsed by {@link GpxReader#parse}
     * @param gpxLayerName The GPX layer name
     * @return the new GPX and marker layers corresponding to the specified GPX data, to be used with {@link #addLayers}
     * @see #addLayers
     */
    public static GpxImporterData loadLayers(final GpxData data, final boolean parsedProperly, final String gpxLayerName) {
        MarkerLayer markerLayer = null;
        GpxRouteLayer gpxRouteLayer = null;
        GpxLayer gpxLayer = new GpxLayer(data, gpxLayerName, data.storageFile != null);
        if (Config.getPref().getBoolean("marker.makeautomarkers", true) && !data.waypoints.isEmpty()) {
            markerLayer = new MarkerLayer(data, tr("Markers from {0}", gpxLayerName), data.storageFile, gpxLayer);
            if (markerLayer.data.isEmpty()) {
                markerLayer = null;
            } else {
                gpxLayer.setLinkedMarkerLayer(markerLayer);
            }
        }
        if (Config.getPref().getBoolean("gpx.makeautoroutes", true)) {
            gpxRouteLayer = new GpxRouteLayer(tr("Routes from {0}", gpxLayerName), gpxLayer);
        }

        final boolean isSameColor = MainApplication.getLayerManager()
                .getLayersOfType(ImageryLayer.class)
                .stream().noneMatch(ImageryLayer::isVisible)
                && data.getTracks().stream().anyMatch(t -> OsmDataLayer.getBackgroundColor().equals(t.getColor()));

        Runnable postLayerTask = () -> {
            if (!parsedProperly) {
                String msg;
                if (data.storageFile == null) {
                    msg = tr("Error occurred while parsing gpx data for layer ''{0}''. Only a part of the file will be available.",
                            gpxLayerName);
                } else {
                    msg = tr("Error occurred while parsing gpx file ''{0}''. Only a part of the file will be available.",
                            data.storageFile.getPath());
                }
                JOptionPane.showMessageDialog(null, msg);
            }
            if (isSameColor) {
                new Notification(tr("The imported track \"{0}\" might not be visible because it has the same color as the background." +
                        "<br>You can change this in the context menu of the imported layer.", gpxLayerName))
                .setIcon(JOptionPane.WARNING_MESSAGE)
                .setDuration(Notification.TIME_LONG)
                .show();
            }
        };
        return new GpxImporterData(gpxLayer, gpxRouteLayer, markerLayer, postLayerTask);
    }

    /**
     * Replies the new GPX and marker layers corresponding to the specified GPX file.
     * @param is input stream to GPX data
     * @param associatedFile GPX file
     * @param gpxLayerName The GPX layer name
     * @param progressMonitor The progress monitor
     * @return the new GPX and marker layers corresponding to the specified GPX file
     * @throws IOException if an I/O error occurs
     */
    public static GpxImporterData loadLayers(InputStream is, final File associatedFile,
                                             final String gpxLayerName, ProgressMonitor progressMonitor) throws IOException {
        try {
            final GpxReader r = new GpxReader(is);
            final boolean parsedProperly = r.parse(true);
            r.getGpxData().storageFile = associatedFile;
            return loadLayers(r.getGpxData(), parsedProperly, gpxLayerName);
        } catch (SAXException e) {
            Logging.error(e);
            throw new IOException(tr("Parsing data for layer ''{0}'' failed", gpxLayerName), e);
        }
    }
}
