// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;

public class ElemStyles
{
    HashMap<String, StyleSource> styleSet;
    public ElemStyles()
    {
        styleSet = new HashMap<String, StyleSource>();
    }

    public void add(String name, Rule r, Collection<Rule> rules, LineElemStyle style)
    {
        if(rules != null)
        {
            style.rules = rules;
            getStyleSet(name, true).linesList.add(style);
        }
        else
        {
            String key = r.getKey();
            style.code = key;
            getStyleSet(name, true).lines.put(key, style);
        }
    }

    public void addModifier(String name, Rule r, Collection<Rule> rules, LineElemStyle style)
    {
        if(rules != null)
        {
            style.rules = rules;
            getStyleSet(name, true).modifiersList.add(style);
        }
        else
        {
            String key = r.getKey();
            style.code = key;
            getStyleSet(name, true).modifiers.put(key, style);
        }
    }

    public void add(String name, Rule r, Collection<Rule> rules, AreaElemStyle style)
    {
        if(rules != null)
        {
            style.rules = rules;
            getStyleSet(name, true).areasList.add(style);
        }
        else
        {
            String key = r.getKey();
            style.code = key;
            getStyleSet(name, true).areas.put(key, style);
        }
    }

    public void add(String name, Rule r, Collection<Rule> rules, IconElemStyle style)
    {
        if(rules != null)
        {
            style.rules = rules;
            getStyleSet(name, true).iconsList.add(style);
        }
        else
        {
            String key = r.getKey();
            style.code = key;
            getStyleSet(name, true).icons.put(key, style);
        }
    }

    private StyleSource getStyleSet(String name, boolean create)
    {
        if(name == null) {
            name = Main.pref.get("mappaint.style", "standard");
        }

        StyleSource s = styleSet.get(name);
        if(create && s == null)
        {
            s = new StyleSource();
            styleSet.put(name, s);
        }
        return s;
    }

    /* called from class users, never return null */
    public StyleSource getStyleSet()
    {
        return getStyleSet(null, true);
    }

    public Collection<String> getStyleNames()
    {
        return styleSet.keySet();
    }
}
