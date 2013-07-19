// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;

/**
 * Enumeration of OSM primitive types associated with names and icons
 * @since 6068
 */
public enum TaggingPresetType {
    NODE("Mf_node", "node"), WAY("Mf_way", "way"), RELATION("Mf_relation", "relation"), CLOSEDWAY("Mf_closedway", "closedway");
    private final String iconName;
    private final String name;

    TaggingPresetType(String iconName, String name) {
        this.iconName = iconName;
        this.name = name;
    }

    public String getIconName() {
        return iconName;
    }

    public String getName() {
        return name;
    }

    public static TaggingPresetType forPrimitive(OsmPrimitive p) {
        return forPrimitiveType(p.getDisplayType());
    }

    public static TaggingPresetType forPrimitiveType(OsmPrimitiveType type) {
        if (type == OsmPrimitiveType.NODE) return NODE;
        if (type == OsmPrimitiveType.WAY) return WAY;
        if (type == OsmPrimitiveType.CLOSEDWAY) return CLOSEDWAY;
        if (type == OsmPrimitiveType.RELATION || type == OsmPrimitiveType.MULTIPOLYGON)
                return RELATION;
        throw new IllegalArgumentException("Unexpected primitive type: " + type);
    }

    public static TaggingPresetType fromString(String type) {
        for (TaggingPresetType t : TaggingPresetType.values()) {
            if (t.getName().equals(type)) {
                return t;
            }
        }
        return null;
    }

}
