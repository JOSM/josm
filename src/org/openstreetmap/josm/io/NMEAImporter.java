// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;

public class NMEAImporter extends FileImporter {

    public NMEAImporter() {
        super(
                new ExtensionFileFilter("nmea,nme,nma,txt", "nmea", tr("NMEA-0183 Files")
                        + " (*.nmea *.nme *.nma *.txt)"));
    }

    @Override public void importData(File file) throws IOException {
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
        String msg = tr("Coordinates imported: ") + r.getNumberOfCoordinates() + "\n" + tr("Malformed sentences: ")
        + r.getParserMalformed() + "\n" + tr("Checksum errors: ") + r.getParserChecksumErrors() + "\n";
        if (!success) {
            msg += tr("Unknown sentences: ") + r.getParserUnknown() + "\n";
        }
        msg += tr("Zero coordinates: ") + r.getParserZeroCoordinates();
        if (success) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    msg,
                    tr("NMEA import success"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(Main.parent, msg, tr("NMEA import failure!"), JOptionPane.ERROR_MESSAGE);
        }
    }

}
