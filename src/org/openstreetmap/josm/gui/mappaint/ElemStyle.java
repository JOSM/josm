// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.gui.mappaint.xml.XmlCondition;

abstract public class ElemStyle {
    // zoom range to display the feature
    public long minScale;
    public long maxScale;

    public int priority;
    public String code;
    Collection<XmlCondition> conditions = null;

    @Override
    public boolean equals(Object o) {
        return (o instanceof ElemStyle) && (((ElemStyle) o).getCode().equals(getCode()));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCode() {
        if (code == null) {
            code = "";
            if (conditions != null) {
                for (XmlCondition c: conditions) {
                    code += c.toCode();
                }
            }
        }
        return code;
    }
    public boolean check(OsmPrimitive primitive)
    {
        if(conditions == null)
            return true;
        for(XmlCondition c : conditions)
        {
            String k = primitive.get(c.key);
            String bv = OsmUtils.getNamedOsmBoolean(c.boolValue);
            if(k == null || (c.value != null && !k.equals(c.value))
                    || (bv != null && !bv.equals(OsmUtils.getNamedOsmBoolean(k))))
                return false;
        }
        return true;
    }

    public abstract void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member);
}
