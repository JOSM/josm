// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Utils;

public class OsmExporter extends FileExporter {

    public OsmExporter() {
        super(OsmImporter.FILE_FILTER);
    }

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

    public void exportData(File file, Layer layer, boolean noBackup) throws IOException {
        if (layer instanceof OsmDataLayer) {
            save(file, (OsmDataLayer) layer, noBackup);
        } else
            throw new IllegalArgumentException(MessageFormat.format("Expected instance of OsmDataLayer. Got ''{0}''.", layer
                    .getClass().getName()));
    }

    protected OutputStream getOutputStream(File file) throws FileNotFoundException, IOException {
        return Compression.getCompressedFileOutputStream(file);
    }

    private void save(File file, OsmDataLayer layer, boolean noBackup) {
        File tmpFile = null;
        try {
            // use a tmp file because if something errors out in the
            // process of writing the file, we might just end up with
            // a truncated file.  That can destroy lots of work.
            if (file.exists()) {
                tmpFile = new File(file.getPath() + "~");
                Utils.copyFile(file, tmpFile);
            }

            // create outputstream and wrap it with gzip or bzip, if necessary
            OutputStream out = getOutputStream(file);
            Writer writer = new OutputStreamWriter(out, Utils.UTF_8);

            OsmWriter w = OsmWriterFactory.createOsmWriter(new PrintWriter(writer), false, layer.data.getVersion());
            layer.data.getReadLock().lock();
            try {
                w.writeLayer(layer);
            } finally {
                Utils.close(w);
                layer.data.getReadLock().unlock();
            }
            // FIXME - how to close?
            if (noBackup || !Main.pref.getBoolean("save.keepbackup", false)) {
                if (tmpFile != null) {
                    tmpFile.delete();
                }
            }
            layer.onPostSaveToFile();
        } catch (IOException e) {
            Main.error(e);
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>An error occurred while saving.<br>Error is:<br>{0}</html>", e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );

            try {
                // if the file save failed, then the tempfile will not
                // be deleted.  So, restore the backup if we made one.
                if (tmpFile != null && tmpFile.exists()) {
                    Utils.copyFile(tmpFile, file);
                }
            } catch (IOException e2) {
                Main.error(e2);
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>An error occurred while restoring backup file.<br>Error is:<br>{0}</html>", e2.getMessage()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
