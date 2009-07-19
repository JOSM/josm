// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class PrimitiveNameFormatter {
    public String getName(OsmPrimitive primitive) {
        String name = primitive.getName();
        if (Main.pref.getBoolean("osm-primitives.showid")) {
            name += tr(" [id: {0}]", primitive.id);
        }
        return name;
    }
}
