// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter;
import org.openstreetmap.josm.gui.io.importexport.NMEAImporter;
import org.openstreetmap.josm.gui.io.importexport.RtkLibImporter;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.IGpxReader;
import org.openstreetmap.josm.io.nmea.NmeaReader;
import org.openstreetmap.josm.io.rtklib.RtkLibPosReader;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Helper class to select and load "GPX data" (GPX, NMEA, pos) files.
 * @since 18042
 */
public final class GpxDataHelper {

    /**
     * Opens a file chooser to let the user select a "GPX data" file and returns it.
     * @return the file chosen by the user, or {@code null}
     */
    public static File chooseGpxDataFile() {
        ExtensionFileFilter gpxFilter = GpxImporter.getFileFilter();
        AbstractFileChooser fc = new FileChooserManager(true, null).createFileChooser(false, null,
                Arrays.asList(gpxFilter, NMEAImporter.FILE_FILTER, RtkLibImporter.FILE_FILTER), gpxFilter, JFileChooser.FILES_ONLY)
                .openFileChooser();
        return fc != null ? fc.getSelectedFile() : null;
    }

    /**
     * Loads {@link GpxData} from the given file and returns it. Shows message dialog in case of error.
     * @param file gpx data file, must not be null
     * @return {@code GpxData} file, or null in case of error
     */
    public static GpxData loadGpxData(File file) {
        GpxData data = null;
        try (InputStream iStream = Compression.getUncompressedFileInputStream(file)) {
            IGpxReader reader = GpxImporter.getFileFilter().accept(file) ? new GpxReader(iStream)
                    : NMEAImporter.FILE_FILTER.accept(file) ? new NmeaReader(iStream)
                            : new RtkLibPosReader(iStream);
            reader.parse(false);
            data = reader.getGpxData();
            data.storageFile = file;
        } catch (SAXException ex) {
            Logging.error(ex);
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("Error while parsing {0}", file.getName()) + ": " + ex.getMessage(),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (IOException ex) {
            Logging.error(ex);
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("Could not read \"{0}\"", file.getName()) + '\n' + ex.getMessage(),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return data;
    }
}
