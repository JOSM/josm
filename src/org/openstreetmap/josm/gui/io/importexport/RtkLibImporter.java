// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.io.importexport.GpxImporter.GpxImporterData;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.rtklib.RtkLibPosReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.xml.sax.SAXException;

/**
 * File importer allowing to import RTKLib files (*.pos files).
 * @since 15247
 */
public class RtkLibImporter extends FileImporter {

    /**
     * The RtkLib file filter (*.pos files).
     */
    public static final ExtensionFileFilter FILE_FILTER = ExtensionFileFilter.newFilterWithArchiveExtensions(
            "pos", "pos", tr("RTKLib Positioning Solution Files"), false);

    /**
     * Constructs a new {@code RtkLibImporter}.
     */
    public RtkLibImporter() {
        super(FILE_FILTER);
    }

    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException {
        final String fn = file.getName();
        try (InputStream fis = Compression.getUncompressedFileInputStream(file)) {
            final RtkLibPosReader r = buildAndParse(fis);
            if (r.getNumberOfCoordinates() > 0) {
                r.getGpxData().storageFile = file;
                final GpxLayer gpxLayer = new GpxLayer(r.getGpxData(), fn, true);
                final File fileFinal = file;

                GuiHelper.runInEDT(() -> {
                    MainApplication.getLayerManager().addLayer(gpxLayer);
                    if (Config.getPref().getBoolean("marker.makeautomarkers", true)) {
                        MarkerLayer ml = new MarkerLayer(r.getGpxData(), tr("Markers from {0}", fn), fileFinal, gpxLayer);
                        if (!ml.data.isEmpty()) {
                            MainApplication.getLayerManager().addLayer(ml);
                        }
                    }
                });
            }
            showRtkLibInfobox(r.getNumberOfCoordinates() > 0, r);
        }
    }

    private static void showRtkLibInfobox(boolean success, RtkLibPosReader r) {
        final StringBuilder msg = new StringBuilder(160).append("<html>")
           .append(tr("Coordinates imported: {0}", r.getNumberOfCoordinates()))
           .append("</html>");
        if (success) {
            SwingUtilities.invokeLater(() -> new Notification(
                    "<h3>" + tr("RTKLib import success:") + "</h3>" + msg.toString())
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .show());
        } else {
            HelpAwareOptionPane.showMessageDialogInEDT(
                    MainApplication.getMainFrame(),
                    msg.toString(),
                    tr("RTKLib import failure!"),
                    JOptionPane.ERROR_MESSAGE, null);
        }
    }

    /**
     * Replies the new GPX and marker layers corresponding to the specified RTKLib file.
     * @param is input stream to RTKLib data
     * @param associatedFile RTKLib file
     * @param gpxLayerName The GPX layer name
     * @param markerLayerName The marker layer name
     * @return the new GPX and marker layers corresponding to the specified RTKLib file
     * @throws IOException if an I/O error occurs
     */
    public static GpxImporterData loadLayers(InputStream is, final File associatedFile,
            final String gpxLayerName, String markerLayerName) throws IOException {
        final RtkLibPosReader r = buildAndParse(is);
        final boolean parsedProperly = r.getNumberOfCoordinates() > 0;
        r.getGpxData().storageFile = associatedFile;
        return GpxImporter.loadLayers(r.getGpxData(), parsedProperly, gpxLayerName, markerLayerName);
    }

    static RtkLibPosReader buildAndParse(InputStream fis) throws IOException {
        final RtkLibPosReader r = new RtkLibPosReader(fis);
        try {
            r.parse(true);
        } catch (SAXException e) {
            throw new IOException(e);
        }
        return r;
    }
}
