// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetType;
import org.openstreetmap.josm.tools.GBC;

public class PresetListPanel extends JPanel {

    public PresetListPanel() {
        super(new GridBagLayout());
    }

    public interface PresetHandler {
        Collection<OsmPrimitive> getSelection();
        void updateTags(List<Tag> tags);
    }

    /**
     * Small helper class that manages the highlighting of the label on hover as well as opening
     * the corresponding preset when clicked
     */
    public static class PresetLabelML implements MouseListener {
        final JLabel label;
        final Font hover;
        final Font normal;
        final TaggingPreset tag;
        final PresetHandler presetHandler;

        public PresetLabelML(JLabel lbl, TaggingPreset t, PresetHandler presetHandler) {
            super();
            label = lbl;
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            normal = label.getFont();
            hover = normal.deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED));
            tag = t;
            this.presetHandler = presetHandler;
        }
        @Override
        public void mouseClicked(MouseEvent arg0) {
            Collection<OsmPrimitive> selection = tag.createSelection(presetHandler.getSelection());
            if (selection == null || selection.isEmpty())
                return;
            int answer = tag.showDialog(selection, false);

            if (answer == TaggingPreset.DIALOG_ANSWER_APPLY) {
                presetHandler.updateTags(tag.getChangedTags());
            }

        }
        @Override
        public void mouseEntered(MouseEvent arg0) {
            label.setFont(hover);
        }
        @Override
        public void mouseExited(MouseEvent arg0) {
            label.setFont(normal);
        }
        @Override
        public void mousePressed(MouseEvent arg0) {}
        @Override
        public void mouseReleased(MouseEvent arg0) {}
    }

    public static JLabel createLabelForPreset(TaggingPreset t) {
        JLabel lbl = new JLabel(t.getName() + " …");
        lbl.setIcon(t.getIcon());
        return lbl;
    }

    public void updatePresets(final Collection<TaggingPresetType> types, final Map<String, String> tags, PresetHandler presetHandler) {

        removeAll();
        if (types.isEmpty()) {
            setVisible(false);
            return;
        }

        for (TaggingPreset t : TaggingPreset.getMatchingPresets(types, tags, true)) {
            final JLabel lbl = createLabelForPreset(t);
            lbl.addMouseListener(new PresetLabelML(lbl, t, presetHandler));
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
