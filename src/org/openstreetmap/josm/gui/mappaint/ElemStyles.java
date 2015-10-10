// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.StyleCache.StyleList;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

public class ElemStyles {
    private List<StyleSource> styleSources;
    private boolean drawMultipolygon;

    private int cacheIdx = 1;

    private boolean defaultNodes, defaultLines;
    private int defaultNodesIdx, defaultLinesIdx;

    /**
     * Constructs a new {@code ElemStyles}.
     */
    public ElemStyles() {
        styleSources = new ArrayList<>();
    }

    /**
     * Clear the style cache for all primitives of all DataSets.
     */
    public void clearCached() {
        // run in EDT to make sure this isn't called during rendering run
        // {@link org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer#render}
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                cacheIdx++;
            }
        });
    }

    public List<StyleSource> getStyleSources() {
        return Collections.<StyleSource>unmodifiableList(styleSources);
    }

    /**
     * Create the list of styles for one primitive.
     *
     * @param osm the primitive
     * @param scale the scale (in meters per 100 pixel)
     * @param nc display component
     * @return list of styles
     */
    public StyleList get(OsmPrimitive osm, double scale, NavigatableComponent nc) {
        return getStyleCacheWithRange(osm, scale, nc).a;
    }

    /**
     * Create the list of styles and its valid scale range for one primitive.
     *
     * Automatically adds default styles in case no proper style was found.
     * Uses the cache, if possible, and saves the results to the cache.
     */
    public Pair<StyleList, Range> getStyleCacheWithRange(OsmPrimitive osm, double scale, NavigatableComponent nc) {
        if (osm.mappaintStyle == null || osm.mappaintCacheIdx != cacheIdx || scale <= 0) {
            osm.mappaintStyle = StyleCache.EMPTY_STYLECACHE;
        } else {
            Pair<StyleList, Range> lst = osm.mappaintStyle.getWithRange(scale);
            if (lst.a != null)
                return lst;
        }
        Pair<StyleList, Range> p = getImpl(osm, scale, nc);
        if (osm instanceof Node && isDefaultNodes()) {
            if (p.a.isEmpty()) {
                if (TextElement.AUTO_LABEL_COMPOSITION_STRATEGY.compose(osm) != null) {
                    p.a = NodeElemStyle.DEFAULT_NODE_STYLELIST_TEXT;
                } else {
                    p.a = NodeElemStyle.DEFAULT_NODE_STYLELIST;
                }
            } else {
                boolean hasNonModifier = false;
                boolean hasText = false;
                for (ElemStyle s : p.a) {
                    if (s instanceof BoxTextElemStyle) {
                        hasText = true;
                    } else {
                        if (!s.isModifier) {
                            hasNonModifier = true;
                        }
                    }
                }
                if (!hasNonModifier) {
                    p.a = new StyleList(p.a, NodeElemStyle.SIMPLE_NODE_ELEMSTYLE);
                    if (!hasText) {
                        if (TextElement.AUTO_LABEL_COMPOSITION_STRATEGY.compose(osm) != null) {
                            p.a = new StyleList(p.a, BoxTextElemStyle.SIMPLE_NODE_TEXT_ELEMSTYLE);
                        }
                    }
                }
            }
        } else if (osm instanceof Way && isDefaultLines()) {
            boolean hasProperLineStyle = false;
            for (ElemStyle s : p.a) {
                if (s.isProperLineStyle()) {
                    hasProperLineStyle = true;
                    break;
                }
            }
            if (!hasProperLineStyle) {
                AreaElemStyle area = Utils.find(p.a, AreaElemStyle.class);
                LineElemStyle line = area == null ? LineElemStyle.UNTAGGED_WAY : LineElemStyle.createSimpleLineStyle(area.color, true);
                p.a = new StyleList(p.a, line);
            }
        }
        StyleCache style = osm.mappaintStyle != null ? osm.mappaintStyle : StyleCache.EMPTY_STYLECACHE;
        try {
            osm.mappaintStyle = style.put(p.a, p.b);
        } catch (StyleCache.RangeViolatedError e) {
            throw new AssertionError("Range violated: " + e.getMessage()
                    + " (object: " + osm.getPrimitiveId() + ", current style: "+osm.mappaintStyle
                    + ", scale: " + scale + ", new stylelist: " + p.a + ", new range: " + p.b + ')', e);
        }
        osm.mappaintCacheIdx = cacheIdx;
        return p;
    }

    /**
     * Create the list of styles and its valid scale range for one primitive.
     *
     * This method does multipolygon handling.
     *
     * There are different tagging styles for multipolygons, that have to be respected:
     * - tags on the relation
     * - tags on the outer way (deprecated)
     *
     * If the primitive is a way, look for multipolygon parents. In case it
     * is indeed member of some multipolygon as role "outer", all area styles
     * are removed. (They apply to the multipolygon area.)
     * Outer ways can have their own independent line styles, e.g. a road as
     * boundary of a forest. Otherwise, in case, the way does not have an
     * independent line style, take a line style from the multipolygon.
     * If the multipolygon does not have a line style either, at least create a
     * default line style from the color of the area.
     *
     * Now consider the case that the way is not an outer way of any multipolygon,
     * but is member of a multipolygon as "inner".
     * First, the style list is regenerated, considering only tags of this way.
     * Then check, if the way describes something in its own right. (linear feature
     * or area) If not, add a default line style from the area color of the multipolygon.
     *
     */
    private Pair<StyleList, Range> getImpl(OsmPrimitive osm, double scale, NavigatableComponent nc) {
        if (osm instanceof Node)
            return generateStyles(osm, scale, false);
        else if (osm instanceof Way) {
            Pair<StyleList, Range> p = generateStyles(osm, scale, false);

            boolean isOuterWayOfSomeMP = false;
            Color wayColor = null;

            for (OsmPrimitive referrer : osm.getReferrers()) {
                Relation r = (Relation) referrer;
                if (!drawMultipolygon || !r.isMultipolygon()  || !r.isUsable()) {
                    continue;
                }
                Multipolygon multipolygon = MultipolygonCache.getInstance().get(nc, r);

                if (multipolygon.getOuterWays().contains(osm)) {
                    boolean hasIndependentLineStyle = false;
                    if (!isOuterWayOfSomeMP) { // do this only one time
                        List<ElemStyle> tmp = new ArrayList<>(p.a.size());
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
                        Pair<StyleList, Range> mpElemStyles;
                        synchronized (r) {
                            mpElemStyles = getStyleCacheWithRange(r, scale, nc);
                        }
                        ElemStyle mpLine = null;
                        for (ElemStyle s : mpElemStyles.a) {
                            if (s.isProperLineStyle()) {
                                mpLine = s;
                                break;
                            }
                        }
                        p.b = Range.cut(p.b, mpElemStyles.b);
                        if (mpLine != null) {
                            p.a = new StyleList(p.a, mpLine);
                            break;
                        } else if (wayColor == null && isDefaultLines()) {
                            AreaElemStyle mpArea = Utils.find(mpElemStyles.a, AreaElemStyle.class);
                            if (mpArea != null) {
                                wayColor = mpArea.color;
                            }
                        }
                    }
                }
            }
            if (isOuterWayOfSomeMP) {
                if (isDefaultLines()) {
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
                }
                return p;
            }

            if (!isDefaultLines()) return p;

            for (OsmPrimitive referrer : osm.getReferrers()) {
                Relation ref = (Relation) referrer;
                if (!drawMultipolygon || !ref.isMultipolygon() || !ref.isUsable()) {
                    continue;
                }
                final Multipolygon multipolygon = MultipolygonCache.getInstance().get(nc, ref);

                if (multipolygon.getInnerWays().contains(osm)) {
                    p = generateStyles(osm, scale, false);
                    boolean hasIndependentElemStyle = false;
                    for (ElemStyle s : p.a) {
                        if (s.isProperLineStyle() || s instanceof AreaElemStyle) {
                            hasIndependentElemStyle = true;
                            break;
                        }
                    }
                    if (!hasIndependentElemStyle && !multipolygon.getOuterWays().isEmpty()) {
                        Color mpColor = null;
                        StyleList mpElemStyles = null;
                        synchronized (ref) {
                            mpElemStyles = get(ref, scale, nc);
                        }
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
        } else if (osm instanceof Relation) {
            Pair<StyleList, Range> p = generateStyles(osm, scale, true);
            if (drawMultipolygon && ((Relation) osm).isMultipolygon()) {
                if (!Utils.exists(p.a, AreaElemStyle.class) && Main.pref.getBoolean("multipolygon.deprecated.outerstyle", true)) {
                    // look at outer ways to find area style
                    Multipolygon multipolygon = MultipolygonCache.getInstance().get(nc, (Relation) osm);
                    for (Way w : multipolygon.getOuterWays()) {
                        Pair<StyleList, Range> wayStyles = generateStyles(w, scale, false);
                        p.b = Range.cut(p.b, wayStyles.b);
                        ElemStyle area = Utils.find(wayStyles.a, AreaElemStyle.class);
                        if (area != null) {
                            p.a = new StyleList(p.a, area);
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
     * Create the list of styles and its valid scale range for one primitive.
     *
     * Loops over the list of style sources, to generate the map of properties.
     * From these properties, it generates the different types of styles.
     *
     * @param osm the primitive to create styles for
     * @param scale the scale (in meters per 100 px), must be &gt; 0
     * @param pretendWayIsClosed For styles that require the way to be closed,
     * we pretend it is. This is useful for generating area styles from the (segmented)
     * outer ways of a multipolygon.
     * @return the generated styles and the valid range as a pair
     */
    public Pair<StyleList, Range> generateStyles(OsmPrimitive osm, double scale, boolean pretendWayIsClosed) {

        List<ElemStyle> sl = new ArrayList<>();
        MultiCascade mc = new MultiCascade();
        Environment env = new Environment(osm, mc, null, null);

        for (StyleSource s : styleSources) {
            if (s.active) {
                s.apply(mc, osm, scale, pretendWayIsClosed);
            }
        }

        for (Entry<String, Cascade> e : mc.getLayers()) {
            if ("*".equals(e.getKey())) {
                continue;
            }
            env.layer = e.getKey();
            if (osm instanceof Way) {
                addIfNotNull(sl, AreaElemStyle.create(env));
                addIfNotNull(sl, RepeatImageElemStyle.create(env));
                addIfNotNull(sl, LineElemStyle.createLine(env));
                addIfNotNull(sl, LineElemStyle.createLeftCasing(env));
                addIfNotNull(sl, LineElemStyle.createRightCasing(env));
                addIfNotNull(sl, LineElemStyle.createCasing(env));
                addIfNotNull(sl, LineTextElemStyle.create(env));
            } else if (osm instanceof Node) {
                NodeElemStyle nodeStyle = NodeElemStyle.create(env);
                if (nodeStyle != null) {
                    sl.add(nodeStyle);
                    addIfNotNull(sl, BoxTextElemStyle.create(env, nodeStyle.getBoxProvider()));
                } else {
                    addIfNotNull(sl, BoxTextElemStyle.create(env, NodeElemStyle.SIMPLE_NODE_ELEMSTYLE_BOXPROVIDER));
                }
            } else if (osm instanceof Relation) {
                if (((Relation) osm).isMultipolygon()) {
                    addIfNotNull(sl, AreaElemStyle.create(env));
                    addIfNotNull(sl, RepeatImageElemStyle.create(env));
                    addIfNotNull(sl, LineElemStyle.createLine(env));
                    addIfNotNull(sl, LineElemStyle.createCasing(env));
                    addIfNotNull(sl, LineTextElemStyle.create(env));
                } else if ("restriction".equals(osm.get("type"))) {
                    addIfNotNull(sl, NodeElemStyle.create(env));
                }
            }
        }
        return new Pair<>(new StyleList(sl), mc.range);
    }

    private static <T> void addIfNotNull(List<T> list, T obj) {
        if (obj != null) {
            list.add(obj);
        }
    }

    /**
     * Draw a default node symbol for nodes that have no style?
     */
    private boolean isDefaultNodes() {
        if (defaultNodesIdx == cacheIdx)
            return defaultNodes;
        defaultNodes = fromCanvas("default-points", Boolean.TRUE, Boolean.class);
        defaultNodesIdx = cacheIdx;
        return defaultNodes;
    }

    /**
     * Draw a default line for ways that do not have an own line style?
     */
    private boolean isDefaultLines() {
        if (defaultLinesIdx == cacheIdx)
            return defaultLines;
        defaultLines = fromCanvas("default-lines", Boolean.TRUE, Boolean.class);
        defaultLinesIdx = cacheIdx;
        return defaultLines;
    }

    private <T> T fromCanvas(String key, T def, Class<T> c) {
        MultiCascade mc = new MultiCascade();
        Relation r = new Relation();
        r.put("#canvas", "query");

        for (StyleSource s : styleSources) {
            if (s.active) {
                s.apply(mc, r, 1, false);
            }
        }
        return mc.getCascade("default").get(key, def, c);
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

    /**
     * Returns the first AreaElemStyle for a given primitive.
     * @param p the OSM primitive
     * @param pretendWayIsClosed For styles that require the way to be closed,
     * we pretend it is. This is useful for generating area styles from the (segmented)
     * outer ways of a multipolygon.
     * @return first AreaElemStyle found or {@code null}.
     */
    public static AreaElemStyle getAreaElemStyle(OsmPrimitive p, boolean pretendWayIsClosed) {
        MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
        try {
            if (MapPaintStyles.getStyles() == null)
                return null;
            for (ElemStyle s : MapPaintStyles.getStyles().generateStyles(p, 1.0, pretendWayIsClosed).a) {
                if (s instanceof AreaElemStyle)
                    return (AreaElemStyle) s;
            }
            return null;
        } finally {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
        }
    }

    /**
     * Determines whether primitive has an AreaElemStyle.
     * @param p the OSM primitive
     * @param pretendWayIsClosed For styles that require the way to be closed,
     * we pretend it is. This is useful for generating area styles from the (segmented)
     * outer ways of a multipolygon.
     * @return {@code true} if primitive has an AreaElemStyle
     */
    public static boolean hasAreaElemStyle(OsmPrimitive p, boolean pretendWayIsClosed) {
        return getAreaElemStyle(p, pretendWayIsClosed) != null;
    }

    /**
     * Determines whether primitive has <b>only</b> an AreaElemStyle.
     * @param p the OSM primitive
     * @return {@code true} if primitive has only an AreaElemStyle
     * @since 7486
     */
    public static boolean hasOnlyAreaElemStyle(OsmPrimitive p) {
        MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
        try {
            if (MapPaintStyles.getStyles() == null)
                return false;
            StyleList styles = MapPaintStyles.getStyles().generateStyles(p, 1.0, false).a;
            if (styles.isEmpty()) {
                return false;
            }
            for (ElemStyle s : styles) {
                if (!(s instanceof AreaElemStyle)) {
                    return false;
                }
            }
            return true;
        } finally {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
        }
    }
}
