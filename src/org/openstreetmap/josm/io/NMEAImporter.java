// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.GpxImporter.GpxImporterData;

/**
 * File importer allowing to import NMEA-0183 files (*.nmea/nme/nma/log/txt files).
 * @since 1637
 */
public class NMEAImporter extends FileImporter {

    /**
     * The NMEA file filter (*.nmea *.nme *.nma *.log *.txt files).
     */
    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "nmea,nme,nma,log,txt", "nmea", tr("NMEA-0183 Files") + " (*.nmea *.nme *.nma *.log *.txt)");

    /**
     * Constructs a new {@code NMEAImporter}.
     */
    public NMEAImporter() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException {
        final String fn = file.getName();
        try (InputStream fis = new FileInputStream(file)) {
            final NmeaReader r = new NmeaReader(fis);
            if (r.getNumberOfCoordinates() > 0) {
                r.data.storageFile = file;
                final GpxLayer gpxLayer = new GpxLayer(r.data, fn, true);
                final File fileFinal = file;

                GuiHelper.runInEDT(new Runnable() {
                    @Override
                    public void run() {
                        Main.main.addLayer(gpxLayer);
                        if (Main.pref.getBoolean("marker.makeautomarkers", true)) {
                            MarkerLayer ml = new MarkerLayer(r.data, tr("Markers from {0}", fn), fileFinal, gpxLayer);
                            if (!ml.data.isEmpty()) {
                                Main.main.addLayer(ml);
                            }
                        }
                    }
                });
            }
            showNmeaInfobox(r.getNumberOfCoordinates() > 0, r);
        }
    }

    private void showNmeaInfobox(boolean success, NmeaReader r) {
        final StringBuilder msg = new StringBuilder(160).append("<html>");
        msg.append(tr("Coordinates imported: {0}", r.getNumberOfCoordinates())).append("<br>")
           .append(tr("Malformed sentences: {0}", r.getParserMalformed())).append("<br>")
           .append(tr("Checksum errors: {0}", r.getParserChecksumErrors())).append("<br>");
        if (!success) {
            msg.append(tr("Unknown sentences: {0}", r.getParserUnknown())).append("<br>");
        }
        msg.append(tr("Zero coordinates: {0}", r.getParserZeroCoordinates()))
           .append("</html>");
        if (success) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new Notification(
                            "<h3>" + tr("NMEA import success:") + "</h3>" + msg.toString())
                            .setIcon(JOptionPane.INFORMATION_MESSAGE)
                            .show();
                }
            });
        } else {
            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
                    msg.toString(),
                    tr("NMEA import failure!"),
                    JOptionPane.ERROR_MESSAGE, null);
        }
    }

    public static GpxImporterData loadLayers(InputStream is, final File associatedFile,
            final String gpxLayerName, String markerLayerName) throws IOException {
        final NmeaReader r = new NmeaReader(is);
        final boolean parsedProperly = r.getNumberOfCoordinates() > 0;
        r.data.storageFile = associatedFile;
        return GpxImporter.loadLayers(r.data, parsedProperly, gpxLayerName, markerLayerName);
    }
}
