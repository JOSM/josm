// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;

/**
 * OSM primitive type.
 * @since 1670
 */
public enum OsmPrimitiveType {

    /** Node type */
    NODE(marktr(/* ICON(data/) */"node"), Node.class, NodeData.class),
    /** Way type */
    WAY(marktr(/* ICON(data/) */"way"), Way.class, WayData.class),
    /** Relation type */
    RELATION(marktr(/* ICON(data/) */"relation"), Relation.class, RelationData.class),

    /** Closed way: only for display, no real type */
    CLOSEDWAY(marktr(/* ICON(data/) */"closedway"), null, WayData.class),
    /** Multipolygon: only for display, no real type */
    MULTIPOLYGON(marktr(/* ICON(data/) */"multipolygon"), null, RelationData.class);

    private static final Collection<OsmPrimitiveType> DATA_VALUES = Arrays.asList(NODE, WAY, RELATION);

    private final String apiTypeName;
    private final Class<? extends OsmPrimitive> osmClass;
    private final Class<? extends PrimitiveData> dataClass;

    OsmPrimitiveType(String apiTypeName, Class<? extends OsmPrimitive> osmClass, Class<? extends PrimitiveData> dataClass) {
        this.apiTypeName = apiTypeName;
        this.osmClass = osmClass;
        this.dataClass = dataClass;
    }

    /**
     * Returns the API type name / JOSM display name.
     * @return the API type name / JOSM display name
     */
    public String getAPIName() {
        return apiTypeName;
    }

    /**
     * Returns the OSM class for data values, or null.
     * @return the OSM class for data values, or null
     */
    public Class<? extends OsmPrimitive> getOsmClass() {
        return osmClass;
    }

    /**
     * Returns the data class.
     * @return the data class
     */
    public Class<? extends PrimitiveData> getDataClass() {
        return dataClass;
    }

    /**
     * Returns enum value from API type name / JOSM display name, case sensitive.
     * @param typeName API type name / JOSM display name, case sensitive
     * @return matching enum value
     * @throws IllegalArgumentException if the type name does not match any valid type
     * @see #from(String)
     */
    public static OsmPrimitiveType fromApiTypeName(String typeName) {
        for (OsmPrimitiveType type : OsmPrimitiveType.values()) {
            if (type.getAPIName().equals(typeName)) return type;
        }
        throw new IllegalArgumentException(MessageFormat.format(
                "Parameter ''{0}'' is not a valid type name. Got ''{1}''.", "typeName", typeName));
    }

    /**
     * Determines the OSM primitive type of the given object.
     * @param obj the OSM object to inspect
     * @return the OSM primitive type of {@code obj}
     * @throws IllegalArgumentException if {@code obj} is null or of unknown type
     */
    public static OsmPrimitiveType from(IPrimitive obj) {
        if (obj instanceof INode) return NODE;
        if (obj instanceof IWay) return WAY;
        if (obj instanceof IRelation) return RELATION;
        throw new IllegalArgumentException("Unknown type: "+obj);
    }

    /**
     * Returns enum value from API type name / JOSM display name, case insensitive.
     * @param value API type name / JOSM display name, case insensitive
     * @return matching enum value or null
     * @see #fromApiTypeName
     */
    public static OsmPrimitiveType from(String value) {
        for (OsmPrimitiveType type: values()) {
            if (type.getAPIName().equalsIgnoreCase(value))
                return type;
        }
        return null;
    }

    /**
     * Returns the values matching real OSM API data types (node, way, relation).
     * @return the values matching real OSM API data types (node, way, relation)
     */
    public static Collection<OsmPrimitiveType> dataValues() {
        return DATA_VALUES;
    }

    /**
     * Constructs a new primitive instance (node, way or relation) without version.
     * @param uniqueId the unique id
     * @param allowNegative {@code true} to allow negative id
     * @return a new primitive instance (node, way or relation)
     * @throws IllegalArgumentException if uniqueId &lt; 0 and allowNegative is false
     */
    public OsmPrimitive newInstance(long uniqueId, boolean allowNegative) {
        switch (this) {
        case NODE:
            return new Node(uniqueId, allowNegative);
        case WAY:
            return new Way(uniqueId, allowNegative);
        case RELATION:
            return new Relation(uniqueId, allowNegative);
        default:
            throw new AssertionError();
        }
    }

    /**
     * Constructs a new primitive instance (node, way or relation) with given version.
     * @param id The id. Must be &gt;= 0
     * @param version The version
     * @return a new primitive instance (node, way or relation) with given version
     * @throws IllegalArgumentException if id &lt; 0
     * @since 12018
     */
    public OsmPrimitive newVersionedInstance(long id, int version) {
        switch (this) {
        case NODE:
            return new Node(id, version);
        case WAY:
            return new Way(id, version);
        case RELATION:
            return new Relation(id, version);
        default:
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        return tr(getAPIName());
    }
}
