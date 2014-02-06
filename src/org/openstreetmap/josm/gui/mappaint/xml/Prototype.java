// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.gui.mappaint.Range;

abstract public class Prototype {
    // zoom range to display the feature
    public Range range;

    public int priority;
    public String code;
    public Collection<XmlCondition> conditions = null;

    public Prototype(Range range) {
        this.range = range;
    }

    public Prototype() {
    }

    public String getCode() {
        if(code == null) {
            if (conditions == null || conditions.isEmpty()) {
                code = "";
            } else {
                final StringBuilder sb = new StringBuilder();
                for(XmlCondition r: conditions) {
                    r.appendCode(sb);
                }
                code = sb.toString();
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

            if (k == null || (r.value != null && !k.equals(r.value)))
                return false;

            String bv = OsmUtils.getNamedOsmBoolean(r.boolValue);

            if (bv != null && !bv.equals(OsmUtils.getNamedOsmBoolean(k)))
                return false;
        }
        return true;
    }

}