// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.xml.sax.SAXException;

public class GpxImporter extends FileImporter {

    public GpxImporter() {
        super(new ExtensionFileFilter("gpx,gpx.gz", "gpx", tr("GPX Files") + " (*.gpx *.gpx.gz)"));
    }

    @Override public void importData(File file) throws IOException {
        String fn = file.getName();

        try {
            GpxReader r = null;
            InputStream is;
            if (file.getName().endsWith(".gpx.gz")) {
                is = new GZIPInputStream(new FileInputStream(file));
            } else {
                is = new FileInputStream(file);
            }
            // Workaround for SAX BOM bug
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6206835
            if (!((is.read() == 0xef) && (is.read() == 0xbb) && (is.read() == 0xbf))) {
                is.close();
                if (file.getName().endsWith(".gpx.gz")) {
                    is = new GZIPInputStream(new FileInputStream(file));
                } else {
                    is = new FileInputStream(file);
                }
            }
            r = new GpxReader(is, file.getAbsoluteFile().getParentFile());
            r.data.storageFile = file;
            GpxLayer gpxLayer = new GpxLayer(r.data, fn, true);
            Main.main.addLayer(gpxLayer);
            if (Main.pref.getBoolean("marker.makeautomarkers", true)) {
                MarkerLayer ml = new MarkerLayer(r.data, tr("Markers from {0}", fn), file, gpxLayer);
                if (ml.data.size() > 0) {
                    Main.main.addLayer(ml);
                }
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
