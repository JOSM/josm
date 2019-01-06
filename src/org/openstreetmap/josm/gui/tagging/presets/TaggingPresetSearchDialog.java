// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * The tagging presets search dialog (F3).
 * @since 3388
 */
public final class TaggingPresetSearchDialog extends ExtendedDialog {

    private static TaggingPresetSearchDialog instance;

    private final TaggingPresetSelector selector;

    /**
     * Returns the unique instance of {@code TaggingPresetSearchDialog}.
     * @return the unique instance of {@code TaggingPresetSearchDialog}.
     */
    public static synchronized TaggingPresetSearchDialog getInstance() {
        if (instance == null) {
            instance = new TaggingPresetSearchDialog();
        }
        return instance;
    }

    private TaggingPresetSearchDialog() {
        super(MainApplication.getMainFrame(), tr("Search presets"), tr("Select"), tr("Cancel"));
        setButtonIcons("dialogs/search", "cancel");
        configureContextsensitiveHelp("/Action/TaggingPresetSearch", true /* show help button */);
        selector = new TaggingPresetSelector(true, true);
        setContent(selector, false);
        SelectionEventManager.getInstance().addSelectionListener(selector);
        selector.setDblClickListener(e -> buttonAction(0, null));
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
            TaggingPreset preset = selector.getSelectedPresetAndUpdateClassification();
            if (preset != null) {
                preset.actionPerformed(null);
            }
        }
        selector.savePreferences();
    }
}
