// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.preferences.SourceEntry;

public class StyleSource extends SourceEntry {

    public final HashMap<String, IconElemStyle> icons = new HashMap<String, IconElemStyle>();
    public final HashMap<String, LineElemStyle> lines = new HashMap<String, LineElemStyle>();
    public final HashMap<String, LineElemStyle> modifiers = new HashMap<String, LineElemStyle>();
    public final HashMap<String, AreaElemStyle> areas = new HashMap<String, AreaElemStyle>();
    public final LinkedList<IconElemStyle> iconsList = new LinkedList<IconElemStyle>();
    public final LinkedList<LineElemStyle> linesList = new LinkedList<LineElemStyle>();
    public final LinkedList<LineElemStyle> modifiersList = new LinkedList<LineElemStyle>();
    public final LinkedList<AreaElemStyle> areasList = new LinkedList<AreaElemStyle>();

    public boolean hasError = false;

    public StyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription, true);
    }

    public StyleSource(SourceEntry entry) {
        super(entry.url, entry.name, entry.shortdescription, entry.active);
    }

    public IconElemStyle getNode(OsmPrimitive primitive, IconElemStyle icon) {
        for (String key : primitive.keySet()) {
            String val = primitive.get(key);
            IconElemStyle style;
            if ((style = icons.get("n" + key + "=" + val)) != null) {
                if (icon == null || style.priority > icon.priority) {
                    icon = style;
                }
            }
            if ((style = icons.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))) != null) {
                if (icon == null || style.priority > icon.priority) {
                    icon = style;
                }
            }
            if ((style = icons.get("x" + key)) != null) {
                if (icon == null || style.priority > icon.priority) {
                    icon = style;
                }
            }
        }
        for (IconElemStyle s : iconsList) {
            if ((icon == null || s.priority > icon.priority) && s.check(primitive)) {
                icon = s;
            }
        }
        return icon;
    }

    public ElemStyle get(OsmPrimitive primitive, boolean noclosed, AreaElemStyle area, LineElemStyle line) {
        String lineIdx = null;
        HashMap<String, LineElemStyle> overlayMap = new HashMap<String, LineElemStyle>();
        for (String key : primitive.keySet()) {
            String val = primitive.get(key);
            AreaElemStyle styleArea;
            LineElemStyle styleLine;
            String idx = "n" + key + "=" + val;
            if ((styleArea = areas.get(idx)) != null && (area == null || styleArea.priority > area.priority) && (!noclosed || !styleArea.closed)) {
                area = styleArea;
            }
            if ((styleLine = lines.get(idx)) != null && (line == null || styleLine.priority > line.priority)) {
                line = styleLine;
                lineIdx = idx;
            }
            if ((styleLine = modifiers.get(idx)) != null) {
                overlayMap.put(idx, styleLine);
            }
            idx = "b" + key + "=" + OsmUtils.getNamedOsmBoolean(val);
            if ((styleArea = areas.get(idx)) != null && (area == null || styleArea.priority > area.priority) && (!noclosed || !styleArea.closed)) {
                area = styleArea;
            }
            if ((styleLine = lines.get(idx)) != null && (line == null || styleLine.priority > line.priority)) {
                line = styleLine;
                lineIdx = idx;
            }
            if ((styleLine = modifiers.get(idx)) != null) {
                overlayMap.put(idx, styleLine);
            }
            idx = "x" + key;
            if ((styleArea = areas.get(idx)) != null && (area == null || styleArea.priority > area.priority) && (!noclosed || !styleArea.closed)) {
                area = styleArea;
            }
            if ((styleLine = lines.get(idx)) != null && (line == null || styleLine.priority > line.priority)) {
                line = styleLine;
                lineIdx = idx;
            }
            if ((styleLine = modifiers.get(idx)) != null) {
                overlayMap.put(idx, styleLine);
            }
        }
        for (AreaElemStyle s : areasList) {
            if ((area == null || s.priority > area.priority) && (!noclosed || !s.closed) && s.check(primitive)) {
                area = s;
            }
        }
        for (LineElemStyle s : linesList) {
            if ((line == null || s.priority > line.priority) && s.check(primitive)) {
                line = s;
            }
        }
        for (LineElemStyle s : modifiersList) {
            if (s.check(primitive)) {
                overlayMap.put(s.getCode(), s);
            }
        }
        overlayMap.remove(lineIdx); // do not use overlay if linestyle is from the same rule (example: railway=tram)
        if (!overlayMap.isEmpty() && line != null) {
            List<LineElemStyle> tmp = new LinkedList<LineElemStyle>();
            if (line.overlays != null) {
                tmp.addAll(line.overlays);
            }
            tmp.addAll(overlayMap.values());
            Collections.sort(tmp);
            line = new LineElemStyle(line, tmp);
        }
        if (area != null) {
            if (line != null) {
                return new AreaElemStyle(area, line);
            } else {
                return area;
            }
        }
        return line;
    }

    public boolean isArea(OsmPrimitive o) {
        if (o.hasKeys() && !(o instanceof Node)) {
            boolean noclosed = o instanceof Way && !((Way) o).isClosed();
            Iterator<String> iterator = o.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String val = o.get(key);
                AreaElemStyle s = areas.get("n" + key + "=" + val);
                if (s == null || (s.closed && noclosed)) {
                    s = areas.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val));
                }
                if (s == null || (s.closed && noclosed)) {
                    s = areas.get("x" + key);
                }
                if (s != null && !(s.closed && noclosed)) {
                    return true;
                }
            }
            for (AreaElemStyle s : areasList) {
                if (!(s.closed && noclosed) && s.check(o)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasAreas() {
        return areas.size() > 0;
    }

    public void add(Rule r, Collection<Rule> rules, LineElemStyle style) {
        if(rules != null)
        {
            style.rules = rules;
            linesList.add(style);
        }
        else {
            String key = r.getKey();
            style.code = key;
            lines.put(key, style);
        }
    }

    public void addModifier(Rule r, Collection<Rule> rules, LineElemStyle style) {
        if(rules != null)
        {
            style.rules = rules;
            modifiersList.add(style);
        }
        else
        {
            String key = r.getKey();
            style.code = key;
            modifiers.put(key, style);
        }
    }

    public void add(Rule r, Collection<Rule> rules, AreaElemStyle style) {
        if(rules != null)
        {
            style.rules = rules;
            areasList.add(style);
        }
        else
        {
            String key = r.getKey();
            style.code = key;
            areas.put(key, style);
        }
    }

    public void add(Rule r, Collection<Rule> rules, IconElemStyle style) {
        if(rules != null)
        {
            style.rules = rules;
            iconsList.add(style);
        }
        else
        {
            String key = r.getKey();
            style.code = key;
            icons.put(key, style);
        }
    }

    /**
     * the name / identifier that should be used to save custom color values
     * and similar stuff to the preference file
     * @return the identifier; never null. Usually the result is "standard"
     */
    public String getPrefName() {
        return name == null ? "standard" : name;
    }

    /**
     * String to show in menus and error messages.
     * @return Usually the shortdescription, but can be the file name
     * if no shortdescription is available.
     */
    public String getDisplayString() {
        if (shortdescription != null)
            return shortdescription;
        /**
         * extract file part from url, e.g.:
         * http://www.test.com/file.xml?format=text  --> file.xml
         */
        Pattern p = Pattern.compile("([^/\\\\]*?)([?].*)?$");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group(1);
        } else {
            System.err.println("Warning: Unexpected URL format: "+url);
            return url;
        }
    }
}
