// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetLabel;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

public class PresetLink extends TaggingPresetItem {

    public String preset_name = "";

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
        final String presetName = preset_name;
        final TaggingPreset t = Utils.filter(TaggingPresets.getTaggingPresets(), new Predicate<TaggingPreset>() {
            @Override
            public boolean evaluate(TaggingPreset object) {
                return presetName.equals(object.name);
            }
        }).iterator().next();
        if (t == null) return false;
        JLabel lbl = new TaggingPresetLabel(t);
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
                t.actionPerformed(null);
            }
        });
        p.add(lbl, GBC.eol().fill(GBC.HORIZONTAL));
        return false;
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
    }
}
