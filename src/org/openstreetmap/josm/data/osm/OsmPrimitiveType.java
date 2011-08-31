// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;

public enum OsmPrimitiveType {

    NODE (marktr(/* ICON(data/) */"node"), Node.class, NodeData.class),
    WAY  (marktr(/* ICON(data/) */"way"), Way.class, WayData.class),
    RELATION (marktr(/* ICON(data/) */"relation"), Relation.class, RelationData.class),

    /* only for display, no real type */
    CLOSEDWAY  (marktr(/* ICON(data/) */"closedway"), null, WayData.class),
    MULTIPOLYGON (marktr(/* ICON(data/) */"multipolygon"), null, RelationData.class);

    private final static Collection<OsmPrimitiveType> DATA_VALUES = Arrays.asList(NODE, WAY, RELATION);

    private final String apiTypeName;
    private final Class<? extends OsmPrimitive> osmClass;
    private final Class<? extends PrimitiveData> dataClass;

    OsmPrimitiveType(String apiTypeName, Class<? extends OsmPrimitive> osmClass, Class<? extends PrimitiveData> dataClass) {
        this.apiTypeName = apiTypeName;
        this.osmClass = osmClass;
        this.dataClass = dataClass;
    }

    public String getAPIName() {
        return apiTypeName;
    }

    public Class<? extends OsmPrimitive> getOsmClass() {
        return osmClass;
    }

    public Class<? extends PrimitiveData> getDataClass() {
        return dataClass;
    }

    public static OsmPrimitiveType fromApiTypeName(String typeName) {
        for (OsmPrimitiveType type : OsmPrimitiveType.values()) {
            if (type.getAPIName().equals(typeName)) return type;
        }
        throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' is not a valid type name. Got ''{1}''.", "typeName", typeName));
    }

    public static OsmPrimitiveType from(IPrimitive obj) {
        if (obj instanceof INode) return NODE;
        if (obj instanceof IWay) return WAY;
        if (obj instanceof IRelation) return RELATION;
        throw new IllegalArgumentException();
    }

    public static OsmPrimitiveType from(String value) {
        if (value == null) return null;
        for (OsmPrimitiveType type: values()){
            if (type.getAPIName().equalsIgnoreCase(value))
                return type;
        }
        return null;
    }

    public static Collection<OsmPrimitiveType> dataValues() {
        return DATA_VALUES;
    }

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

    @Override
    public String toString() {
        return tr(getAPIName());
    }
}
