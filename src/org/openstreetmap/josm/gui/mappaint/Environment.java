// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;

public class Environment {

    public OsmPrimitive osm;
    public MultiCascade mc;
    public String layer;
    public StyleSource source;

    /**
     * <p>When matching a child selector, the matching referrers will be stored.
     * They can be accessed in {@link Instruction#execute(Environment)} to access
     * tags from parent objects.</p>
     */
    private List<OsmPrimitive> matchingReferrers = null;

    public Environment(OsmPrimitive osm, MultiCascade mc, String layer, StyleSource source) {
        this.osm = osm;
        this.mc = mc;
        this.layer = layer;
        this.source = source;
    }

    public Collection<OsmPrimitive> getMatchingReferrers() {
        return matchingReferrers;
    }

    public void setMatchingReferrers(List<OsmPrimitive> refs) {
        matchingReferrers = refs;
    }

    public void clearMatchingReferrers() {
        matchingReferrers = null;
    }
}
