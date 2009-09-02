// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class OsmExporter extends FileExporter {

    public OsmExporter() {
        super(new ExtensionFileFilter("osm,xml", "osm", tr("OSM Server Files") + " (*.osm *.xml)"));
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
        if (layer instanceof OsmDataLayer) {
            save(file, (OsmDataLayer) layer);
        } else
            throw new IllegalArgumentException(tr("Expected instance of OsmDataLayer. Got ''{0}''.", layer
                    .getClass().getName()));
    }

    protected OutputStream getOutputStream(File file) throws FileNotFoundException, IOException {
        return new FileOutputStream(file);
    }

    private void save(File file, OsmDataLayer layer) {
        File tmpFile = null;
        try {
            // use a tmp file because if something errors out in the
            // process of writing the file, we might just end up with
            // a truncated file.  That can destroy lots of work.
            if (file.exists()) {
                tmpFile = new File(file.getPath() + "~");
                copy(file, tmpFile);
            }

            // create outputstream and wrap it with gzip or bzip, if necessary
            OutputStream out = getOutputStream(file);
            Writer writer = new OutputStreamWriter(out, "UTF-8");

            OsmWriter w = new OsmWriter(new PrintWriter(writer), false, layer.data.version);
            w.header();
            w.writeDataSources(layer.data);
            w.writeContent(layer.data);
            w.footer();
            w.close();
            // FIXME - how to close?
            if (!Main.pref.getBoolean("save.keepbackup") && (tmpFile != null)) {
                tmpFile.delete();
            }
            layer.onPostSaveToFile();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>An error occurred while saving.<br>Error is: <br>{0}</html>", e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );

            try {
                // if the file save failed, then the tempfile will not
                // be deleted.  So, restore the backup if we made one.
                if (tmpFile != null && tmpFile.exists()) {
                    copy(tmpFile, file);
                }
            } catch (IOException e2) {
                e2.printStackTrace();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>An error occurred while restoring backup file.<br>Error is: <br>{0}</html>", e2.getMessage()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void copy(File src, File dst) throws IOException {
        FileInputStream srcStream;
        FileOutputStream dstStream;
        try {
            srcStream = new FileInputStream(src);
            dstStream = new FileOutputStream(dst);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(Main.parent, tr("Could not back up file. Exception is: {0}", e
                    .getMessage()), tr("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        byte buf[] = new byte[1 << 16];
        int len;
        while ((len = srcStream.read(buf)) != -1) {
            dstStream.write(buf, 0, len);
        }
        srcStream.close();
        dstStream.close();
    }

}
