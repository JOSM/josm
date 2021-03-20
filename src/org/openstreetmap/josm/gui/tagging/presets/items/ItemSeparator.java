// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.tools.GBC;

/**
 * Class used to represent a {@link JSeparator} inside tagging preset window.
 * @since 6198
 */
public class ItemSeparator extends TaggingPresetItem {

    @Override
    public boolean addToPanel(JPanel p, TaggingPresetItemGuiSupport support) {
        p.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 0, 5));
        return false;
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
        // Do nothing
    }

    @Override
    public String toString() {
        return "ItemSeparator";
    }
}
