//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.FileExporter;
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
        if (Main.isDisplayingMapView() && (Main.map.mapView.getActiveLayer() instanceof OsmDataLayer
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

        try {
            boolean exported = false;
            for (FileExporter exporter : ExtensionFileFilter.exporters) {
                if (exporter.acceptFile(file, layer)) {
                    exporter.exportData(file, layer);
                    exported = true;
                }
            }
            if (!exported) {
                JOptionPane.showMessageDialog(Main.parent, tr("No Exporter found! Nothing saved."), tr("Warning"),
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
            layer.setName(file.getName());
            layer.setAssociatedFile(file);
            if (layer instanceof OsmDataLayer) {
                ((OsmDataLayer) layer).onPostSaveToFile();
            }
            Main.parent.repaint();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    protected abstract File getFile(Layer layer);

    /**
     * Checks whether it is ok to launch a save (whether we have data,
     * there is no conflict etc.)
     * @return <code>true</code>, if it is safe to save.
     */
    public boolean checkSaveConditions(Layer layer) {
        if (layer instanceof OsmDataLayer && isDataSetEmpty((OsmDataLayer)layer)) {
            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                    tr("Empty document"),
                    new String[] {tr("Save anyway"), tr("Cancel")}
            );
            dialog.setContent(tr("The document contains no data."));
            dialog.setButtonIcons(new String[] {"save.png", "cancel.png"});
            dialog.showDialog();
            if (dialog.getValue() != 1) return false;
        }

        if (layer instanceof GpxLayer && ((GpxLayer)layer).data == null)
            return false;
        if (layer instanceof OsmDataLayer)  {
            ConflictCollection conflicts = ((OsmDataLayer)layer).getConflicts();
            if (conflicts != null && !conflicts.isEmpty()) {
                ExtendedDialog dialog = new ExtendedDialog(
                        Main.parent,
                        tr("Conflicts"),
                        new String[] {tr("Reject Conflicts and Save"), tr("Cancel")}
                );
                dialog.setContent(tr("There are unresolved conflicts. Conflicts will not be saved and handled as if you rejected all. Continue?"));
                dialog.setButtonIcons(new String[] {"save.png", "cancel.png"});
                dialog.showDialog();
                if (dialog.getValue() != 1) return false;
            }
        }
        return true;
    }

    public static File openFileDialog(Layer layer) {
        if (layer instanceof OsmDataLayer)
            return createAndOpenSaveFileChooser(tr("Save OSM file"), "osm");
        else if (layer instanceof GpxLayer)
            return createAndOpenSaveFileChooser(tr("Save GPX file"), "gpx");
        return createAndOpenSaveFileChooser(tr("Save Layer"), "lay");
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
            if (!osm.isDeleted() || !osm.isNewOrUndeleted())
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

    public static File createAndOpenSaveFileChooser(String title, String extension) {
        String curDir = Main.pref.get("lastDirectory");
        if (curDir.equals("")) {
            curDir = ".";
        }
        JFileChooser fc = new JFileChooser(new File(curDir));
        if (title != null) {
            fc.setDialogTitle(title);
        }

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setAcceptAllFileFilterUsed(false);
        ExtensionFileFilter.applyChoosableExportFileFilters(fc, extension);
        int answer = fc.showSaveDialog(Main.parent);
        if (answer != JFileChooser.APPROVE_OPTION)
            return null;

        if (!fc.getCurrentDirectory().getAbsolutePath().equals(curDir)) {
            Main.pref.put("lastDirectory", fc.getCurrentDirectory().getAbsolutePath());
        }

        File file = fc.getSelectedFile();
        String fn = file.getPath();
        if(fn.indexOf('.') == -1)
        {
            FileFilter ff = fc.getFileFilter();
            if (ff instanceof ExtensionFileFilter) {
                fn += "." + ((ExtensionFileFilter)ff).getDefaultExtension();
            } else if(extension != null) {
                fn += "." + extension;
            }
            file = new File(fn);
        }
        if(file == null || (file.exists())) {
            ExtendedDialog dialog = new ExtendedDialog(
                    Main.parent,
                    tr("Overwrite"),
                    new String[] {tr("Overwrite"), tr("Cancel")}
            );
            dialog.setContent(tr("File exists. Overwrite?"));
            dialog.setButtonIcons(new String[] {"save_as.png", "cancel.png"});
            dialog.showDialog();
            if (dialog.getValue() != 1) return null;
        }
        return file;
    }
}
