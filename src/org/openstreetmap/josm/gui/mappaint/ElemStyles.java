// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.StyleCache.StyleList;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

public class ElemStyles {
    private List<StyleSource> styleSources;
    private boolean drawMultipolygon;

    private int cacheIdx;

    public ElemStyles()
    {
        styleSources = new ArrayList<StyleSource>();
    }

    public void clearCached() {
        cacheIdx++;
    }

    public List<StyleSource> getStyleSources() {
        return Collections.<StyleSource>unmodifiableList(styleSources);
    }

    public StyleList get(OsmPrimitive osm, double scale, NavigatableComponent nc) {
        return getStyleCacheWithRange(osm, scale, nc).a;
    }

    public Pair<StyleList, Range> getStyleCacheWithRange(OsmPrimitive osm, double scale, NavigatableComponent nc) {
        if (osm.mappaintStyle == null || osm.mappaintCacheIdx != cacheIdx) {
            osm.mappaintStyle = StyleCache.EMPTY_STYLECACHE;
        } else {
            Pair<StyleList, Range> lst = osm.mappaintStyle.getWithRange(scale);
            if (lst.a != null)
                return lst;
        }
        Pair<StyleList, Range> p = getImpl(osm, scale, nc);
        if (osm instanceof Node) {
            boolean hasNonModifier = false;
            for (ElemStyle s : p.a) {
                if (!s.isModifier) {
                    hasNonModifier = true;
                    break;
                }
            }
            if (!hasNonModifier) {
                p.a = new StyleList(p.a, NodeElemStyle.SIMPLE_NODE_ELEMSTYLE);
            }
        } else if (osm instanceof Way) {
            boolean hasProperLineStyle = false;
            for (ElemStyle s : p.a) {
                if (s.isProperLineStyle()) {
                    hasProperLineStyle = true;
                    break;
                }
            }
            if (!hasProperLineStyle) {
                AreaElemStyle area = Utils.find(p.a, AreaElemStyle.class);
                LineElemStyle line = (area == null ? LineElemStyle.UNTAGGED_WAY : LineElemStyle.createSimpleLineStyle(area.color, true));
                p.a = new StyleList(p.a, line);
            }
        }
        osm.mappaintStyle = osm.mappaintStyle.put(p.a, p.b);
        osm.mappaintCacheIdx = cacheIdx;
        return p;
    }

    private Pair<StyleList, Range> getImpl(OsmPrimitive osm, double scale, NavigatableComponent nc) {
        if (osm instanceof Node)
            return generateStyles(osm, scale, null, false);
        else if (osm instanceof Way)
        {
            Pair<StyleList, Range> p = generateStyles(osm, scale, null, false);

            boolean isOuterWayOfSomeMP = false;
            Color wayColor = null;

            for (OsmPrimitive referrer : osm.getReferrers()) {
                Relation r = (Relation) referrer;
                if (!drawMultipolygon || !r.isMultipolygon()  || !r.isUsable()) {
                    continue;
                }
                Multipolygon multipolygon = new Multipolygon(nc);
                multipolygon.load(r);

                if (multipolygon.getOuterWays().contains(osm)) {
                    boolean hasIndependentLineStyle = false;
                    if (!isOuterWayOfSomeMP) { // do this only one time
                        List<ElemStyle> tmp = new ArrayList<ElemStyle>(p.a.size());
                        for (ElemStyle s : p.a) {
                            if (s instanceof AreaElemStyle) {
                                wayColor = ((AreaElemStyle) s).color;
                            } else {
                                tmp.add(s);
                                if (s.isProperLineStyle()) {
                                    hasIndependentLineStyle = true;
                                }
                            }
                        }
                        p.a = new StyleList(tmp);
                        isOuterWayOfSomeMP = true;
                    }

                    if (!hasIndependentLineStyle) {
                        Pair<StyleList, Range> mpElemStyles = getStyleCacheWithRange(r, scale, nc);
                        ElemStyle mpLine = null;
                        for (ElemStyle s : mpElemStyles.a) {
                            if (s.isProperLineStyle()) {
                                mpLine = s;
                                break;
                            }
                        }
                        if (mpLine != null) {
                            p.a = new StyleList(p.a, mpLine);
                            p.b = Range.cut(p.b, mpElemStyles.b);
                            break;
                        } else if (wayColor == null) {
                            AreaElemStyle mpArea = Utils.find(mpElemStyles.a, AreaElemStyle.class);
                            if (mpArea != null) {
                                p.b = Range.cut(p.b, mpElemStyles.b);
                                wayColor = mpArea.color;
                            }
                        }
                    }
                }
            }
            if (isOuterWayOfSomeMP) {
                boolean hasLineStyle = false;
                for (ElemStyle s : p.a) {
                    if (s.isProperLineStyle()) {
                        hasLineStyle = true;
                        break;
                    }
                }
                if (!hasLineStyle) {
                    p.a = new StyleList(p.a, LineElemStyle.createSimpleLineStyle(wayColor, true));
                }
                return p;
            }

            for (OsmPrimitive referrer : osm.getReferrers()) {
                Relation ref = (Relation) referrer;
                if (!drawMultipolygon || !ref.isMultipolygon() || !ref.isUsable()) {
                    continue;
                }
                Multipolygon multipolygon = new Multipolygon(nc);
                multipolygon.load(ref);

                if (multipolygon.getInnerWays().contains(osm)) {
                    Iterator<Way> it = multipolygon.getOuterWays().iterator();
                    p = generateStyles(osm, scale, it.hasNext() ? it.next() : null, false);
                    boolean hasIndependentElemStyle = false;
                    for (ElemStyle s : p.a) {
                        if (s.isProperLineStyle() || s instanceof AreaElemStyle) {
                            hasIndependentElemStyle = true;
                        }
                    }
                    if (!hasIndependentElemStyle && !multipolygon.getOuterWays().isEmpty()) {
                        StyleList mpElemStyles = get(ref, scale, nc);
                        Color mpColor = null;
                        for (ElemStyle mpS : mpElemStyles) {
                            if (mpS instanceof AreaElemStyle) {
                                mpColor = ((AreaElemStyle) mpS).color;
                                break;
                            }
                        }
                        p.a = new StyleList(p.a, LineElemStyle.createSimpleLineStyle(mpColor, true));
                    }
                    return p;
                }
            }
            return p;
        }
        else if (osm instanceof Relation)
        {
            Pair<StyleList, Range> p = generateStyles(osm, scale, null, true);
            if (drawMultipolygon && ((Relation)osm).isMultipolygon()) {
                if (!Utils.exists(p.a, AreaElemStyle.class)) {
                    // look at outer ways to find area style
                    Multipolygon multipolygon = new Multipolygon(nc);
                    multipolygon.load((Relation) osm);
                    for (Way w : multipolygon.getOuterWays()) {
                        Pair<StyleList, Range> wayStyles = generateStyles(w, scale, null, false);
                        ElemStyle area = Utils.find(wayStyles.a, AreaElemStyle.class);
                        if (area != null) {
                            p.a = new StyleList(p.a, area);
                            p.b = Range.cut(p.b, wayStyles.b);
                            break;
                        }
                    }
                }
            }
            return p;
        }
        return null;
    }

