// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.io.File;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Export the data as an OSM xml file.
 *
 * @author imi
 */
public class SaveAction extends SaveActionBase {

    /**
     * Construct the action with "Save" as label.
     * @param layer Save this layer.
     */
    public SaveAction() {
        super(tr("Save"), "save", tr("Save the current data."),
                Shortcut.registerShortcut("system:save", tr("File: {0}", tr("Save")), KeyEvent.VK_S, Shortcut.GROUP_MENU));
    }

    @Override public File getFile(Layer layer) {
        File f = layer.getAssociatedFile();
        if(f != null && ! f.exists()) {
            f=null;
        }
        if(f != null && layer instanceof GpxLayer && 1 !=
            new ExtendedDialog(Main.parent, tr("Overwrite"),
                    tr("File {0} exists. Overwrite?", f.getName()),
                    new String[] {tr("Overwrite"), tr("Cancel")},
                    new String[] {"save_as.png", "cancel.png"}).getValue()) {
            f = null;
        }
        return f == null ? openFileDialog(layer) : f;
    }
}
