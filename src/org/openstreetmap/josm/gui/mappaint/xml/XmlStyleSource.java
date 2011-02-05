// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlStyleSource extends StyleSource {

    protected final HashMap<String, IconPrototype> icons = new HashMap<String, IconPrototype>();
    protected final HashMap<String, LinePrototype> lines = new HashMap<String, LinePrototype>();
    protected final HashMap<String, LinemodPrototype> modifiers = new HashMap<String, LinemodPrototype>();
    protected final HashMap<String, AreaPrototype> areas = new HashMap<String, AreaPrototype>();
    protected final LinkedList<IconPrototype> iconsList = new LinkedList<IconPrototype>();
    protected final LinkedList<LinePrototype> linesList = new LinkedList<LinePrototype>();
    protected final LinkedList<LinemodPrototype> modifiersList = new LinkedList<LinemodPrototype>();
    protected final LinkedList<AreaPrototype> areasList = new LinkedList<AreaPrototype>();

    public XmlStyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription);
    }

    public XmlStyleSource(SourceEntry entry) {
        super(entry);
    }

    protected void init() {
        hasError = false;
        icons.clear();
        lines.clear();
        modifiers.clear();
        areas.clear();
        iconsList.clear();
        linesList.clear();
        modifiersList.clear();
        areasList.clear();
    }

    @Override
    public void loadStyleSource() {
        init();
        try {
            MirroredInputStream in = new MirroredInputStream(url);
            InputStream zip = in.getZipEntry("xml", "style");
            InputStreamReader reader = null;
            if (zip != null) {
                reader = new InputStreamReader(zip);
                zipIcons = in.getFile();
            } else {
                reader = new InputStreamReader(in);
                zipIcons = null;
            }

            XmlObjectParser parser = new XmlObjectParser(new XmlStyleSourceHandler(this));
            parser.startWithValidation(reader,
                    "http://josm.openstreetmap.de/mappaint-style-1.0",
                    "resource://data/mappaint-style.xsd");
            while(parser.hasNext()) {
            }

        } catch(IOException e) {
            System.err.println(tr("Warning: failed to load Mappaint styles from ''{0}''. Exception was: {1}", url, e.toString()));
            e.printStackTrace();
            hasError = true;
        } catch(SAXParseException e) {
            System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: [{1}:{2}] {3}", url, e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
            e.printStackTrace();
            hasError = true;
        } catch(SAXException e) {
            System.err.println(tr("Warning: failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
            e.printStackTrace();
            hasError = true;
        }
    }

    private static class WayPrototypesRecord {
        public LinePrototype line;
        public List<LinemodPrototype> linemods;
        public AreaPrototype area;
    }

    private <T extends Prototype> T update(T current, T candidate, Double scale, MultiCascade mc) {
        return requiresUpdate(current, candidate, scale, mc) ? candidate : current;
    }

    /**
     * checks whether a certain match is better than the current match
     * @param current can be null
     * @param candidate the new Prototype that could be used instead
     * @param scale ignored if null, otherwise checks if scale is within the range of candidate
     * @param mc side effect: update the valid region for the current MultiCascade
     */
    private boolean requiresUpdate(Prototype current, Prototype candidate, Double scale, MultiCascade mc) {
        if (current == null || candidate.priority >= current.priority) {
            if (scale == null)
                return true;

            if (candidate.range.contains(scale)) {
                mc.range = Range.cut(mc.range, candidate.range);
                return true;
            } else {
                mc.range = mc.range.reduceAround(scale, candidate.range);
                return false;
            }
        }
        return false;
    }

    private IconPrototype getNode(OsmPrimitive primitive, Double scale, MultiCascade mc) {
        IconPrototype icon = null;
        for (String key : primitive.keySet()) {
            String val = primitive.get(key);
            IconPrototype p;
            if ((p = icons.get("n" + key + "=" + val)) != null) {
                icon = update(icon, p, scale, mc);
            }
            if ((p = icons.get("b" + key + "=" + OsmUtils.getNamedOsmBoolean(val))) != null) {
                icon = update(icon, p, scale, mc);
            }
            if ((p = icons.get("x" + key)) != null) {
                icon = update(icon, p, scale, mc);
            }
        }
        for (IconPrototype s : iconsList) {
            if (s.check(primitive))
            {
                icon = update(icon, s, scale, mc);
            }
        }
        return icon;
    }

    /**
     * @param closed The primitive is a closed way or we pretend it is closed.
     *  This is useful for multipolygon relations and outer ways of untagged
     *  multipolygon relations.
     */
    private void get(OsmPrimitive primitive, boolean closed, WayPrototypesRecord p, Double scale, MultiCascade mc) {
        String lineIdx = null;
        HashMap<String, LinemodPrototype> overlayMap = new HashMap<String, LinemodPrototype>();
        for (String key : primitive.keySet()) {
            String val = primitive.get(key);
            AreaPrototype styleArea;
            LinePrototype styleLine;
            LinemodPrototype styleLinemod;
            String idx = "n" + key + "=" + val;
            if ((styleArea = areas.get(idx)) != null && (closed || !styleArea.closed)) {
                p.area = update(p.area, styleArea, scale, mc);
            }
            if ((styleLine = lines.get(idx)) != null) {
                if (requiresUpdate(p.line, styleLine, scale, mc)) {
                    p.line = styleLine;
                    lineIdx = idx;
                }
            }
            if ((styleLinemod = modifiers.get(idx)) != null) {
                if (requiresUpdate(null, styleLinemod, scale, mc)) {
                    overlayMap.put(idx, styleLinemod);
                }
            }
            idx = "b" + key + "=" + OsmUtils.getNamedOsmBoolean(val);
            if ((styleArea = areas.get(idx)) != null && (closed || !styleArea.closed)) {
                p.area = update(p.area, styleArea, scale, mc);
            }
            if ((styleLine = lines.get(idx)) != null) {
                if (requiresUpdate(p.line, styleLine, scale, mc)) {
                    p.line = styleLine;
                    lineIdx = idx;
                }
            }
            if ((styleLinemod = modifiers.get(idx)) != null) {
                if (requiresUpdate(null, styleLinemod, scale, mc)) {
                    overlayMap.put(idx, styleLinemod);
                }
            }
            idx = "x" + key;
            if ((styleArea = areas.get(idx)) != null && (closed || !styleArea.closed)) {
                p.area = update(p.area, styleArea, scale, mc);
            }
            if ((styleLine = lines.get(idx)) != null) {
                if (requiresUpdate(p.line, styleLine, scale, mc)) {
                    p.line = styleLine;
                    lineIdx = idx;
                }
            }
            if ((styleLinemod = modifiers.get(idx)) != null) {
                if (requiresUpdate(null, styleLinemod, scale, mc)) {
                    overlayMap.put(idx, styleLinemod);
                }
            }
        }
        for (AreaPrototype s : areasList) {
            if ((closed || !s.closed) && s.check(primitive)) {
                p.area = update(p.area, s, scale, mc);
            }
        }
        for (LinePrototype s : linesList) {
            if (s.check(primitive)) {
                p.line = update(p.line, s, scale, mc);
            }
        }
        for (LinemodPrototype s : modifiersList) {
            if (s.check(primitive)) {
                if (requiresUpdate(null, s, scale, mc)) {
                    overlayMap.put(s.getCode(), s);
                }
            }
        }
        overlayMap.remove(lineIdx); // do not use overlay if linestyle is from the same rule (example: railway=tram)
        if (!overlayMap.isEmpty()) {
            List<LinemodPrototype> tmp = new LinkedList<LinemodPrototype>();
            if (p.linemods != null) {
                tmp.addAll(p.linemods);
            }
            tmp.addAll(overlayMap.values());
            Collections.sort(tmp);
            p.linemods = tmp;
        }
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

    @Override
    public void apply(MultiCascade mc, OsmPrimitive osm, double scale, OsmPrimitive multipolyOuterWay, boolean pretendWayIsClosed) {
        Cascade def = mc.getCascade("default");
        boolean useMinMaxScale = Main.pref.getBoolean("mappaint.zoomLevelDisplay", false);

        if (osm instanceof Node || (osm instanceof Relation && "restriction".equals(osm.get("type")))) {
            IconPrototype icon = getNode(osm, (useMinMaxScale ? scale : null), mc);
            if (icon != null) {
                def.put("icon-image", icon.icon);
                if (osm instanceof Node) {
                    if (icon.annotate != null) {
                        if (icon.annotate) {
                            def.put("text", "yes");
                        } else {
                            def.remove("text");
                        }
                    }
                }
            }
        } else if (osm instanceof Way || (osm instanceof Relation && ((Relation)osm).isMultipolygon())) {
            WayPrototypesRecord p = new WayPrototypesRecord();
            get(osm, pretendWayIsClosed || !(osm instanceof Way) || ((Way) osm).isClosed(), p, (useMinMaxScale ? scale : null), mc);
            if (p.line != null) {
                def.put("width", new Float(p.line.getWidth()));
                def.putOrClear("real-width", p.line.realWidth != null ? new Float(p.line.realWidth) : null);
                def.putOrClear("color", p.line.color);
                if (p.line.color != null) {
                    int alpha = p.line.color.getAlpha();
                    if (alpha != 255) {
                        def.put("opacity", ElemStyle.color_int2float(alpha));
                    }
                }
                def.putOrClear("dashes", p.line.getDashed());
                def.putOrClear("dashes-background-color", p.line.dashedColor);
            }
            Float refWidth = def.get("width", null, Float.class);
            if (refWidth != null && p.linemods != null) {
                int numOver = 0, numUnder = 0;

                while (mc.containsKey(String.format("over_%d", ++numOver))) {}
                while (mc.containsKey(String.format("under_%d", ++numUnder))) {}

                for (LinemodPrototype mod : p.linemods) {
                    Cascade c;
                    if (mod.over) {
                        c = mc.getCascade(String.format("over_%d", numOver));
                        c.put("object-z-index", new Float(numOver));
                        ++numOver;
                    } else {
                        c = mc.getCascade(String.format("under_%d", numUnder));
                        c.put("object-z-index", new Float(-numUnder));
                        ++numUnder;
                    }
                    c.put("width", new Float(mod.getWidth(refWidth)));
                    c.putOrClear("color", mod.color);
                    if (mod.color != null) {
                        int alpha = mod.color.getAlpha();
                        if (alpha != 255) {
                            c.put("opacity", ElemStyle.color_int2float(alpha));
                        }
                    }
                    c.putOrClear("dashes", mod.getDashed());
                    c.putOrClear("dashes-background-color", mod.dashedColor);
                }
            }
            if (multipolyOuterWay != null) {
                WayPrototypesRecord p2 = new WayPrototypesRecord();
                get(multipolyOuterWay, true, p2, (useMinMaxScale ? scale : null), mc);
                if (Utils.equal(p.area, p2.area)) {
                    p.area = null;
                }
            }
            if (p.area != null) {
                def.putOrClear("fill-color", p.area.color);
                def.remove("fill-image");
            }
        }
    }

}
