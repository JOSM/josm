// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class Environment {
    
    public OsmPrimitive osm;
    public MultiCascade mc;
    public String layer;
    public StyleSource source;

    public Environment(OsmPrimitive osm, MultiCascade mc, String layer, StyleSource source) {
        this.osm = osm;
        this.mc = mc;
        this.layer = layer;
        this.source = source;
    }

}
