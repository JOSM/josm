// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.ExtendedDialog;

/**
 * The tagging presets search dialog (F3).
 * @since 3388
 */
public final class TaggingPresetSearchDialog extends ExtendedDialog {

    private TaggingPresetSelector selector;

    private static TaggingPresetSearchDialog instance;
    
    /**
     * Returns the unique instance of {@code TaggingPresetSearchDialog}.
     * @return the unique instance of {@code TaggingPresetSearchDialog}.
     */
    public static TaggingPresetSearchDialog getInstance() {
        if (instance == null) {
            instance = new TaggingPresetSearchDialog();
        }
        return instance;
    }
    
    private TaggingPresetSearchDialog() {
        super(Main.parent, tr("Presets"), new String[] {tr("Select"), tr("Cancel")});
        selector = new TaggingPresetSelector(true, true);
        setContent(selector);
        DataSet.addSelectionListener(selector);
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
                preset.actionPerformed(null);
            }
        }
        selector.savePreferences();
    }
}