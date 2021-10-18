// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.SaveToFile;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Export the data as an OSM xml file.
 *
 * @author imi
 */
public final class SaveAction extends SaveActionBase {
    private static final SaveAction instance = new SaveAction();

    private final PropertyChangeListener updateOnRequireSaveChange = evt -> {
        if (AbstractModifiableLayer.REQUIRES_SAVE_TO_DISK_PROP.equals(evt.getPropertyName())) {
            updateEnabledState();
        }
    };

    /**
     * Construct the action with "Save" as label.
     */
    private SaveAction() {
        super(tr("Save"), "save", tr("Save the current data."),
                Shortcut.registerShortcut("system:save", tr("File: {0}", tr("Save")), KeyEvent.VK_S, Shortcut.CTRL),
                true);
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
                Layer l = e.getAddedLayer();
                if (l instanceof AbstractModifiableLayer) {
                    l.addPropertyChangeListener(updateOnRequireSaveChange);
                }
                super.layerAdded(e);
            }

            @Override
            public void layerRemoving(LayerRemoveEvent e) {
                Layer l = e.getRemovedLayer();
                if (l instanceof AbstractModifiableLayer) {
                    l.removePropertyChangeListener(updateOnRequireSaveChange);
                }
                super.layerRemoving(e);
            }
        };
    }

    @Override
    protected boolean listenToSelectionChange() {
        return false;
    }

    @Override
    protected void updateEnabledState() {
        Layer activeLayer = getLayerManager().getActiveLayer();
        boolean en = activeLayer != null
                && activeLayer.isSavable() && !(activeLayer.getAssociatedFile() != null
                && activeLayer instanceof SaveToFile && !((SaveToFile) activeLayer).requiresSaveToFile());
        GuiHelper.runInEDT(() -> setEnabled(en));
    }

    @Override
    public File getFile(Layer layer) {
        File f = layer.getAssociatedFile();
        if (f != null && !f.exists()) {
            f = null;
        }

        // Ask for overwrite in case of GpxLayer
        if (f != null
                && layer instanceof GpxLayer
                && (((GpxLayer) layer).data == null
                || !GpxConstants.JOSM_CREATOR_NAME.equals(((GpxLayer) layer).data.creator))
                && !Config.getPref().getBoolean("gpx.export.overwrite", false)) {

            JPanel p = new JPanel(new GridBagLayout());
            JLabel label = new JLabel("<html>"
                    + tr("The file \"{0}\" will be modified.<br>Would you like to overwrite the existing file?", f.getName())
                    + "</html>");
            label.setHorizontalAlignment(SwingConstants.CENTER);
            JCheckBox remember = new JCheckBox(tr("Always overwrite GPX files without asking"));
            remember.setHorizontalAlignment(SwingConstants.CENTER);
            p.add(label, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 5, 5, 10));
            p.add(remember, GBC.eop().fill(GBC.HORIZONTAL));
            ExtendedDialog dialog = new ExtendedDialog(
                    MainApplication.getMainFrame(),
                    tr("Overwrite"),
                    tr("Overwrite"), tr("Save As..."), tr("Cancel"))
                .setButtonIcons("save", "save_as", "cancel")
                .setContent(p);
            int val = dialog.showDialog().getValue();
            if (val == 1) {
                Config.getPref().putBoolean("gpx.export.overwrite", remember.isSelected());
            } else if (val == 2) {
                f = null;
            } else {
                return null;
            }
        }
        return f == null ? layer.createAndOpenSaveFileChooser() : f;
    }
}
