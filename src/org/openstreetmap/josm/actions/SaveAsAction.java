// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
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
    private static SaveAsAction instance = new SaveAsAction();

    /**
     * Construct the action with "Save" as label.
     */
    public SaveAsAction() {
        super(tr("Save As..."), "save_as", tr("Save the current data to a new file."),
            Shortcut.registerShortcut("system:saveas", tr("File: {0}", tr("Save As...")),
            KeyEvent.VK_S, Shortcut.CTRL_SHIFT), false);
        setHelpId(ht("/Action/SaveAs"));
    }

    /**
     * Returns the unique instance.
     * @return the unique instance
     */
    public static SaveAsAction getInstance() {
        return instance;
    }

    @Override protected File getFile(Layer layer) {
        return layer.createAndOpenSaveFileChooser();
    }
}
