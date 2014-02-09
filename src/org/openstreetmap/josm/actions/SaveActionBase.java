//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.FileExporter;
import org.openstreetmap.josm.tools.Shortcut;

public abstract class SaveActionBase extends DiskAccessAction {
    private File file;

    public SaveActionBase(String name, String iconName, String tooltip, Shortcut shortcut) {
        super(name, iconName, tooltip, shortcut);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        boolean saved = doSave();
        if (saved) {
            addToFileOpenHistory();
        }
    }

    public boolean doSave() {
        if (Main.isDisplayingMapView()) {
            Layer layer = Main.map.mapView.getActiveLayer();
            if (layer != null && layer.isSavable()) {
                return doSave(layer);
            }
        }
        return false;
    }

    public boolean doSave(Layer layer) {
        if(!layer.checkSaveConditions())
            return false;
        file = getFile(layer);
        return doInternalSave(layer, file);
    }

    public static boolean doSave(Layer layer, File file) {
        if(!layer.checkSaveConditions())
            return false;
        return doInternalSave(layer, file);
    }

    private static boolean doInternalSave(Layer layer, File file) {
        if (file == null)
            return false;

        try {
            boolean exported = false;
            boolean canceled = false;
            for (FileExporter exporter : ExtensionFileFilter.exporters) {
                if (exporter.acceptFile(file, layer)) {
                    exporter.exportData(file, layer);
                    exported = true;
                    canceled = exporter.isCanceled();
                    break;
                }
            }
            if (!exported) {
                JOptionPane.showMessageDialog(Main.parent, tr("No Exporter found! Nothing saved."), tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
                return false;
            } else if (canceled) {
                return false;
            }
            layer.setName(file.getName());
            layer.setAssociatedFile(file);
            if (layer instanceof OsmDataLayer) {
                ((OsmDataLayer) layer).onPostSaveToFile();
            }
            Main.parent.repaint();
        } catch (IOException e) {
            Main.error(e);
            return false;
        }
        return true;
    }

    protected abstract File getFile(Layer layer);

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
        boolean check = Main.isDisplayingMapView()
        && Main.map.mapView.getActiveLayer() != null;
        if(!check) {
            setEnabled(false);
            return;
        }
        Layer layer = Main.map.mapView.getActiveLayer();
        setEnabled(layer != null && layer.isSavable());
    }

    /**
     * Creates a new "Save" dialog for a single {@link ExtensionFileFilter} and makes it visible.<br>
     * When the user has chosen a file, checks the file extension, and confirms overwrite if needed.
     *
     * @param title The dialog title
     * @param filter The dialog file filter
     * @return The output {@code File}
     * @since 5456
     * @see DiskAccessAction#createAndOpenFileChooser(boolean, boolean, String, FileFilter, int, String)
     */
    public static File createAndOpenSaveFileChooser(String title, ExtensionFileFilter filter) {
        JFileChooser fc = createAndOpenFileChooser(false, false, title, filter, JFileChooser.FILES_ONLY, null);
        return checkFileAndConfirmOverWrite(fc, filter.getDefaultExtension());
    }

    /**
     * Creates a new "Save" dialog for a given file extension and makes it visible.<br>
     * When the user has chosen a file, checks the file extension, and confirms overwrite if needed.
     *
     * @param title The dialog title
     * @param extension The file extension
     * @return The output {@code File}
     * @see DiskAccessAction#createAndOpenFileChooser(boolean, boolean, String, String)
     */
    public static File createAndOpenSaveFileChooser(String title, String extension) {
        JFileChooser fc = createAndOpenFileChooser(false, false, title, extension);
        return checkFileAndConfirmOverWrite(fc, extension);
    }

    private static File checkFileAndConfirmOverWrite(JFileChooser fc, String extension) {
        if (fc == null) return null;
        File file = fc.getSelectedFile();

        FileFilter ff = fc.getFileFilter();
        if (!ff.accept(file)) {
            // Extension of another filefilter given ?
            for (FileFilter cff : fc.getChoosableFileFilters()) {
                if (cff.accept(file)) {
                    fc.setFileFilter(cff);
                    return file;
                }
            }
            // No filefilter accepts current filename, add default extension
            String fn = file.getPath();
            if (ff instanceof ExtensionFileFilter) {
                fn += "." + ((ExtensionFileFilter)ff).getDefaultExtension();
            } else if (extension != null) {
                fn += "." + extension;
            }
            file = new File(fn);
            if (!confirmOverwrite(file))
                return null;
        }
        return file;
    }

    public static boolean confirmOverwrite(File file) {
        if (file == null || (file.exists())) {
            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                    tr("Overwrite"),
                    new String[] {tr("Overwrite"), tr("Cancel")}
            );
            dialog.setContent(tr("File exists. Overwrite?"));
            dialog.setButtonIcons(new String[] {"save_as.png", "cancel.png"});
            dialog.showDialog();
            return (dialog.getValue() == 1);
        }
        return true;
    }

    protected void addToFileOpenHistory() {
        String filepath;
        try {
            filepath = file.getCanonicalPath();
        } catch (IOException ign) {
            return;
        }

        int maxsize = Math.max(0, Main.pref.getInteger("file-open.history.max-size", 15));
        Collection<String> oldHistory = Main.pref.getCollection("file-open.history");
        List<String> history = new LinkedList<String>(oldHistory);
        history.remove(filepath);
        history.add(0, filepath);
        Main.pref.putCollectionBounded("file-open.history", maxsize, history);
    }
}
