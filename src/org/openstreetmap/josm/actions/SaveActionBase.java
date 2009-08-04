//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.zip.GZIPOutputStream;

import javax.swing.JOptionPane;

import org.apache.tools.bzip2.CBZip2OutputStream;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.OptionPaneUtil;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.GpxImporter;
import org.openstreetmap.josm.io.GpxWriter;
import org.openstreetmap.josm.io.OsmBzip2Importer;
import org.openstreetmap.josm.io.OsmGzipImporter;
import org.openstreetmap.josm.io.OsmImporter;
import org.openstreetmap.josm.io.OsmWriter;
import org.openstreetmap.josm.tools.Shortcut;

public abstract class SaveActionBase extends DiskAccessAction {

    public SaveActionBase(String name, String iconName, String tooltip, Shortcut shortcut) {
        super(name, iconName, tooltip, shortcut);
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        doSave();
    }

    public boolean doSave() {
        Layer layer = null;
        if (layer == null && Main.map != null && (Main.map.mapView.getActiveLayer() instanceof OsmDataLayer
                || Main.map.mapView.getActiveLayer() instanceof GpxLayer)) {
            layer = Main.map.mapView.getActiveLayer();
        }
        if (layer == null)
            return false;
        return doSave(layer);
    }

    public boolean doSave(Layer layer) {
        if (layer == null)
            return false;
        if ( !(layer instanceof OsmDataLayer) && !(layer instanceof GpxLayer))
            return false;
        if (!checkSaveConditions(layer))
            return false;

        File file = getFile(layer);
        if (file == null)
            return false;

        save(file, layer);

        layer.setName(file.getName());
        layer.setAssociatedFile(file);
        Main.parent.repaint();
        return true;
    }

    protected abstract File getFile(Layer layer);

