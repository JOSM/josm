// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;
import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.ImageIcon;

public enum OsmPrimitiveType {

    NODE ("node", tr("node"), tr("nodes")),
    WAY  ("way", tr("way"), tr("ways")),
    RELATION ("relation", tr("relation"), tr("relations")),
    CHANGESET ("changeset", tr("changeset"), tr("changesets"));

    private String apiTypeName;
    private String localizedDisplayNameSingular;
    private String localizedDisplayNamePlural;

    OsmPrimitiveType(String apiTypeName, String localizedDisplayNameSingular, String localizedDisplayNamePlural) {
        this.apiTypeName = apiTypeName;
        this.localizedDisplayNameSingular = localizedDisplayNameSingular;
        this.localizedDisplayNamePlural = localizedDisplayNamePlural;
    }

    public String getAPIName() {
        return apiTypeName;
    }

    public String getLocalizedDisplayNameSingular() {
        return localizedDisplayNameSingular;
    }

    public String getLocalizedDisplayNamePlural() {
        return localizedDisplayNamePlural;
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
        if (cls.equals(Changeset.class)) return CHANGESET;
        throw new IllegalArgumentException(tr("parameter ''{0}'' is not an acceptable class, got ''{1}''", "cls", cls.toString()));
    }

}
