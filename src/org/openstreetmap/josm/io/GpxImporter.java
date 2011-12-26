// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.xml.sax.SAXException;

public class GpxImporter extends FileImporter {

    protected GpxLayer gpxLayer;
    protected MarkerLayer markerLayer;
    protected Runnable postLayerTask;

    public GpxImporter() {
        super(new ExtensionFileFilter("gpx,gpx.gz", "gpx", tr("GPX Files") + " (*.gpx *.gpx.gz)"));
    }

    @Override public void importData(File file, ProgressMonitor progressMonitor) throws IOException {
        InputStream is;
        if (file.getName().endsWith(".gpx.gz")) {
            is = new GZIPInputStream(new FileInputStream(file));
        } else {
            is = new FileInputStream(file);
        }
        String fileName = file.getName();
        loadLayers(is, file, fileName, tr("Markers from {0}", fileName), progressMonitor);

        // FIXME: remove UI stuff from the IO subsystem
        GuiHelper.runInEDT(new Runnable() {
            public void run() {
                if (markerLayer != null) {
                    Main.main.addLayer(markerLayer);
                }
                if (gpxLayer != null) {
                    Main.main.addLayer(gpxLayer);
                }
                postLayerTask.run();
            }
        });
    }

    public void loadLayers(InputStream is, final File associatedFile,
                    final String gpxLayerName, String markerLayerName, ProgressMonitor progressMonitor) throws IOException {
        try {
            final GpxReader r = new GpxReader(is);
            final boolean parsedProperly = r.parse(true);
            r.data.storageFile = associatedFile;
            if (r.data.hasRoutePoints() || r.data.hasTrackPoints()) {
                gpxLayer = new GpxLayer(r.data, gpxLayerName, associatedFile != null);
            }
            if (Main.pref.getBoolean("marker.makeautomarkers", true) && !r.data.waypoints.isEmpty()) {
                markerLayer = new MarkerLayer(r.data, markerLayerName, associatedFile, gpxLayer, false);
                if (markerLayer.data.size() == 0) {
                    markerLayer = null;
                }
            }
            postLayerTask = new Runnable() {
                @Override
                public void run() {
                    if (markerLayer != null) {
                        markerLayer.addMouseHandler();
                    }
                    if (!parsedProperly) {
                        String msg;
                        if (associatedFile == null) {
                            msg = tr("Error occurred while parsing gpx data for layer ''{0}''. Only a part of the file will be available.",
                                    gpxLayerName);
                        } else {
                            msg = tr("Error occurred while parsing gpx file ''{0}''. Only a part of the file will be available.",
                                    associatedFile.getPath());
                        }
                        JOptionPane.showMessageDialog(null, msg);
                    }
                }
            };
        } catch (SAXException e) {
            e.printStackTrace();
            throw new IOException(tr("Parsing data for layer ''{0}'' failed", gpxLayerName));
        }
    }

    public GpxLayer getGpxLayer() {
        return gpxLayer;
    }

    public MarkerLayer getMarkerLayer() {
        return markerLayer;
    }

    public Runnable getPostLayerTask() {
        return postLayerTask;
    }
}
