package org.openstreetmap.josm.gui.mappaint;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;

abstract public class ElemStyle
{
    // zoom range to display the feature
    public long minScale;
    public long maxScale;

    public int priority;
    public String code;
    Collection<Rule> rules = null;

    public Boolean equals(ElemStyle s)
    {
        return s != null && s.getCode().equals(getCode());
    }
    public String getCode()
    {
        if(code == null && rules != null)
        {
            code = "";
            for(Rule r: rules)
                code += r.toCode();
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
}
