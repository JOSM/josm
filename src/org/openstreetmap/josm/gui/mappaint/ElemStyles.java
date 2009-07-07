package org.openstreetmap.josm.gui.mappaint;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.Main;

public class ElemStyles
{
    public class StyleSet {
        private HashMap<String, IconElemStyle> icons;
        private HashMap<String, LineElemStyle> lines;
        private HashMap<String, AreaElemStyle> areas;
        private HashMap<String, LineElemStyle> modifiers;
        public StyleSet()
        {
            icons = new HashMap<String, IconElemStyle>();
            lines = new HashMap<String, LineElemStyle>();
            modifiers = new HashMap<String, LineElemStyle>();
            areas = new HashMap<String, AreaElemStyle>();
        }
        private IconElemStyle getNode(Map<String, String> keys)
        {
            IconElemStyle ret = null;
            Iterator<String> iterator = keys.keySet().iterator();
            while(iterator.hasNext())
            {
                String key = iterator.next();
                String val = keys.get(key);
                IconElemStyle style;
                if((style = icons.get("n" + key + "=" + val)) != null)
                {
                    if((ret == null || style.priority > ret.priority) && style.check(keys))
                        ret = style;
                }
                if((style = icons.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))) != null)
                {
                    if((ret == null || style.priority > ret.priority) && style.check(keys))
                        ret = style;
                }
                if((style = icons.get("x" + key)) != null)
                {
                    if((ret == null || style.priority > ret.priority) && style.check(keys))
                        ret = style;
                }
            }
            return ret;
        }
        private ElemStyle get(Map<String, String> keys, boolean noclosed)
        {
            AreaElemStyle retArea = null;
            LineElemStyle retLine = null;
            String linestring = null;
            HashMap<String, LineElemStyle> over = new HashMap<String, LineElemStyle>();
            Iterator<String> iterator = keys.keySet().iterator();
            while(iterator.hasNext())
            {
                String key = iterator.next();
                String val = keys.get(key);
                AreaElemStyle styleArea;
                LineElemStyle styleLine;
                String idx = "n" + key + "=" + val;
                if((styleArea = areas.get(idx)) != null && (retArea == null
                || styleArea.priority > retArea.priority) && (!noclosed
                || !styleArea.closed) && styleArea.check(keys))
                    retArea = styleArea;
                if((styleLine = lines.get(idx)) != null && (retLine == null
                || styleLine.priority > retLine.priority) && styleLine.check(keys))
                {
                    retLine = styleLine;
                    linestring = idx;
                }
                if((styleLine = modifiers.get(idx)) != null && styleLine.check(keys))
                    over.put(idx, styleLine);
                idx = "b" + key + "=" + OsmUtils.getNamedOsmBoolean(val);
                if((styleArea = areas.get(idx)) != null && (retArea == null
                || styleArea.priority > retArea.priority) && (!noclosed
                || !styleArea.closed) && styleArea.check(keys))
                    retArea = styleArea;
                if((styleLine = lines.get(idx)) != null && (retLine == null
                || styleLine.priority > retLine.priority) && styleLine.check(keys))
                {
                    retLine = styleLine;
                    linestring = idx;
                }
                if((styleLine = modifiers.get(idx)) != null && styleLine.check(keys))
                    over.put(idx, styleLine);
                idx = "x" + key;
                if((styleArea = areas.get(idx)) != null && (retArea == null
                || styleArea.priority > retArea.priority) && (!noclosed
                || !styleArea.closed) && styleArea.check(keys))
                    retArea = styleArea;
                if((styleLine = lines.get(idx)) != null && (retLine == null
                || styleLine.priority > retLine.priority) && styleLine.check(keys))
                {
                    retLine = styleLine;
                    linestring = idx;
                }
                if((styleLine = modifiers.get(idx)) != null && styleLine.check(keys))
                    over.put(idx, styleLine);
            }
            over.remove(linestring);
            if(over.size() != 0 && retLine != null)
            {
                List<LineElemStyle> s = new LinkedList<LineElemStyle>(over.values());
                Collections.sort(s);
                retLine = new LineElemStyle(retLine, s);
            }
            if(retArea != null)
            {
                if(retLine != null)
                    return new AreaElemStyle(retArea, retLine);
                else
                    return retArea;
            }
            return retLine;
        }

        public ElemStyle get(OsmPrimitive osm)
        {
            return (osm.keys == null) ? null :
            ((osm instanceof Node) ? getNode(osm.keys) : get(osm.keys,
            osm instanceof Way && !((Way)osm).isClosed()));
        }

        public ElemStyle getArea(Way osm)
        {
            if(osm.keys != null)
            {
                /* force area mode also for unclosed ways */
                ElemStyle style = get(osm.keys, false);
                if(style != null && style instanceof AreaElemStyle)
                    return style;
            }
            return null;
        }

        public IconElemStyle getIcon(OsmPrimitive osm)
        {
            return (osm.keys == null) ? null : getNode(osm.keys);
        }

        public boolean isArea(OsmPrimitive o)
        {
            if(o.keys != null && !(o instanceof Node))
            {
                boolean noclosed = o instanceof Way && !((Way)o).isClosed();
                Iterator<String> iterator = o.keys.keySet().iterator();
                while(iterator.hasNext())
                {
                    String key = iterator.next();
                    String val = o.keys.get(key);
                    AreaElemStyle s = areas.get("n" + key + "=" + val);
                    if(s == null || (s.closed && noclosed))
                        s = areas.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val));
                    if(s == null || (s.closed && noclosed))
                        s = areas.get("x" + key);
                    if(s != null && !(s.closed && noclosed))
                        return true;
                }
            }
            return false;
        }

        public boolean hasAreas()
        {
            return areas.size() > 0;
        }
    }

    HashMap<String, StyleSet> styleSet;
    public ElemStyles()
    {
        styleSet = new HashMap<String, StyleSet>();
    }

    public void add(String name, Rule r, LineElemStyle style)
    {
        String key = r.getKey();
        style.code = key;
        getStyleSet(name, true).lines.put(key, style);
    }

    public void addModifier(String name, Rule r, LineElemStyle style)
    {
        String key = r.getKey();
        style.code = key;
        getStyleSet(name, true).modifiers.put(key, style);
    }

    public void add(String name, Rule r, AreaElemStyle style)
    {
        String key = r.getKey();
        style.code = key;
        getStyleSet(name, true).areas.put(key, style);
    }

    public void add(String name, Rule r, IconElemStyle style)
    {
        String key = r.getKey();
        style.code = key;
        getStyleSet(name, true).icons.put(key, style);
    }

    private StyleSet getStyleSet(String name, boolean create)
    {
        if(name == null)
            name = Main.pref.get("mappaint.style", "standard");

        StyleSet s = styleSet.get(name);
        if(create && s == null)
        {
            s = new StyleSet();
            styleSet.put(name, s);
        }
        return s;
    }

    /* called from class users, never return null */
    public StyleSet getStyleSet()
    {
        return getStyleSet(null, false);
    }
}
