// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;

/**
 * This class stores the set of highlited primitives and
 * allows easy and fast change of highlighting
 */
public class HighlightHelper {
    Set<OsmPrimitive> highlightedPrimitives = new HashSet<OsmPrimitive>();
    
    /**
     * Highlight and remember given primitives
     * @param prims - primitives to highlight/unhighlight
     * @param flag - true to highlight
     */
    public void highlight(Collection <? extends OsmPrimitive> prims, boolean flag) {
        for (OsmPrimitive p: prims) {
            highlight(p, flag);
        }
    }
    
    /**
     * Highlight and remember given primitives, forgetting previously highlighted by this instance
     * @param prims - primitives to highlight/unhighlight
     */
    public void highlightOnly(Collection <? extends OsmPrimitive> prims) {
        clear();
        highlight(prims, true);
    }
    
    /**
     * Highlight and remember given primitive, forgetting previously highlighted by this instance
     * @param p - primitives to highlight/unhighlight
     */
    public void highlightOnly(OsmPrimitive p) {
        clear();
        highlight(p, true);
    }
    
    /**
     * Highlight and remember given primitive
     * @param prims - primitives to highlight/unhighlight
     * @param flag - true to highlight
     */
    public void highlight(OsmPrimitive p, boolean flag) {
        if (p instanceof Relation) {
            for (OsmPrimitive m: ((Relation) p).getMemberPrimitives()) {
                highlight(m, flag);
            }
        } else
        if (flag) {
            if (highlightedPrimitives.add(p)) {
                p.setHighlighted(true);
            }
        } else {
            if (highlightedPrimitives.remove(p)) {
                p.setHighlighted(false);
            }
        }
    }
    
    /**
     * Clear highlighting of all remembered primitives
     */
    public void clear() {
        for (OsmPrimitive p: highlightedPrimitives) {
            p.setHighlighted(false);
        }
        highlightedPrimitives.clear();
    }
    
    /**
     * Slow method to import all currently highlighted primitives into this instance
     */
    public void findAllHighligted() {
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds!=null) {
            highlightedPrimitives.addAll( ds.allNonDeletedPrimitives() );
        }
    }
    
    /**
     * Slow method to import all currently highlighted primitives into this instance
     */
    public static void clearAllHighligted() {
        DataSet ds = Main.main.getCurrentDataSet();
        if (ds!=null) {
            for (OsmPrimitive p: ds.allNonDeletedPrimitives()) {
                p.setHighlighted(false);
            }
        }
    }
}
