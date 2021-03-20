// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

import java.util.Arrays;
import java.util.Collection;

/**
 * Supporting class for creating the GUI for a preset item.
 *
 * @since xxx
 */
public class TaggingPresetItemGuiSupport {

    private final Collection<OsmPrimitive> selected;
    private final boolean presetInitiallyMatches;

    private TaggingPresetItemGuiSupport(boolean presetInitiallyMatches, Collection<OsmPrimitive> selected) {
        this.selected = selected;
        this.presetInitiallyMatches = presetInitiallyMatches;
    }

    /**
     * Returns the selected primitives
     *
     * @return the selected primitives
     */
    public Collection<OsmPrimitive> getSelected() {
        return selected;
    }

    /**
     * Returns whether the preset initially matched (before opening the dialog)
     *
     * @return whether the preset initially matched
     */
    public boolean isPresetInitiallyMatches() {
        return presetInitiallyMatches;
    }

    /**
     * Creates a new {@code TaggingPresetItemGuiSupport}
     *
     * @param selected the selected primitives
     * @param presetInitiallyMatches whether the preset initially matched
     * @return the new {@code TaggingPresetItemGuiSupport}
     */
    public static TaggingPresetItemGuiSupport create(boolean presetInitiallyMatches, Collection<OsmPrimitive> selected) {
        return new TaggingPresetItemGuiSupport(presetInitiallyMatches, selected);
    }

    /**
     * Creates a new {@code TaggingPresetItemGuiSupport}
     *
     * @param selected the selected primitives
     * @param presetInitiallyMatches whether the preset initially matched
     * @return the new {@code TaggingPresetItemGuiSupport}
     */
    public static TaggingPresetItemGuiSupport create(boolean presetInitiallyMatches, OsmPrimitive... selected) {
        return new TaggingPresetItemGuiSupport(presetInitiallyMatches, Arrays.asList(selected));
    }
}
