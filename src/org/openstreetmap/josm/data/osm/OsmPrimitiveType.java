// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;
import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.MessageFormat;

public enum OsmPrimitiveType {

    NODE (marktr("node"), Node.class, NodeData.class),
    WAY  (marktr("way"), Way.class, WayData.class),
    RELATION (marktr("relation"), Relation.class, RelationData.class);

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

    public static OsmPrimitiveType from(OsmPrimitive obj) {
        return from(obj.getClass());
    }

    public static OsmPrimitiveType from(Class<? extends OsmPrimitive> cls) {
        if (cls.equals(Node.class)) return NODE;
        if (cls.equals(Way.class)) return WAY;
        if (cls.equals(Relation.class)) return RELATION;
        throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' is not an acceptable class. Got ''{1}''.", "cls", cls.toString()));
    }

    public static OsmPrimitiveType fromData(Class<? extends PrimitiveData> cls) {
        if (cls.equals(NodeData.class)) return NODE;
        if (cls.equals(WayData.class)) return WAY;
        if (cls.equals(RelationData.class)) return RELATION;
        throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' is not an acceptable class. Got ''{1}''.", "cls", cls.toString()));
    }

    public static OsmPrimitiveType fromData(PrimitiveData data) {
        return fromData(data.getClass());
    }

    public static OsmPrimitiveType from(String value) {
        if (value == null) return null;
        for (OsmPrimitiveType type: values()){
            if (type.getAPIName().equalsIgnoreCase(value))
                return type;
        }
        return null;
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
