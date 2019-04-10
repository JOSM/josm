// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.InvalidPathException;
import java.text.MessageFormat;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.Compression;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.io.OsmWriterFactory;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Exports data to an .osm file.
 * @since 1949
 */
public class OsmExporter extends FileExporter {

    /**
     * Constructs a new {@code OsmExporter}.
     */
    public OsmExporter() {
        super(new ExtensionFileFilter(
            "osm,xml", "osm", tr("OSM Server Files") + " (*.osm)"));
    }

    /**
     * Constructs a new {@code OsmExporter}.
     * @param filter The extension file filter
     */
    public OsmExporter(ExtensionFileFilter filter) {
        super(filter);
    }

    @Override
    public boolean acceptFile(File pathname, Layer layer) {
        if (!(layer instanceof OsmDataLayer))
            return false;
        return super.acceptFile(pathname, layer);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        exportData(file, layer, false);
    }

    /**
     * Exports OSM data to the given file.
     * @param file Output file
     * @param layer Data layer. Must be an instance of {@link OsmDataLayer}.
     * @param noBackup if {@code true}, the potential backup file created if the output file already exists will be deleted
     *                 after a successful export
     * @throws IOException in case of IO errors
     * @throws InvalidPathException when file name cannot be converted into a Path
     * @throws IllegalArgumentException if {@code layer} is not an instance of {@code OsmDataLayer}
     */
    public void exportData(File file, Layer layer, boolean noBackup) throws IOException {
        if (!(layer instanceof OsmDataLayer)) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Expected instance of OsmDataLayer. Got ''{0}''.", layer.getClass().getName()));
        }
        save(file, (OsmDataLayer) layer, noBackup);
    }

    protected static OutputStream getOutputStream(File file) throws IOException {
        return Compression.getCompressedFileOutputStream(file);
    }

    private void save(File file, OsmDataLayer layer, boolean noBackup) throws IOException {
        File tmpFile = null;
        try {
            if (file.exists() && !file.canWrite()) {
                throw new AccessDeniedException(file.toString());
            }

            // use a tmp file because if something errors out in the process of writing the file,
            // we might just end up with a truncated file.  That can destroy lots of work.
            if (file.exists()) {
                tmpFile = new File(file.getPath() + '~');
                Utils.copyFile(file, tmpFile);
            }

            doSave(file, layer);
            if ((noBackup || !Config.getPref().getBoolean("save.keepbackup", false)) && tmpFile != null) {
                Utils.deleteFile(tmpFile);
            }
            layer.onPostSaveToFile();
        } catch (IOException | InvalidPathException e) {
            Logging.error(e);

            try {
                // if the file save failed, then the tempfile will not be deleted. So, restore the backup if we made one.
                if (tmpFile != null && tmpFile.exists()) {
                    Utils.copyFile(tmpFile, file);
                }
            } catch (IOException | InvalidPathException e2) {
                Logging.error(e2);
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("<html>An error occurred while restoring backup file.<br>Error is:<br>{0}</html>",
                                Utils.escapeReservedCharactersHTML(e2.getMessage())),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
            // re-throw original error
            throw e;
        }
    }

    protected void doSave(File file, OsmDataLayer layer) throws IOException {
        // create outputstream and wrap it with gzip, xz or bzip, if necessary
        try (
            OutputStream out = getOutputStream(file);
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            OsmWriter w = OsmWriterFactory.createOsmWriter(new PrintWriter(writer), false, layer.data.getVersion())
        ) {
            layer.data.getReadLock().lock();
            try {
                w.write(layer.data);
            } finally {
                layer.data.getReadLock().unlock();
            }
        }
    }
}
