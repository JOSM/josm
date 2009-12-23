package org.openstreetmap.josm.gui.mappaint;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

abstract public class ElemStyle {
    // zoom range to display the feature
    public long minScale;
    public long maxScale;

    public int priority;
    public String code;
    Collection<Rule> rules = null;

    @Override
    public boolean equals(Object o) {
        return (o instanceof ElemStyle) && (((ElemStyle) o).getCode().equals(getCode()));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public String getCode()
    {
        if(code == null && rules != null)
        {
            code = "";
            for(Rule r: rules) {
                code += r.toCode();
            }
        }
        return code;
    }
    public boolean check(OsmPrimitive primitive)
    {
        if(rules == null)
            return true;
        for(Rule r : rules)
        {
            String k = primitive.get(r.key);
            String bv = OsmUtils.getNamedOsmBoolean(r.boolValue);
            if(k == null || (r.value != null && !k.equals(r.value))
                    || (bv != null && !bv.equals(OsmUtils.getNamedOsmBoolean(k))))
                return false;
        }
        return true;
    }

    public abstract void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected);
}
