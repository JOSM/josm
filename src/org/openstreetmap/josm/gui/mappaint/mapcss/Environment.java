// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;

public class Environment {
    
    OsmPrimitive osm;
    MultiCascade mc;
    String layer;

    public Environment(OsmPrimitive osm, MultiCascade mc, String layer) {
        this.osm = osm;
        this.mc = mc;
        this.layer = layer;
    }

    public Cascade getCascade() {
        return mc.getCascade(layer);
    }
}
