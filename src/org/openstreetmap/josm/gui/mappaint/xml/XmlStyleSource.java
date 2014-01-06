// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.StyleSource;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlStyleSource extends StyleSource implements StyleKeys {

    protected final Map<String, IconPrototype> icons = new HashMap<String, IconPrototype>();
    protected final Map<String, LinePrototype> lines = new HashMap<String, LinePrototype>();
    protected final Map<String, LinemodPrototype> modifiers = new HashMap<String, LinemodPrototype>();
    protected final Map<String, AreaPrototype> areas = new HashMap<String, AreaPrototype>();
    protected final List<IconPrototype> iconsList = new LinkedList<IconPrototype>();
    protected final List<LinePrototype> linesList = new LinkedList<LinePrototype>();
    protected final List<LinemodPrototype> modifiersList = new LinkedList<LinemodPrototype>();
    protected final List<AreaPrototype> areasList = new LinkedList<AreaPrototype>();

    public XmlStyleSource(String url, String name, String shortdescription) {
        super(url, name, shortdescription);
    }

    public XmlStyleSource(SourceEntry entry) {
        super(entry);
    }

    @Override
    protected void init() {
        super.init();
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
            InputStream in = getSourceInputStream();
            try {
                InputStreamReader reader = new InputStreamReader(in);
                XmlObjectParser parser = new XmlObjectParser(new XmlStyleSourceHandler(this));
                parser.startWithValidation(reader,
                        Main.JOSM_WEBSITE+"/mappaint-style-1.0",
                        "resource://data/mappaint-style.xsd");
                while (parser.hasNext());
            } finally {
                closeSourceInputStream(in);
            }

        } catch (IOException e) {
            Main.warn(tr("Failed to load Mappaint styles from ''{0}''. Exception was: {1}", url, e.toString()));
            Main.error(e);
            logError(e);
        } catch (SAXParseException e) {
            Main.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: [{1}:{2}] {3}", url, e.getLineNumber(), e.getColumnNumber(), e.getMessage()));
            Main.error(e);
            logError(e);
        } catch (SAXException e) {
            Main.warn(tr("Failed to parse Mappaint styles from ''{0}''. Error was: {1}", url, e.getMessage()));
            Main.error(e);
            logError(e);
        }
    }

    @Override
    public InputStream getSourceInputStream() throws IOException {
        MirroredInputStream in = new MirroredInputStream(url);
        InputStream zip = in.findZipEntryInputStream("xml", "style");
        if (zip != null) {
            zipIcons = in.getFile();
            return zip;
        } else {
            zipIcons = null;
            return in;
        }
    }

    private static class WayPrototypesRecord {
        public LinePrototype line;
        public List<LinemodPrototype> linemods;
        public AreaPrototype area;
    }

    private <T extends Prototype> T update(T current, T candidate, Double scale, MultiCascade mc) {
        if (requiresUpdate(current, candidate, scale, mc))
            return candidate;
        else
            return current;
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
        boolean isNotArea = primitive.isKeyFalse("area");
        for (String key : primitive.keySet()) {
            String val = primitive.get(key);
            AreaPrototype styleArea;
            LinePrototype styleLine;
            LinemodPrototype styleLinemod;
            String idx = "n" + key + "=" + val;
            if ((styleArea = areas.get(idx)) != null && (closed || !styleArea.closed) && !isNotArea) {
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
            if ((styleArea = areas.get(idx)) != null && (closed || !styleArea.closed) && !isNotArea) {
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
            if ((styleArea = areas.get(idx)) != null && (closed || !styleArea.closed) && !isNotArea) {
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
            if ((closed || !s.closed) && !isNotArea && s.check(primitive)) {
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
        Cascade def = mc.getOrCreateCascade("default");
        boolean useMinMaxScale = Main.pref.getBoolean("mappaint.zoomLevelDisplay", false);

        if (osm instanceof Node || (osm instanceof Relation && "restriction".equals(osm.get("type")))) {
            IconPrototype icon = getNode(osm, (useMinMaxScale ? scale : null), mc);
            if (icon != null) {
                def.put(ICON_IMAGE, icon.icon);
                if (osm instanceof Node) {
                    if (icon.annotate != null) {
                        if (icon.annotate) {
                            def.put(TEXT, Keyword.AUTO);
                        } else {
                            def.remove(TEXT);
                        }
                    }
                }
            }
        } else if (osm instanceof Way || (osm instanceof Relation && ((Relation)osm).isMultipolygon())) {
            WayPrototypesRecord p = new WayPrototypesRecord();
            get(osm, pretendWayIsClosed || !(osm instanceof Way) || ((Way) osm).isClosed(), p, (useMinMaxScale ? scale : null), mc);
            if (p.line != null) {
                def.put(WIDTH, new Float(p.line.getWidth()));
                def.putOrClear(REAL_WIDTH, p.line.realWidth != null ? new Float(p.line.realWidth) : null);
                def.putOrClear(COLOR, p.line.color);
                if (p.line.color != null) {
                    int alpha = p.line.color.getAlpha();
                    if (alpha != 255) {
                        def.put(OPACITY, Utils.color_int2float(alpha));
                    }
                }
                def.putOrClear(DASHES, p.line.getDashed());
                def.putOrClear(DASHES_BACKGROUND_COLOR, p.line.dashedColor);
            }
            Float refWidth = def.get(WIDTH, null, Float.class);
            if (refWidth != null && p.linemods != null) {
                int numOver = 0, numUnder = 0;

                while (mc.hasLayer(String.format("over_%d", ++numOver)));
                while (mc.hasLayer(String.format("under_%d", ++numUnder)));

                for (LinemodPrototype mod : p.linemods) {
                    Cascade c;
                    if (mod.over) {
                        String layer = String.format("over_%d", numOver);
                        c = mc.getOrCreateCascade(layer);
                        c.put(OBJECT_Z_INDEX, new Float(numOver));
                        ++numOver;
                    } else {
                        String layer = String.format("under_%d", numUnder);
                        c = mc.getOrCreateCascade(layer);
                        c.put(OBJECT_Z_INDEX, new Float(-numUnder));
                        ++numUnder;
                    }
                    c.put(WIDTH, new Float(mod.getWidth(refWidth)));
                    c.putOrClear(COLOR, mod.color);
                    if (mod.color != null) {
                        int alpha = mod.color.getAlpha();
                        if (alpha != 255) {
                            c.put(OPACITY, Utils.color_int2float(alpha));
                        }
                    }
                    c.putOrClear(DASHES, mod.getDashed());
                    c.putOrClear(DASHES_BACKGROUND_COLOR, mod.dashedColor);
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
                def.putOrClear(FILL_COLOR, p.area.color);
                def.putOrClear(TEXT_POSITION, Keyword.CENTER);
                def.putOrClear(TEXT, Keyword.AUTO);
                def.remove(FILL_IMAGE);
            }
        }
    }

}
