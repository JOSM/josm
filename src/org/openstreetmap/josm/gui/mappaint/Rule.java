package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmUtils;

public class Rule
{
    String key;
    String value;
    String boolValue;

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

}



