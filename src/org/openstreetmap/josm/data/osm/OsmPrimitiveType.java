// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;
import static org.openstreetmap.josm.tools.I18n.tr;

public enum OsmPrimitiveType {

    NODE ("node"),
    WAY  ("way"),
    RELATION ("relation");

    private String apiTypeName;

    OsmPrimitiveType(String apiTypeName) {
        this.apiTypeName = apiTypeName;
    }

    public String getAPIName() {
        return apiTypeName;
    }

    public static OsmPrimitiveType fromApiTypeName(String typeName) {
        for (OsmPrimitiveType type : OsmPrimitiveType.values()) {
            if (type.getAPIName().equals(typeName)) return type;
        }
        throw new IllegalArgumentException(tr("parameter ''{0}'' is not a valid type name, got ''{1}''", "typeName", typeName));
    }

    public static OsmPrimitiveType from(OsmPrimitive obj) {
        return from(obj.getClass());
    }

    public static OsmPrimitiveType from(Class cls) {
        if (cls.equals(Node.class)) return NODE;
        if (cls.equals(Way.class)) return WAY;
        if (cls.equals(Relation.class)) return RELATION;
        throw new IllegalArgumentException(tr("parameter ''{0}'' is not an acceptable class, got ''{1}''", "cls", cls.toString()));
    }

    public static OsmPrimitiveType from(String value) {
        if (value == null) return null;
        for (OsmPrimitiveType type: values()){
            if (type.getAPIName().equalsIgnoreCase(value))
                return type;
        }
        return null;
    }

}
