// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.StyleSource;

public class Environment {
    
    OsmPrimitive osm;
    MultiCascade mc;
    String layer;
    StyleSource source;

    public Environment(OsmPrimitive osm, MultiCascade mc, String layer, StyleSource source) {
        this.osm = osm;
        this.mc = mc;
        this.layer = layer;
        this.source = source;
    }

    public Cascade getCascade() {
        return mc.getCascade(layer);
    }
}
