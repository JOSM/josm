// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * This class stores the set of highlighted primitives and
 * allows easy and fast change of highlighting.
 */
public class HighlightHelper {
    private final Set<OsmPrimitive> highlightedPrimitives = new HashSet<>();

    /**
     * Highlight and remember given primitives
     * @param prims - primitives to highlight/unhighlight
     * @return {@code true} if a repaint is needed
     */
    public boolean highlight(Collection<? extends OsmPrimitive> prims) {
        return highlight(prims, false);
    }

    /**
     * Highlight and remember given primitives
     * @param prims - primitives to highlight/unhighlight
     * @param only - remove previous highlighting
     * @return {@code true} if a repaint is needed
     */
    public boolean highlight(Collection<? extends OsmPrimitive> prims, boolean only) {
        boolean needsRepaint = false;
        if (only) {
            Iterator<OsmPrimitive> it = highlightedPrimitives.iterator();
            while (it.hasNext()) {
                OsmPrimitive p = it.next();
                if (!prims.contains(p)) {
                    p.setHighlighted(false);
                    it.remove();
                    needsRepaint = true;
                }
            }
        }
        for (OsmPrimitive p: prims) {
            needsRepaint |= setHighlight(p, true);
        }

        return needsRepaint;
    }

    /**
     * Highlight and remember given primitives, forgetting previously highlighted by this instance
     * @param prims - primitives to highlight/unhighlight
     * @return {@code true} if a repaint is needed
     */
    public boolean highlightOnly(Collection<? extends OsmPrimitive> prims) {
        return highlight(prims, true);
    }

    /**
     * Highlight and remember given primitive, forgetting previously highlighted by this instance
     * @param p - primitives to highlight/unhighlight
     * @return {@code true} if a repaint is needed
     */
    public boolean highlightOnly(OsmPrimitive p) {
        return highlight(Collections.singleton(p), true);
    }

    /**
     * Highlight and remember given primitive
     * @param p - primitive to highlight/unhighlight
     * @param flag - true to highlight
     * @return {@code true} if a repaint is needed
     */
    public boolean setHighlight(OsmPrimitive p, boolean flag) {
        return setHighlight(p, flag, new HashSet<Relation>());
    }

    private boolean setHighlight(OsmPrimitive p, boolean flag, Set<Relation> seenRelations) {
        if (p instanceof Relation) {
            Relation r = (Relation) p;
            seenRelations.add(r);
            boolean needRepaint = false;
            for (OsmPrimitive m : r.getMemberPrimitivesList()) {
                if (!(m instanceof Relation) || !seenRelations.contains(m)) {
                    needRepaint |= setHighlight(m, flag, seenRelations);
                }
            }
            return needRepaint;
        } else if (flag) {
            if (highlightedPrimitives.add(p)) {
                p.setHighlighted(true);
                return true;
            }
        } else {
            if (highlightedPrimitives.remove(p)) {
                p.setHighlighted(false);
                return true;
            }
        }
        return false;
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
    public void findAllHighlighted() {
        DataSet ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds != null) {
            highlightedPrimitives.addAll(ds.allNonDeletedPrimitives());
        }
    }

    /**
     * Slow method to remove highlights from all primitives
     */
    public static void clearAllHighlighted() {
        DataSet ds = MainApplication.getLayerManager().getActiveDataSet();
        if (ds != null) {
            for (OsmPrimitive p: ds.allNonDeletedPrimitives()) {
                p.setHighlighted(false);
            }
        }
    }
}
