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
import org.xml.sax.SAXException;

public class GpxImporter extends FileImporter {

    public GpxImporter() {
        super(new ExtensionFileFilter("gpx,gpx.gz", "gpx", tr("GPX Files") + " (*.gpx *.gpx.gz)"));
    }

    @Override public void importData(final File file, ProgressMonitor progressMonitor) throws IOException {
        final String fn = file.getName();

        try {
            InputStream is;
            if (file.getName().endsWith(".gpx.gz")) {
                is = new GZIPInputStream(new FileInputStream(file));
            } else {
                is = new FileInputStream(file);
            }
            final GpxReader r = new GpxReader(is);
            final boolean parsedProperly = r.parse(true);
            r.data.storageFile = file;
            final GpxLayer gpxLayer = new GpxLayer(r.data, fn, true);

            // FIXME: remove UI stuff from the IO subsystem
            //
            Runnable task = new Runnable() {
                public void run() {
                    if (r.data.hasRoutePoints() || r.data.hasTrackPoints()) {
                        Main.main.addLayer(gpxLayer);
                    }
                    if (Main.pref.getBoolean("marker.makeautomarkers", true) && !r.data.waypoints.isEmpty()) {
                        MarkerLayer ml = new MarkerLayer(r.data, tr("Markers from {0}", fn), file, gpxLayer);
                        if (ml.data.size() > 0) {
                            Main.main.addLayer(ml);
                        }
                    }
                    if (!parsedProperly) {
                        JOptionPane.showMessageDialog(null, tr("Error occured while parsing gpx file {0}. Only part of the file will be available", file.getName()));
                    }
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeLater(task);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IOException(tr("File \"{0}\" does not exist", file.getName()));
        } catch (SAXException e) {
            e.printStackTrace();
            throw new IOException(tr("Parsing file \"{0}\" failed", file.getName()));
        }
    }
}
