// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;

/**
 * Invisible type allowing to hardcode an OSM key/value from the preset definition.
 */
public class Key extends KeyedItem {

    /** The hardcoded value for key */
    public String value;

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
        return false;
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
        changedTags.add(new Tag(key, value));
    }

    @Override
    public MatchType getDefaultMatch() {
        return MatchType.KEY_VALUE_REQUIRED;
    }

    @Override
    public Collection<String> getValues() {
        return Collections.singleton(value);
    }

    @Override
    public String toString() {
        return "Key [key=" + key + ", value=" + value + ", text=" + text
                + ", text_context=" + text_context + ", match=" + match
                + ']';
    }
}
