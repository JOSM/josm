// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * A dialog that allows to select a preset and then selects all matching OSM objects.
 * @see org.openstreetmap.josm.gui.tagging.TaggingPresetSearchDialog
 */
public final class TaggingPresetSearchPrimitiveDialog extends ExtendedDialog {

    private TaggingPresetSelector selector;

    private static TaggingPresetSearchPrimitiveDialog instance;

    /**
     * Returns the unique instance of {@code TaggingPresetSearchPrimitiveDialog}.
     * @return the unique instance of {@code TaggingPresetSearchPrimitiveDialog}.
     */
    public static synchronized TaggingPresetSearchPrimitiveDialog getInstance() {
        if (instance == null) {
            instance = new TaggingPresetSearchPrimitiveDialog();
        }
        return instance;
    }

    TaggingPresetSearchPrimitiveDialog() {
        super(Main.parent, tr("Presets"), new String[] {tr("Search"), tr("Cancel")});
        selector = new TaggingPresetSelector(false, false);
        setContent(selector);
        selector.setDblClickListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonAction(0, null);
            }
        });
    }

    @Override
    public ExtendedDialog showDialog() {
        selector.init();
        super.showDialog();
        selector.clearSelection();
        return this;
    }

    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        super.buttonAction(buttonIndex, evt);
        if (buttonIndex == 0) {
            TaggingPreset preset = selector.getSelectedPreset();
            if (preset != null) {
                final Set<OsmPrimitive> matching = new HashSet<>(Utils.filter(Main.main.getCurrentDataSet().allPrimitives(), preset));
                Main.main.getCurrentDataSet().setSelected(matching);
            }
        }
    }

    /**
     * An action executing {@link TaggingPresetSearchPrimitiveDialog}.
     */
    public static class Action extends JosmAction {

        /**
         * Constructs a new {@link TaggingPresetSearchPrimitiveDialog.Action}.
         */
        public Action() {
            super(tr("Search for objects by preset"), "dialogs/search", tr("Show preset search dialog"),
                    Shortcut.registerShortcut("preset:search-objects", tr("Search for objects by preset"), KeyEvent.VK_F3, Shortcut.SHIFT), false);
            putValue("toolbar", "presets/search-objects");
            Main.toolbar.register(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (Main.main.hasEditLayer()) {
                TaggingPresetSearchPrimitiveDialog.getInstance().showDialog();
            }
        }
    }

}
