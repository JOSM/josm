// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.tagging.PresetHandler;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.PresetLabel;
import org.openstreetmap.josm.tools.GBC;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Map;

/**
 * A list of matching presets for a set of tags.
 */
public class PresetListPanel extends JPanel {

    /**
     * Constructs a new {@code PresetListPanel}.
     */
    public PresetListPanel() {
        super(new GridBagLayout());
    }

    /**
     * Updates the preset list based on the {@code tags} and {@code types},
     * and associates an interaction with (matching) presets via {@code presetHandler}.
     */
    public void updatePresets(final Collection<TaggingPresetType> types, final Map<String, String> tags, final PresetHandler presetHandler) {

        removeAll();
        if (types.isEmpty()) {
            setVisible(false);
            return;
        }

        for (final TaggingPreset t : TaggingPreset.getMatchingPresets(types, tags, true)) {
            final JLabel lbl = new PresetLabel(t);
            lbl.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    Collection<OsmPrimitive> selection = t.createSelection(presetHandler.getSelection());
                    if (selection == null || selection.isEmpty())
                        return;
                    int answer = t.showDialog(selection, false);

                    if (answer == TaggingPreset.DIALOG_ANSWER_APPLY) {
                        presetHandler.updateTags(t.getChangedTags());
                    }
                }
            });
            add(lbl, GBC.eol().fill(GBC.HORIZONTAL));
        }

        if (getComponentCount() > 0) {
            setVisible(true);
            // This ensures the presets are exactly as high as needed.
            int height = getComponentCount() * getComponent(0).getHeight();
            Dimension size = new Dimension(getWidth(), height);
            setMaximumSize(size);
            setMinimumSize(size);
        } else {
            setVisible(false);
        }
    }
}
