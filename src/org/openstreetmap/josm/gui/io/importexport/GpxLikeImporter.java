// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.IGpxReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.xml.sax.SAXException;

/**
 * Abstraction of {@link NMEAImporter}, {@link OziWptImporter} and {@link RtkLibImporter}.
 * @param <T> GPX reader type
 * @since 18179
 */
public abstract class GpxLikeImporter<T extends IGpxReader> extends FileImporter {

    private Class<T> klass;

    /**
     * Constructs a new {@code GpxLikeImporter}.
     * @param filter The extension file filter
     * @param klass type class
     */
    protected GpxLikeImporter(ExtensionFileFilter filter, Class<T> klass) {
        super(Objects.requireNonNull(filter));
        this.klass = Objects.requireNonNull(klass);
    }

    @Override
    public final void importData(File file, ProgressMonitor progressMonitor) throws IOException {
        final String fn = file.getName();
        try (InputStream fis = Compression.getUncompressedFileInputStream(file)) {
            final T r = buildAndParse(fis, klass);
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
            showInfobox(r.getNumberOfCoordinates() > 0, r);
        }
    }

    protected final void showInfobox(boolean success, T r) {
        final StringBuilder msg = new StringBuilder(160).append("<html>")
           .append(tr("Coordinates imported: {0}", r.getNumberOfCoordinates()));
        appendInfoboxContent(msg, success, r);
        msg.append("</html>");
        if (success) {
            SwingUtilities.invokeLater(() -> new Notification(
                    "<h3>" + tr("Import success:") + "</h3>" + msg.toString())
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .show());
        } else {
            HelpAwareOptionPane.showMessageDialogInEDT(
                    MainApplication.getMainFrame(),
                    msg.toString(),
                    tr("Import failure!"),
                    JOptionPane.ERROR_MESSAGE, null);
        }
    }

    protected void appendInfoboxContent(StringBuilder msg, boolean success, T r) {
        // Complete if needed
    }

    protected static final <T extends IGpxReader> T buildAndParse(InputStream fis, Class<T> klass) throws IOException {
        try {
            final T r = klass.getConstructor(InputStream.class).newInstance(fis);
            r.parse(true);
            return r;
        } catch (SAXException | ReflectiveOperationException | IllegalArgumentException | SecurityException e) {
            throw new IOException(e);
        }
    }
}
