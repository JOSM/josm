// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.io.File;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Export the data.
 *
 * @author imi
 */
public class SaveAsAction extends SaveActionBase {

    /**
     * Construct the action with "Save" as label.
     * @param layer Save this layer.
     */
    public SaveAsAction() {
        super(tr("Save As..."), "save_as", tr("Save the current data to a new file."),
                Shortcut.registerShortcut("system:saveas", tr("File: {0}", tr("Save As...")), KeyEvent.VK_S, Shortcut.GROUP_MENU, Shortcut.SHIFT_DEFAULT));
    }

    @Override protected File getFile(Layer layer) {
        return openFileDialog(layer);
    }
}