    /**
     * @param multipolyOuterWay support for a very old multipolygon tagging style
     * where you add the tags both to the outer and the inner way.
     * However, independent inner way style is also possible.
     * @param pretendWayIsClosed For styles that require the way to be closed,
     * we pretend it is. This is useful for generating area styles from the (segmented)
     * outer ways of a multipolygon.
     */
    public Pair<StyleList, Range> generateStyles(OsmPrimitive osm, double scale, OsmPrimitive multipolyOuterWay, boolean pretendWayIsClosed) {

        List<ElemStyle> sl = new ArrayList<ElemStyle>();
        MultiCascade mc = new MultiCascade();
        Environment env = new Environment(osm, mc, null, null);

        for (StyleSource s : styleSources) {
            if (s.active) {
                s.apply(mc, osm, scale, multipolyOuterWay, pretendWayIsClosed);
            }
        }

        for (Entry<String, Cascade> e : mc.getLayers()) {
            if ("*".equals(e.getKey())) {
                continue;
            }
            env.layer = e.getKey();
            Cascade c = e.getValue();
            if (osm instanceof Way) {
                addIfNotNull(sl, AreaElemStyle.create(c));
                addIfNotNull(sl, LinePatternElemStyle.create(env));
                addIfNotNull(sl, LineElemStyle.createLine(env));
                addIfNotNull(sl, LineElemStyle.createCasing(env));
                addIfNotNull(sl, LineTextElemStyle.create(env));
            } else if (osm instanceof Node) {
                addIfNotNull(sl, NodeElemStyle.create(env));
            } else if (osm instanceof Relation) {
                if (((Relation)osm).isMultipolygon()) {
                    addIfNotNull(sl, AreaElemStyle.create(c));
                    addIfNotNull(sl, LinePatternElemStyle.create(env));
                    addIfNotNull(sl, LineElemStyle.createLine(env));
                    addIfNotNull(sl, LineElemStyle.createCasing(env));
                    addIfNotNull(sl, LineTextElemStyle.create(env));
                } else if ("restriction".equals(osm.get("type"))) {
                    addIfNotNull(sl, NodeElemStyle.create(env));
                }
            }
        }

        return new Pair<StyleList, Range>(new StyleList(sl), mc.range);
    }

    private static <T> void addIfNotNull(List<T> list, T obj) {
        if (obj != null) {
            list.add(obj);
        }
    }

    public boolean isDrawMultipolygon() {
        return drawMultipolygon;
    }

    public void setDrawMultipolygon(boolean drawMultipolygon) {
        this.drawMultipolygon = drawMultipolygon;
    }

    /**
     * remove all style sources; only accessed from MapPaintStyles
     */
    void clear() {
        styleSources.clear();
    }

    /**
     * add a style source; only accessed from MapPaintStyles
     */
    void add(StyleSource style) {
        styleSources.add(style);
    }

    /**
     * set the style sources; only accessed from MapPaintStyles
     */
    void setStyleSources(Collection<StyleSource> sources) {
        styleSources.clear();
        styleSources.addAll(sources);
    }

}
