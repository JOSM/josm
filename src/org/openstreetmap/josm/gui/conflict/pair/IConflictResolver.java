// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public interface IConflictResolver {

    void deletePrimitive(boolean deleted);
    void populate(OsmPrimitive my, OsmPrimitive their);

}
