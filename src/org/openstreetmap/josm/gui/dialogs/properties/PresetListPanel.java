// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Check;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Combo;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.PresetType;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Text;
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
    private static class PresetLabelML implements MouseListener {
        final JLabel label;
        final Font bold;
        final Font normal;
        final TaggingPreset tag;
        final PresetHandler presetHandler;

        PresetLabelML(JLabel lbl, TaggingPreset t, PresetHandler presetHandler) {
            super();
            label = lbl;
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            normal = label.getFont();
            bold = normal.deriveFont(normal.getStyle() ^ Font.BOLD);
            tag = t;
            this.presetHandler = presetHandler;
        }
        public void mouseClicked(MouseEvent arg0) {
            Collection<OsmPrimitive> selection = presetHandler.getSelection();
            if (selection == null || selection.isEmpty())
                return;
            int answer = tag.showDialog(selection, false);

            if (answer == TaggingPreset.DIALOG_ANSWER_APPLY) {
                presetHandler.updateTags(tag.getChangedTags());
            }

        }
        public void mouseEntered(MouseEvent arg0) {
            label.setFont(bold);
        }
        public void mouseExited(MouseEvent arg0) {
            label.setFont(normal);
        }
        public void mousePressed(MouseEvent arg0) {}
        public void mouseReleased(MouseEvent arg0) {}
    }

    public void updatePresets(int nodes, int ways, int relations, int closedways, Map<String, Map<String, Integer>> valueCount, PresetHandler presetHandler)  {

        removeAll();
        int total = nodes+ways+relations+closedways;
        if(total == 0) {
            setVisible(false);
            return;
        }

        for(TaggingPreset t : TaggingPresetPreference.taggingPresets) {
            if(
                    (       t.types == null
                            || (relations > 0 && t.types.contains(PresetType.RELATION))
                            || (nodes > 0 && t.types.contains(PresetType.NODE))
                            || (ways+closedways > 0 && t.types.contains(PresetType.WAY))
                            || (closedways > 0 && t.types.contains(PresetType.CLOSEDWAY))
                    )
                    && t.isShowable())
            {
                int found = 0;
                for(TaggingPreset.Item i : t.data) {
                    if(i instanceof TaggingPreset.Key) {
                        String val = ((TaggingPreset.Key)i).value;
                        String key = ((TaggingPreset.Key)i).key;
                        // we subtract 100 if not found and add 1 if found
                        found -= 100;
                        if(key == null || !valueCount.containsKey(key)) {
                            continue;
                        }

                        Map<String, Integer> v = valueCount.get(key);
                        if(v.size() == 1 && val != null && v.containsKey(val) && v.get(val) == total) {
                            found += 101;
                        }
                    } else {
                        String key = null;
                        if ((i instanceof Text) && ((Text)i).required) {
                            key = ((Text)i).key;
                        } else if ((i instanceof Combo) && ((Combo)i).required) {
                            key = ((Combo)i).key;
                        } else if ((i instanceof Check) && ((Check)i).required) {
                            key = ((Check)i).key;
                        }
                        if (key != null && valueCount.get(key) == null) {
                            found -= 100;
                        }
                    }
                }

                if(found <= 0) {
                    continue;
                }

                JLabel lbl = new JLabel(t.getName());
                lbl.addMouseListener(new PresetLabelML(lbl, t, presetHandler));
                add(lbl, GBC.eol().fill(GBC.HORIZONTAL));
            }
        }

        if(getComponentCount() > 0) {
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
