// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.gui.DefaultNameFormatter;

/** Comparator, comparing by type and objects display names */
public class OsmPrimitiveComparator implements Comparator<OsmPrimitive> {
    final private Map<OsmPrimitive, String> cache= new HashMap<OsmPrimitive, String>();
    final private DefaultNameFormatter df = DefaultNameFormatter.getInstance();
    public boolean relationsFirst = false;

    private String cachedName(OsmPrimitive p) {
        String name = cache.get(p);
        if (name == null) {
            name = p.getDisplayName(df);
            cache.put(p, name);
        }
        return name;
    }

    private int compareName(OsmPrimitive a, OsmPrimitive b) {
        String an = cachedName(a);
        String bn = cachedName(b);
        // make sure display names starting with digits are the end of the
        // list
        if (Character.isDigit(an.charAt(0)) && Character.isDigit(bn.charAt(0)))
            return an.compareTo(bn);
        else if (Character.isDigit(an.charAt(0)) && !Character.isDigit(bn.charAt(0)))
            return 1;
        else if (!Character.isDigit(an.charAt(0)) && Character.isDigit(bn.charAt(0)))
            return -1;
        return an.compareTo(bn);
    }

    private int compareType(OsmPrimitive a, OsmPrimitive b) {
        if(relationsFirst) {
            // show relations before ways, then nodes
            if (a.getType().equals(OsmPrimitiveType.RELATION)) return -1;
            if (a.getType().equals(OsmPrimitiveType.NODE)) return 1;
            // a is a way
            if (b.getType().equals(OsmPrimitiveType.RELATION)) return 1;
            // b is a node
        } else {
            // show ways before relations, then nodes
            if (a.getType().equals(OsmPrimitiveType.WAY)) return -1;
            if (a.getType().equals(OsmPrimitiveType.NODE)) return 1;
            // a is a relation
            if (b.getType().equals(OsmPrimitiveType.WAY)) return 1;
            // b is a node
        }
        return -1;
    }

    @Override
    public int compare(OsmPrimitive a, OsmPrimitive b) {
        if (a.getType().equals(b.getType()))
            return compareName(a, b);
        return compareType(a, b);
    }
}
