// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import org.openstreetmap.josm.data.osm.OsmUtils;

public class XmlCondition
{
    public String key;
    public String value;
    public String boolValue;

    public XmlCondition()
    {
      init();
    }
    public XmlCondition(XmlCondition c)
    {
      key = c.key;
      value = c.value;
      boolValue = c.boolValue;
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

    public void appendCode(StringBuilder sb)
    {
        sb.append("[k=").append(key);

        if (boolValue != null)
            sb.append(",b=").append(boolValue);
        else
            sb.append(",v=").append(value);

        sb.append("]");
    }
}
