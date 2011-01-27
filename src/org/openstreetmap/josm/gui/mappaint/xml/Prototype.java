// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;

abstract public class Prototype {
    // zoom range to display the feature
    public long minScale;
    public long maxScale;

    public int priority;
    public String code;
    public Collection<XmlCondition> conditions = null;

    public Prototype(long maxScale, long minScale) {
        this.maxScale = maxScale;
        this.minScale = minScale;
    }

    public Prototype() {
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Prototype) && (((Prototype) o).getCode().equals(getCode()));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCode() {
        if(code == null) {
            code = "";
            if (conditions != null) {
                for(XmlCondition r: conditions) {
                    code += r.toCode();
                }
            }
        }
        return code;
    }

    public boolean check(OsmPrimitive primitive)
    {
        if(conditions == null)
            return true;
        for(XmlCondition r : conditions)
        {
            String k = primitive.get(r.key);
            String bv = OsmUtils.getNamedOsmBoolean(r.boolValue);
            if(k == null || (r.value != null && !k.equals(r.value))
                    || (bv != null && !bv.equals(OsmUtils.getNamedOsmBoolean(k))))
                return false;
        }
        return true;
    }

}