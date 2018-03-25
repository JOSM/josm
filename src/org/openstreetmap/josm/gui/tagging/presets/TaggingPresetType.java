// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * Enumeration of OSM primitive types associated with names and icons
 * @since 6068
 */
public enum TaggingPresetType {
    /** Node */
    NODE(/* ICON */ "Mf_node", "node"),
    /** Way */
    WAY(/* ICON */ "Mf_way", "way"),
    /** Relation */
    RELATION(/* ICON */ "Mf_relation", "relation"),
    /** Closed way */
    CLOSEDWAY(/* ICON */ "Mf_closedway", "closedway"),
    /** Multipolygon */
    MULTIPOLYGON(/* ICON */ "Mf_multipolygon", "multipolygon");
    private final String iconName;
    private final String name;

    TaggingPresetType(String iconName, String name) {
        this.iconName = iconName + ".svg";
        this.name = name;
    }

    /**
     * Replies the SVG icon name.
     * @return the SVG icon name
     */
    public String getIconName() {
        return iconName;
    }

    /**
     * Replies the name, as used in XML presets.
     * @return the name: "node", "way", "relation" or "closedway"
     */
    public String getName() {
        return name;
    }

    /**
     * Determines the {@code TaggingPresetType} of a given primitive.
     * @param p The OSM primitive
     * @return the {@code TaggingPresetType} of {@code p}
     */
    public static TaggingPresetType forPrimitive(IPrimitive p) {
        return forPrimitiveType(p.getDisplayType());
    }

    /**
     * Determines the {@code TaggingPresetType} of a given primitive type.
     * @param type The OSM primitive type
     * @return the {@code TaggingPresetType} of {@code type}
     */
    public static TaggingPresetType forPrimitiveType(OsmPrimitiveType type) {
        if (type == OsmPrimitiveType.NODE)
            return NODE;
        if (type == OsmPrimitiveType.WAY)
            return WAY;
        if (type == OsmPrimitiveType.CLOSEDWAY)
            return CLOSEDWAY;
        if (type == OsmPrimitiveType.MULTIPOLYGON)
            return MULTIPOLYGON;
        if (type == OsmPrimitiveType.RELATION)
            return RELATION;
        throw new IllegalArgumentException("Unexpected primitive type: " + type);
    }

    /**
     * Determines the {@code TaggingPresetType} from a given string.
     * @param type The OSM primitive type as string ("node", "way", "relation" or "closedway")
     * @return the {@code TaggingPresetType} from {@code type}
     */
    public static TaggingPresetType fromString(String type) {
        for (TaggingPresetType t : TaggingPresetType.values()) {
            if (t.getName().equals(type)) {
                return t;
            }
        }
        return null;
    }
}
