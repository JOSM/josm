// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The tagging presets search action (F3).
 * @since 3388
 */
public class TaggingPresetSearchAction extends JosmAction {

    /**
     * Constructs a new {@code TaggingPresetSearchAction}.
     */
    public TaggingPresetSearchAction() {
        super(tr("Search preset"), "dialogs/search", tr("Show preset search dialog"),
                Shortcut.registerShortcut("preset:search", tr("Search presets"), KeyEvent.VK_F3, Shortcut.DIRECT), false);
        putValue("toolbar", "presets/search");
        Main.toolbar.register(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (!Main.main.hasEditLayer())
            return;

        TaggingPresetSearchDialog.getInstance().showDialog();
    }
}
