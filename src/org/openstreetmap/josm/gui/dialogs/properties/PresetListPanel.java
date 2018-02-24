// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetHandler;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetLabel;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.tools.GBC;

/**
 * A list of matching presets for a set of tags.
 */
public class PresetListPanel extends JPanel {

    static final class LabelMouseAdapter extends MouseAdapter {
        private final TaggingPreset t;
        private final TaggingPresetHandler presetHandler;

        LabelMouseAdapter(TaggingPreset t, TaggingPresetHandler presetHandler) {
            this.t = t;
            this.presetHandler = presetHandler;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            Collection<OsmPrimitive> selection = t.createSelection(presetHandler.getSelection());
            if (selection.isEmpty())
                return;
            int answer = t.showDialog(selection, false);

            if (answer == TaggingPreset.DIALOG_ANSWER_APPLY && !selection.iterator().next().getDataSet().isLocked()) {
                presetHandler.updateTags(t.getChangedTags());
            }
        }
    }

    /**
     * Constructs a new {@code PresetListPanel}.
     */
    public PresetListPanel() {
        super(new GridBagLayout());
    }

    /**
     * Updates the preset list based on the {@code tags} and {@code types},
     * and associates an interaction with (matching) presets via {@code presetHandler}.
     * @param types collection of tagging presets types
     * @param tags collection of tags
     * @param presetHandler tagging preset handler
     */
    public void updatePresets(final Collection<TaggingPresetType> types, final Map<String, String> tags,
            final TaggingPresetHandler presetHandler) {

        removeAll();
        if (types.isEmpty()) {
            setVisible(false);
            return;
        }

        for (final TaggingPreset t : TaggingPresets.getMatchingPresets(types, tags, true)) {
            final JLabel lbl = new TaggingPresetLabel(t);
            lbl.addMouseListener(new LabelMouseAdapter(t, presetHandler));
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
