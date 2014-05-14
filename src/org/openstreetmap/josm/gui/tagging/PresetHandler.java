// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;

import java.util.Collection;
import java.util.List;

public interface PresetHandler {
    Collection<OsmPrimitive> getSelection();
    void updateTags(List<Tag> tags);
}
