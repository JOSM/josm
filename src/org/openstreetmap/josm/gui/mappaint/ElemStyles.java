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
    public static class StyleSet {
        private HashMap<String, IconElemStyle> icons;
        private HashMap<String, LineElemStyle> lines;
        private HashMap<String, AreaElemStyle> areas;
        private HashMap<String, LineElemStyle> modifiers;
        private LinkedList<IconElemStyle> iconsList;
        private LinkedList<LineElemStyle> linesList;
        private LinkedList<AreaElemStyle> areasList;
        private LinkedList<LineElemStyle> modifiersList;
        public StyleSet()
        {
            icons = new HashMap<String, IconElemStyle>();
            lines = new HashMap<String, LineElemStyle>();
            modifiers = new HashMap<String, LineElemStyle>();
            areas = new HashMap<String, AreaElemStyle>();
            iconsList = new LinkedList<IconElemStyle>();
            linesList = new LinkedList<LineElemStyle>();
            modifiersList = new LinkedList<LineElemStyle>();
            areasList = new LinkedList<AreaElemStyle>();
        }
        private IconElemStyle getNode(OsmPrimitive primitive)
        {
            IconElemStyle ret = null;
            for (String key: primitive.keySet()) {
                String val = primitive.get(key);
                IconElemStyle style;
                if((style = icons.get("n" + key + "=" + val)) != null)
                {
                    if(ret == null || style.priority > ret.priority) {
                        ret = style;
                    }
                }
                if((style = icons.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))) != null)
                {
                    if(ret == null || style.priority > ret.priority) {
                        ret = style;
                    }
                }
                if((style = icons.get("x" + key)) != null)
                {
                    if(ret == null || style.priority > ret.priority) {
                        ret = style;
                    }
                }
            }
            for(IconElemStyle s : iconsList)
            {
                if((ret == null || s.priority > ret.priority) && s.check(primitive)) {
                    ret = s;
                }
            }
            return ret;
        }
        private ElemStyle get(OsmPrimitive primitive, boolean noclosed)
        {
            AreaElemStyle retArea = null;
            LineElemStyle retLine = null;
            String linestring = null;
            HashMap<String, LineElemStyle> over = new HashMap<String, LineElemStyle>();
            for (String key: primitive.keySet()) {
                String val = primitive.get(key);
                AreaElemStyle styleArea;
                LineElemStyle styleLine;
                String idx = "n" + key + "=" + val;
                if((styleArea = areas.get(idx)) != null && (retArea == null
                        || styleArea.priority > retArea.priority) && (!noclosed
                                || !styleArea.closed)) {
                    retArea = styleArea;
                }
                if((styleLine = lines.get(idx)) != null && (retLine == null
                        || styleLine.priority > retLine.priority))
                {
                    retLine = styleLine;
                    linestring = idx;
                }
                if((styleLine = modifiers.get(idx)) != null) {
                    over.put(idx, styleLine);
                }
                idx = "b" + key + "=" + OsmUtils.getNamedOsmBoolean(val);
                if((styleArea = areas.get(idx)) != null && (retArea == null
                        || styleArea.priority > retArea.priority) && (!noclosed
                                || !styleArea.closed)) {
                    retArea = styleArea;
                }
                if((styleLine = lines.get(idx)) != null && (retLine == null
                        || styleLine.priority > retLine.priority))
                {
                    retLine = styleLine;
                    linestring = idx;
                }
                if((styleLine = modifiers.get(idx)) != null) {
                    over.put(idx, styleLine);
                }
                idx = "x" + key;
                if((styleArea = areas.get(idx)) != null && (retArea == null
                        || styleArea.priority > retArea.priority) && (!noclosed
                                || !styleArea.closed)) {
                    retArea = styleArea;
                }
                if((styleLine = lines.get(idx)) != null && (retLine == null
                        || styleLine.priority > retLine.priority))
                {
                    retLine = styleLine;
                    linestring = idx;
                }
                if((styleLine = modifiers.get(idx)) != null) {
                    over.put(idx, styleLine);
                }
            }
            for(AreaElemStyle s : areasList)
            {
                if((retArea == null || s.priority > retArea.priority)
                        && (!noclosed || !s.closed) && s.check(primitive)) {
                    retArea = s;
                }
            }
            for(LineElemStyle s : linesList)
            {
                if((retLine == null || s.priority > retLine.priority)
                        && s.check(primitive)) {
                    retLine = s;
                }
            }
            for(LineElemStyle s : modifiersList)
            {
                if(s.check(primitive)) {
                    over.put(s.getCode(), s);
                }
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
            return (!osm.hasKeys()) ? null :
                ((osm instanceof Node) ? getNode(osm) : get(osm,
                        osm instanceof Way && !((Way)osm).isClosed()));
        }

        public ElemStyle getArea(Way osm)
        {
            if(osm.hasKeys())
            {
                /* force area mode also for unclosed ways */
                ElemStyle style = get(osm, false);
                if(style != null && style instanceof AreaElemStyle)
                    return style;
            }
            return null;
        }

        public IconElemStyle getIcon(OsmPrimitive osm)
        {
            return osm.hasKeys() ? getNode(osm): null;
        }

        public boolean isArea(OsmPrimitive o)
        {
            if(o.hasKeys() && !(o instanceof Node))
            {
                boolean noclosed = o instanceof Way && !((Way)o).isClosed();
                Iterator<String> iterator = o.keySet().iterator();
                while(iterator.hasNext())
                {
                    String key = iterator.next();
                    String val = o.get(key);
                    AreaElemStyle s = areas.get("n" + key + "=" + val);
                    if(s == null || (s.closed && noclosed)) {
                        s = areas.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val));
                    }
                    if(s == null || (s.closed && noclosed)) {
                        s = areas.get("x" + key);
                    }
                    if(s != null && !(s.closed && noclosed))
                        return true;
                }
                for(AreaElemStyle s : areasList)
                {
                    if(!(s.closed && noclosed) && s.check(o))
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

    private StyleSet getStyleSet(String name, boolean create)
    {
        if(name == null) {
            name = Main.pref.get("mappaint.style", "standard");
        }

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
        return getStyleSet(null, true);
    }

    public Collection<String> getStyleNames()
    {
        return styleSet.keySet();
    }
}
