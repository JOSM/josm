// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;

import org.openstreetmap.josm.gui.DefaultNameFormatter;

/**
 * Comparator, comparing pritimives by:<ul>
 * <li>type and ids in "quick" mode</li>
 * <li>type and objects display names instead</li>
 * </ul>
 * @since 4113
 */
public class OsmPrimitiveComparator implements Comparator<OsmPrimitive>, Serializable {

    private static final long serialVersionUID = 1L;

    private final HashMap<OsmPrimitive, String> cache = new HashMap<>();
    private final boolean relationsFirst;
    private final boolean quick;

    /**
     * Constructs a new {@code OsmPrimitiveComparator}.
     */
    public OsmPrimitiveComparator() {
        this(false, false);
    }

    /**
     * Constructs a new {@code OsmPrimitiveComparator}.
     * @param quick if {@code true}, sorts by type and ids (fast), otherwise sort by type and display names (slower)
     * @param relationsFirst if {@code true}, always list relations first
     */
    public OsmPrimitiveComparator(boolean quick, boolean relationsFirst) {
        this.quick = quick;
        this.relationsFirst = relationsFirst;
    }

    private String cachedName(OsmPrimitive p) {
        String name = cache.get(p);
        if (name == null) {
            name = p.getDisplayName(DefaultNameFormatter.getInstance());
            cache.put(p, name);
        }
        return name;
    }

    private int compareName(OsmPrimitive a, OsmPrimitive b) {
        String an = cachedName(a);
        String bn = cachedName(b);
        // make sure display names starting with digits are the end of the list
        if (Character.isDigit(an.charAt(0)) && Character.isDigit(bn.charAt(0)))
            return an.compareTo(bn);
        else if (Character.isDigit(an.charAt(0)) && !Character.isDigit(bn.charAt(0)))
            return 1;
        else if (!Character.isDigit(an.charAt(0)) && Character.isDigit(bn.charAt(0)))
            return -1;
        return an.compareTo(bn);
    }

    private static int compareId(OsmPrimitive a, OsmPrimitive b) {
        long idA = a.getUniqueId();
        long idB = b.getUniqueId();
        if (idA < idB) return -1;
        if (idA > idB) return 1;
        return 0;
    }

    private int compareType(OsmPrimitive a, OsmPrimitive b) {
        if (relationsFirst) {
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
            return quick ? compareId(a, b) : compareName(a, b);
        return compareType(a, b);
    }
}
