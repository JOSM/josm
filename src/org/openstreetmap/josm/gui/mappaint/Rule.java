package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmUtils;

public class Rule
{
    String key;
    String value;
    String boolValue;

    public Rule()
    {
      init();
    }
    public Rule(Rule r)
    {
      key = r.key;
      value = r.value;
      boolValue = r.boolValue;
    }
    public String getKey()
    {
        if(value != null)
            return "n" + key + "=" + value;
        else if(boolValue != null)
            return "b" + key  + "=" + OsmUtils.getNamedOsmBoolean(boolValue);
        else
            return "x" + key;
    }
    public void init()
    {
      key = value = boolValue = null;
    }

    public String toString()
    {
      return "Rule["+key+","+(boolValue != null ? "b="+boolValue:"v="+value)+"]";
    }
    public String toCode()
    {
      return "[k="+key+(boolValue != null ? ",b="+boolValue:",v="+value)+"]";
    }
}
