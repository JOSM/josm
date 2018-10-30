// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSearchDialog;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The tagging presets search action (F3).
 * @since 3388
 * @since 12834 (moved from {@code gui.tagging.presets} package)
 */
public class TaggingPresetSearchAction extends JosmAction {

    /**
     * Constructs a new {@code TaggingPresetSearchAction}.
     */
    public TaggingPresetSearchAction() {
        super(tr("Search preset..."), "dialogs/search", tr("Show preset search dialog"),
                Shortcut.registerShortcut("preset:search", tr("Search presets"), KeyEvent.VK_F3, Shortcut.DIRECT), false);
        putValue("toolbar", "presets/search");
        MainApplication.getToolbar().register(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (MainApplication.getLayerManager().getActiveData() == null)
            return;

        TaggingPresetSearchDialog.getInstance().showDialog();
    }
}
