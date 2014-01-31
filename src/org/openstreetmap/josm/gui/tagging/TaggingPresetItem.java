// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;

/**
 * Class that represents single part of a preset - one field or text label that is shown to user
 * @since 6068
 */
public abstract class TaggingPresetItem {

    protected void initAutoCompletionField(AutoCompletingTextField field, String key) {
        if (Main.main == null) return;
        OsmDataLayer layer = Main.main.getEditLayer();
        if (layer == null) {
            return;
        }
        AutoCompletionList list = new AutoCompletionList();
        layer.data.getAutoCompletionManager().populateWithTagValues(list, key);
        field.setAutoCompletionList(list);
    }

    /**
     * Called by {@link TaggingPreset#createPanel} during tagging preset panel creation.
     * All components defining this tagging preset item must be added to given panel.
     *
     * @param p The panel where components must be added
     * @param sel The related selected OSM primitives
     * @param presetInitiallyMatches Whether this {@link TaggingPreset} already matched before applying,
     *                               i.e. whether the map feature already existed on the primitive.
     * @return {@code true} if this item adds semantic tagging elements, {@code false} otherwise.
     */
    abstract boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches);

    /**
     * Adds the new tags to apply to selected OSM primitives when the preset holding this item is applied.
     * @param changedTags The list of changed tags to modify if needed
     */
    abstract void addCommands(List<Tag> changedTags);

    boolean requestFocusInWindow() {
        return false;
    }

    /**
     * Tests whether the tags match this item.
     * Note that for a match, at least one positive and no negative is required.
     * @param tags the tags of an {@link OsmPrimitive}
     * @return {@code true} if matches (positive), {@code null} if neutral, {@code false} if mismatches (negative).
     */
    Boolean matches(Map<String, String> tags) {
        return null;
    }
}
