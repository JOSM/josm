// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.io.importexport.JpgImporter;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;
import org.openstreetmap.josm.gui.widgets.FileChooserManager;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

/**
 * An aciton that imports images along a GPX path
 */
public class ImportImagesAction extends AbstractAction {
    private final transient GpxLayer layer;

    /**
     * Create a new {@link ImportImagesAction}
     * @param layer The layer this action should be for
     */
    public ImportImagesAction(final GpxLayer layer) {
        super(tr("Import images"));
        new ImageProvider("dialogs/geoimage").getResource().attachImageIcon(this, true);
        this.layer = layer;
        putValue("help", ht("/Action/ImportImages"));
    }

    private static void warnCantImportIntoServerLayer(GpxLayer layer) {
        String msg = tr("<html>The data in the GPX layer ''{0}'' has been downloaded from the server.<br>"+
                "Because its way points do not include a timestamp we cannot correlate them with images.</html>",
                Utils.escapeReservedCharactersHTML(layer.getName()));
        HelpAwareOptionPane.showOptionDialog(Main.parent, msg, tr("Import not possible"),
                JOptionPane.WARNING_MESSAGE, ht("/Action/ImportImages#CantImportIntoGpxLayerFromServer"));
    }

    private static void addRecursiveFiles(List<File> files, File... sel) {
        if (sel == null) { // listFiles might return null
            return;
        }
        for (File f : sel) {
            if (f.isDirectory()) {
                addRecursiveFiles(files, f.listFiles());
            } else if (Utils.hasExtension(f, "jpg")) {
                files.add(f);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (layer.data.fromServer) {
            warnCantImportIntoServerLayer(layer);
            return;
        }
        JpgImporter importer = new JpgImporter(layer);
        AbstractFileChooser fc = new FileChooserManager(true, "geoimage.lastdirectory", Config.getPref().get("lastDirectory")).
                createFileChooser(true, null, importer.filter, JFileChooser.FILES_AND_DIRECTORIES).openFileChooser();
        if (fc != null) {
            File[] sel = fc.getSelectedFiles();
            if (sel != null && sel.length > 0) {
                List<File> files = new LinkedList<>();
                addRecursiveFiles(files, sel);
                importer.importDataHandleExceptions(files, NullProgressMonitor.INSTANCE);
            }
        }
    }

}