    /**
     * Checks whether it is ok to launch a save (whether we have data,
     * there is no conflict etc.)
     * @return <code>true</code>, if it is safe to save.
     */
    public boolean checkSaveConditions(Layer layer) {
        if (layer == null) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("Internal Error: cannot check conditions for no layer. Please report this as a bug."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
        if (Main.map == null) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("No document open so nothing to save."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        if (layer instanceof OsmDataLayer && isDataSetEmpty((OsmDataLayer)layer) && 1 != new ExtendedDialog(Main.parent, tr("Empty document"), tr("The document contains no data."), new String[] {tr("Save anyway"), tr("Cancel")}, new String[] {"save.png", "cancel.png"}).getValue())
            return false;
        if (layer instanceof GpxLayer && ((GpxLayer)layer).data == null)
            return false;
        if (layer instanceof OsmDataLayer)  {
            ConflictCollection conflicts = ((OsmDataLayer)layer).getConflicts();
            if (conflicts != null && !conflicts.isEmpty()) {
                int answer = new ExtendedDialog(Main.parent,
                        tr("Conflicts"),
                        tr("There are unresolved conflicts. Conflicts will not be saved and handled as if you rejected all. Continue?"),
                        new String[] {tr("Reject Conflicts and Save"), tr("Cancel")},
                        new String[] {"save.png", "cancel.png"}).getValue();

                if (answer != 1) return false;
            }
        }
        return true;
    }

    public static File openFileDialog(Layer layer) {
        if (layer instanceof OsmDataLayer)
            return createAndOpenSaveFileChooser(tr("Save OSM file"), ".osm");
        else if (layer instanceof GpxLayer)
            return createAndOpenSaveFileChooser(tr("Save GPX file"), ".gpx");
        return createAndOpenSaveFileChooser(tr("Save Layer"), ".lay");
    }

    private static void copy(File src, File dst) throws IOException {
        FileInputStream srcStream;
        FileOutputStream dstStream;
        try {
            srcStream = new FileInputStream(src);
            dstStream = new FileOutputStream(dst);
        } catch (FileNotFoundException e) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("Could not back up file. Exception is: {0}", e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        byte buf[] = new byte[1<<16];
        int len;
        while ((len = srcStream.read(buf)) != -1) {
            dstStream.write(buf, 0, len);
        }
        srcStream.close();
        dstStream.close();
    }

    public static void save(File file, Layer layer) {
        if (layer instanceof GpxLayer) {
            save(file, (GpxLayer)layer);
            ((GpxLayer)layer).data.storageFile = file;
        } else if (layer instanceof OsmDataLayer) {
            save(file, (OsmDataLayer)layer);
        }
    }

    public static void save(File file, OsmDataLayer layer) {
        File tmpFile = null;
        try {
            GpxImporter gpxImExporter = new GpxImporter();
            OsmImporter osmImExporter = new OsmImporter();
            OsmGzipImporter osmGzipImporter = new OsmGzipImporter();
            OsmBzip2Importer osmBzip2Importer = new OsmBzip2Importer();
            if (gpxImExporter.acceptFile(file)) {
                new GpxExportAction().exportGpx(file, layer);
            } else if (osmImExporter.acceptFile(file)
                    || osmGzipImporter.acceptFile(file)
                    || osmBzip2Importer.acceptFile(file))
            {
                // use a tmp file because if something errors out in the
                // process of writing the file, we might just end up with
                // a truncated file.  That can destroy lots of work.
                if (file.exists()) {
                    tmpFile = new File(file.getPath() + "~");
                    copy(file, tmpFile);
                }

                // create outputstream and wrap it with gzip or bzip, if necessary
                OutputStream out = new FileOutputStream(file);
                if(osmGzipImporter.acceptFile(file)) {
                    out = new GZIPOutputStream(out);
                } else if(osmBzip2Importer.acceptFile(file)) {
                    out.write('B');
                    out.write('Z');
                    out = new CBZip2OutputStream(out);
                }
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
            } else {
                OptionPaneUtil.showMessageDialog(
                        Main.parent,
                        tr("Unknown file extension for file ''{0}''", file.toString()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
            layer.cleanupAfterSaveToDisk();
        } catch (IOException e) {
            e.printStackTrace();
            OptionPaneUtil.showMessageDialog(
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
                OptionPaneUtil.showMessageDialog(
                        Main.parent,
                        tr("<html>An error occurred while restoring backup file.<br>Error is: <br>{0}</html>", e2.getMessage()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    public static void save(File file, GpxLayer layer) {
        File tmpFile = null;
        try {
            GpxImporter gpxImExporter = new GpxImporter();
            if (gpxImExporter.acceptFile(file)) {

                // use a tmp file because if something errors out in the
                // process of writing the file, we might just end up with
                // a truncated file.  That can destroy lots of work.
                if (file.exists()) {
                    tmpFile = new File(file.getPath() + "~");
                    copy(file, tmpFile);
                }
                FileOutputStream fo = new FileOutputStream(file);
                new GpxWriter(fo).write(layer.data);
                fo.flush();
                fo.close();

                if (!Main.pref.getBoolean("save.keepbackup") && (tmpFile != null)) {
                    tmpFile.delete();
                }
            } else {
                OptionPaneUtil.showMessageDialog(
                        Main.parent,
                        tr("Unknown file extension."),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("An error occurred while saving. Error is: {0}", e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
        try {
            // if the file save failed, then the tempfile will not
            // be deleted.  So, restore the backup if we made one.
            if (tmpFile != null && tmpFile.exists()) {
                copy(tmpFile, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("<html>An error occurred while restoring backup file.<br>Error is:<br>{0}</html>", e.getMessage()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );;
        }
    }

    /**
     * Check the data set if it would be empty on save. It is empty, if it contains
     * no objects (after all objects that are created and deleted without being
     * transferred to the server have been removed).
     *
     * @return <code>true</code>, if a save result in an empty data set.
     */
    private boolean isDataSetEmpty(OsmDataLayer layer) {
        for (OsmPrimitive osm : layer.data.allNonDeletedPrimitives())
            if (!osm.deleted || osm.id > 0)
                return false;
        return true;
    }

    /**
     * Refreshes the enabled state
     * 
     */
    @Override
    protected void updateEnabledState() {
        if (Main.applet) {
            setEnabled(false);
            return;
        }
        boolean check =  Main.map != null
        && Main.map.mapView !=null
        && Main.map.mapView.getActiveLayer() != null;
        if(!check) {
            setEnabled(false);
            return;
        }
        Layer layer = Main.map.mapView.getActiveLayer();
        setEnabled(layer instanceof OsmDataLayer || layer instanceof GpxLayer);
    }
}
