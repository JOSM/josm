// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;

/**
 * This interface needs to be implemented in order to display a tagging preset. It allows the preset dialog to query the primitives it should
 * be displayed for and modify them.
 */
public interface TaggingPresetHandler {
    /**
     * Gets the selection the preset should be applied to.
     * @return A collection of primitives.
     */
    Collection<OsmPrimitive> getSelection();

    /**
     * Update the given tags on the selection.
     * @param tags The tags to update.
     */
    void updateTags(List<Tag> tags);
}
