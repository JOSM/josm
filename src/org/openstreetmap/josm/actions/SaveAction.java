// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.SaveToFile;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Export the data as an OSM xml file.
 *
 * @author imi
 */
public final class SaveAction extends SaveActionBase {
    private static SaveAction instance = new SaveAction();

    private final PropertyChangeListener updateOnRequireSaveChange = evt -> {
        if (OsmDataLayer.REQUIRES_SAVE_TO_DISK_PROP.equals(evt.getPropertyName())) {
            updateEnabledState();
        }
    };

    /**
     * Construct the action with "Save" as label.
     */
    private SaveAction() {
        super(tr("Save"), "save", tr("Save the current data."),
                Shortcut.registerShortcut("system:save", tr("File: {0}", tr("Save")), KeyEvent.VK_S, Shortcut.CTRL));
        setHelpId(ht("/Action/Save"));
    }

    /**
     * Returns the unique instance.
     * @return the unique instance
     */
    public static SaveAction getInstance() {
        return instance;
    }

    @Override
    protected LayerChangeAdapter buildLayerChangeAdapter() {
        return new LayerChangeAdapter() {
            @Override
            public void layerAdded(LayerAddEvent e) {
                if (e.getAddedLayer() instanceof OsmDataLayer) {
                    e.getAddedLayer().addPropertyChangeListener(updateOnRequireSaveChange);
                }
                super.layerAdded(e);
            }

            @Override
            public void layerRemoving(LayerRemoveEvent e) {
                if (e.getRemovedLayer() instanceof OsmDataLayer) {
                    e.getRemovedLayer().removePropertyChangeListener(updateOnRequireSaveChange);
                }
                super.layerRemoving(e);
            }
        };
    }

    @Override
    protected void updateEnabledState() {
        Layer activeLayer = getLayerManager().getActiveLayer();
        setEnabled(activeLayer != null && activeLayer.isSavable()
                && (!(activeLayer.getAssociatedFile() != null
                    && activeLayer instanceof SaveToFile && !((SaveToFile) activeLayer).requiresSaveToFile())));
    }

    @Override
    public File getFile(Layer layer) {
        File f = layer.getAssociatedFile();
        if (f != null && !f.exists()) {
            f = null;
        }

        // Ask for overwrite in case of GpxLayer: GpxLayers usually are imports
        // and modifying is an error most of the time.
        if (f != null && layer instanceof GpxLayer) {
            ExtendedDialog dialog = new ExtendedDialog(
                    MainApplication.getMainFrame(),
                    tr("Overwrite"),
                    tr("Overwrite"), tr("Cancel"))
                .setButtonIcons("save_as", "cancel")
                .setContent(tr("File {0} exists. Overwrite?", f.getName()));
            if (dialog.showDialog().getValue() != 1) {
                f = null;
            }
        }
        return f == null ? layer.createAndOpenSaveFileChooser() : f;
    }
}
