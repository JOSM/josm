// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.ElemStyles.WayPrototypesRecord;
import org.openstreetmap.josm.gui.preferences.SourceEntry;

public class XmlStyleSource extends SourceEntry {

    public final HashMap<String, IconPrototype> icons = new HashMap<String, IconPrototype>();
    public final HashMap<String, LinePrototype> lines = new HashMap<String, LinePrototype>();
    public final HashMap<String, LinemodPrototype> modifiers = new HashMap<String, LinemodPrototype>();
    public final HashMap<String, AreaPrototype> areas = new HashMap<String, AreaPrototype>();
    public final LinkedList<IconPrototype> iconsList = new LinkedList<IconPrototype>();
    public final LinkedList<LinePrototype> linesList = new LinkedList<LinePrototype>();
    public final LinkedList<LinemodPrototype> modifiersList = new LinkedList<LinemodPrototype>();
    public final LinkedList<AreaPrototype> areasList = new LinkedList<AreaPrototype>();

    public boolean hasError = false;

    public XmlStyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription, true);
    }

    public XmlStyleSource(SourceEntry entry) {
        super(entry.url, entry.name, entry.shortdescription, entry.active);
    }

    public IconPrototype getNode(OsmPrimitive primitive, IconPrototype icon) {
        for (String key : primitive.keySet()) {
            String val = primitive.get(key);
            IconPrototype style;
            if ((style = icons.get("n" + key + "=" + val)) != null) {
                if (icon == null || style.priority >= icon.priority) {
                    icon = style;
                }
            }
            if ((style = icons.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))) != null) {
                if (icon == null || style.priority >= icon.priority) {
                    icon = style;
                }
            }
            if ((style = icons.get("x" + key)) != null) {
                if (icon == null || style.priority >= icon.priority) {
                    icon = style;
                }
            }
        }
        for (IconPrototype s : iconsList) {
            if ((icon == null || s.priority >= icon.priority) && s.check(primitive)) {
                icon = s;
            }
        }
        return icon;
    }

    /**
     * @param closed The primitive is a closed way or we pretend it is closed.
     *  This is useful for multipolygon relations and outer ways of untagged
     *  multipolygon relations.
     */
    public void get(OsmPrimitive primitive, boolean closed, WayPrototypesRecord p) {
        String lineIdx = null;
        HashMap<String, LinemodPrototype> overlayMap = new HashMap<String, LinemodPrototype>();
        for (String key : primitive.keySet()) {
            String val = primitive.get(key);
            AreaPrototype styleArea;
            LinePrototype styleLine;
            LinemodPrototype styleLinemod;
            String idx = "n" + key + "=" + val;
            if ((styleArea = areas.get(idx)) != null && (p.area == null || styleArea.priority >= p.area.priority) && (closed || !styleArea.closed)) {
                p.area = styleArea;
            }
            if ((styleLine = lines.get(idx)) != null && (p.line == null || styleLine.priority >= p.line.priority)) {
                p.line = styleLine;
                lineIdx = idx;
            }
            if ((styleLinemod = modifiers.get(idx)) != null) {
                overlayMap.put(idx, styleLinemod);
            }
            idx = "b" + key + "=" + OsmUtils.getNamedOsmBoolean(val);
            if ((styleArea = areas.get(idx)) != null && (p.area == null || styleArea.priority >= p.area.priority) && (closed || !styleArea.closed)) {
                p.area = styleArea;
            }
            if ((styleLine = lines.get(idx)) != null && (p.line == null || styleLine.priority >= p.line.priority)) {
                p.line = styleLine;
                lineIdx = idx;
            }
            if ((styleLinemod = modifiers.get(idx)) != null) {
                overlayMap.put(idx, styleLinemod);
            }
            idx = "x" + key;
            if ((styleArea = areas.get(idx)) != null && (p.area == null || styleArea.priority >= p.area.priority) && (closed || !styleArea.closed)) {
                p.area = styleArea;
            }
            if ((styleLine = lines.get(idx)) != null && (p.line == null || styleLine.priority >= p.line.priority)) {
                p.line = styleLine;
                lineIdx = idx;
            }
            if ((styleLinemod = modifiers.get(idx)) != null) {
                overlayMap.put(idx, styleLinemod);
            }
        }
        for (AreaPrototype s : areasList) {
            if ((p.area == null || s.priority >= p.area.priority) && (closed || !s.closed) && s.check(primitive)) {
                p.area = s;
            }
        }
        for (LinePrototype s : linesList) {
            if ((p.line == null || s.priority >= p.line.priority) && s.check(primitive)) {
                p.line = s;
            }
        }
        for (LinemodPrototype s : modifiersList) {
            if (s.check(primitive)) {
                overlayMap.put(s.getCode(), s);
            }
        }
        overlayMap.remove(lineIdx); // do not use overlay if linestyle is from the same rule (example: railway=tram)
        if (!overlayMap.isEmpty() && p.line != null) {
            List<LinemodPrototype> tmp = new LinkedList<LinemodPrototype>();
            if (p.linemods != null) {
                tmp.addAll(p.linemods);
            }
            tmp.addAll(overlayMap.values());
            Collections.sort(tmp);
            p.linemods = tmp;
        }
    }

    public boolean isArea(OsmPrimitive o) {
        if (o.hasKeys() && !(o instanceof Node)) {
            boolean noclosed = o instanceof Way && !((Way) o).isClosed();
            Iterator<String> iterator = o.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String val = o.get(key);
                AreaPrototype s = areas.get("n" + key + "=" + val);
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
            for (AreaPrototype s : areasList) {
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

    public void add(XmlCondition c, Collection<XmlCondition> conditions, Prototype prot) {
         if(conditions != null)
         {
            prot.conditions = conditions;
            if (prot instanceof IconPrototype) {
                iconsList.add((IconPrototype) prot);
            } else if (prot instanceof LinemodPrototype) {
                modifiersList.add((LinemodPrototype) prot);
            } else if (prot instanceof LinePrototype) {
                linesList.add((LinePrototype) prot);
            } else if (prot instanceof AreaPrototype) {
                areasList.add((AreaPrototype) prot);
            } else
                throw new RuntimeException();
         }
         else {
             String key = c.getKey();
            prot.code = key;
            if (prot instanceof IconPrototype) {
                icons.put(key, (IconPrototype) prot);
            } else if (prot instanceof LinemodPrototype) {
               modifiers.put(key, (LinemodPrototype) prot);
            } else if (prot instanceof LinePrototype) {
                lines.put(key, (LinePrototype) prot);
            } else if (prot instanceof AreaPrototype) {
                areas.put(key, (AreaPrototype) prot);
            } else
                throw new RuntimeException();
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
}
