// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public class NMEAImporter extends FileImporter {

    public NMEAImporter() {
        super(
                new ExtensionFileFilter("nmea,nme,nma,log,txt", "nmea", tr("NMEA-0183 Files")
                        + " (*.nmea *.nme *.nma *.log *.txt)"));
    }

    @Override public void importData(File file, ProgressMonitor progressMonitor) throws IOException {
        String fn = file.getName();
        NmeaReader r = new NmeaReader(new FileInputStream(file), file.getAbsoluteFile().getParentFile());
        if (r.getNumberOfCoordinates() > 0) {
            r.data.storageFile = file;
            GpxLayer gpxLayer = new GpxLayer(r.data, fn, true);
            Main.main.addLayer(gpxLayer);
            if (Main.pref.getBoolean("marker.makeautomarkers", true)) {
                MarkerLayer ml = new MarkerLayer(r.data, tr("Markers from {0}", fn), file, gpxLayer);
                if (ml.data.size() > 0) {
                    Main.main.addLayer(ml);
                }
            }
        }
        showNmeaInfobox(r.getNumberOfCoordinates() > 0, r);
    }

    private void showNmeaInfobox(boolean success, NmeaReader r) {
        final StringBuilder msg = new StringBuilder().append("<html>");
        msg.append(tr("Coordinates imported: {0}", r.getNumberOfCoordinates()) + "<br>");
        msg.append(tr("Malformed sentences: {0}", r.getParserMalformed()) + "<br>");
        msg.append(tr("Checksum errors: {0}", r.getParserChecksumErrors()) + "<br>");
        if (!success) {
            msg.append(tr("Unknown sentences: {0}", r.getParserUnknown()) + "<br>");
        }
        msg.append(tr("Zero coordinates: {0}", r.getParserZeroCoordinates()));
        msg.append("</html>");
        if (success) {
            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
                    msg.toString(),
                    tr("NMEA import success"),
                    JOptionPane.INFORMATION_MESSAGE, null);
        } else {
            HelpAwareOptionPane.showMessageDialogInEDT(
                    Main.parent,
                    msg.toString(),
                    tr("NMEA import failure!"),
                    JOptionPane.ERROR_MESSAGE, null);
        }
    }
}
