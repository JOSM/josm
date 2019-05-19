// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.DividedScale.RangeViolatedError;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.styleelement.AreaElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.AreaIconElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.DefaultStyles;
import org.openstreetmap.josm.gui.mappaint.styleelement.LineElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.NodeElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.RepeatImageElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.TextElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.TextLabel;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * Generates a list of {@link StyleElement}s for a primitive, to
 * be drawn on the map.
 * There are several steps to derive the list of elements for display:
 * <ol>
 * <li>{@link #generateStyles(IPrimitive, double, boolean)} applies the
 * {@link StyleSource}s one after another to get a key-value map of MapCSS
 * properties. Then a preliminary set of StyleElements is derived from the
 * properties map.</li>
 * <li>{@link #getImpl(IPrimitive, double, NavigatableComponent)} handles the
 * different forms of multipolygon tagging.</li>
 * <li>{@link #getStyleCacheWithRange(IPrimitive, double, NavigatableComponent)}
 * adds a default StyleElement for primitives that would be invisible otherwise.
 * (For example untagged nodes and ways.)</li>
 * </ol>
 * The results are cached with respect to the current scale.
 *
 * Use {@link #setStyleSources(Collection)} to select the StyleSources that are applied.
 */
public class ElemStyles implements PreferenceChangedListener {
    private final List<StyleSource> styleSources = Collections.synchronizedList(new ArrayList<>());
    private boolean drawMultipolygon;

    private short cacheIdx = 1;

    private boolean defaultNodes;
    private boolean defaultLines;

    private short defaultNodesIdx;
    private short defaultLinesIdx;

    private final Map<String, String> preferenceCache = Collections.synchronizedMap(new HashMap<>());

    private volatile Color backgroundColorCache;

    /**
     * Constructs a new {@code ElemStyles}.
     */
    public ElemStyles() {
        Config.getPref().addPreferenceChangeListener(this);
    }

    /**
     * Clear the style cache for all primitives of all DataSets.
     */
    public void clearCached() {
        // run in EDT to make sure this isn't called during rendering run
        GuiHelper.runInEDT(() -> {
            cacheIdx++;
            preferenceCache.clear();
            backgroundColorCache = null;
            MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class).forEach(
                    dl -> dl.data.clearMappaintCache());
        });
    }

    /**
     * Returns the list of style sources.
     * @return the list of style sources
     */
    public List<StyleSource> getStyleSources() {
        return Collections.<StyleSource>unmodifiableList(styleSources);
    }

    /**
     * Returns the background color.
     * @return the background color
     */
    public Color getBackgroundColor() {
        if (backgroundColorCache != null)
            return backgroundColorCache;
        for (StyleSource s : styleSources) {
            if (!s.active) {
                continue;
            }
            Color backgroundColorOverride = s.getBackgroundColorOverride();
            if (backgroundColorOverride != null) {
                backgroundColorCache = backgroundColorOverride;
            }
        }
        return Optional.ofNullable(backgroundColorCache).orElseGet(PaintColors.BACKGROUND::get);
    }

    /**
     * Create the list of styles for one primitive.
     *
     * @param osm the primitive
     * @param scale the scale (in meters per 100 pixel)
     * @param nc display component
     * @return list of styles
     * @since 13810 (signature)
     */
    public StyleElementList get(IPrimitive osm, double scale, NavigatableComponent nc) {
        return getStyleCacheWithRange(osm, scale, nc).a;
    }

    /**
     * Create the list of styles and its valid scale range for one primitive.
     *
     * Automatically adds default styles in case no proper style was found.
     * Uses the cache, if possible, and saves the results to the cache.
     * @param osm OSM primitive
     * @param scale scale
     * @param nc navigatable component
     * @return pair containing style list and range
     * @since 13810 (signature)
     */
    public Pair<StyleElementList, Range> getStyleCacheWithRange(IPrimitive osm, double scale, NavigatableComponent nc) {
        if (!osm.isCachedStyleUpToDate() || scale <= 0) {
            osm.setCachedStyle(StyleCache.EMPTY_STYLECACHE);
        } else {
            Pair<StyleElementList, Range> lst = osm.getCachedStyle().getWithRange(scale, osm.isSelected());
            if (lst.a != null)
                return lst;
        }
        Pair<StyleElementList, Range> p = getImpl(osm, scale, nc);
        if (osm instanceof INode && isDefaultNodes()) {
            if (p.a.isEmpty()) {
                if (TextLabel.AUTO_LABEL_COMPOSITION_STRATEGY.compose(osm) != null) {
                    p.a = DefaultStyles.DEFAULT_NODE_STYLELIST_TEXT;
                } else {
                    p.a = DefaultStyles.DEFAULT_NODE_STYLELIST;
                }
            } else {
                boolean hasNonModifier = false;
                boolean hasText = false;
                for (StyleElement s : p.a) {
                    if (s instanceof BoxTextElement) {
                        hasText = true;
                    } else {
                        if (!s.isModifier) {
                            hasNonModifier = true;
                        }
                    }
                }
                if (!hasNonModifier) {
                    p.a = new StyleElementList(p.a, DefaultStyles.SIMPLE_NODE_ELEMSTYLE);
                    if (!hasText && TextLabel.AUTO_LABEL_COMPOSITION_STRATEGY.compose(osm) != null) {
                        p.a = new StyleElementList(p.a, DefaultStyles.SIMPLE_NODE_TEXT_ELEMSTYLE);
                    }
                }
            }
        } else if (osm instanceof IWay && isDefaultLines()) {
            boolean hasProperLineStyle = false;
            for (StyleElement s : p.a) {
                if (s.isProperLineStyle()) {
                    hasProperLineStyle = true;
                    break;
                }
            }
            if (!hasProperLineStyle) {
                AreaElement area = Utils.find(p.a, AreaElement.class);
                LineElement line = area == null ? LineElement.UNTAGGED_WAY : LineElement.createSimpleLineStyle(area.color, true);
                p.a = new StyleElementList(p.a, line);
            }
        }
        StyleCache style = osm.getCachedStyle() != null ? osm.getCachedStyle() : StyleCache.EMPTY_STYLECACHE;
        try {
            osm.setCachedStyle(style.put(p.a, p.b, osm.isSelected()));
        } catch (RangeViolatedError e) {
            throw new AssertionError("Range violated: " + e.getMessage()
                    + " (object: " + osm.getPrimitiveId() + ", current style: "+osm.getCachedStyle()
                    + ", scale: " + scale + ", new stylelist: " + p.a + ", new range: " + p.b + ')', e);
        }
        osm.declareCachedStyleUpToDate();
        return p;
    }

    /**
     * Create the list of styles and its valid scale range for one primitive.
     *
     * This method does multipolygon handling.
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
     * @param osm OSM primitive
     * @param scale scale
     * @param nc navigatable component
     * @return pair containing style list and range
     */
    private Pair<StyleElementList, Range> getImpl(IPrimitive osm, double scale, NavigatableComponent nc) {
        if (osm instanceof INode)
            return generateStyles(osm, scale, false);
        else if (osm instanceof IWay) {
            Pair<StyleElementList, Range> p = generateStyles(osm, scale, false);

            boolean isOuterWayOfSomeMP = false;
            Color wayColor = null;

            // FIXME: Maybe in the future outer way styles apply to outers ignoring the multipolygon?
            for (IPrimitive referrer : osm.getReferrers()) {
                IRelation<?> r = (IRelation<?>) referrer;
                if (!drawMultipolygon || !r.isMultipolygon() || !r.isUsable() || !(r instanceof Relation)) {
                    continue;
                }
                Multipolygon multipolygon = MultipolygonCache.getInstance().get((Relation) r);

                if (multipolygon.getOuterWays().contains(osm)) {
                    boolean hasIndependentLineStyle = false;
                    if (!isOuterWayOfSomeMP) { // do this only one time
                        List<StyleElement> tmp = new ArrayList<>(p.a.size());
                        for (StyleElement s : p.a) {
                            if (s instanceof AreaElement) {
                                wayColor = ((AreaElement) s).color;
                            } else {
                                tmp.add(s);
                                if (s.isProperLineStyle()) {
                                    hasIndependentLineStyle = true;
                                }
                            }
                        }
                        p.a = new StyleElementList(tmp);
                        isOuterWayOfSomeMP = true;
                    }

                    if (!hasIndependentLineStyle) {
                        Pair<StyleElementList, Range> mpElemStyles;
                        synchronized (r) {
                            mpElemStyles = getStyleCacheWithRange(r, scale, nc);
                        }
                        StyleElement mpLine = null;
                        for (StyleElement s : mpElemStyles.a) {
                            if (s.isProperLineStyle()) {
                                mpLine = s;
                                break;
                            }
                        }
                        p.b = Range.cut(p.b, mpElemStyles.b);
                        if (mpLine != null) {
                            p.a = new StyleElementList(p.a, mpLine);
                            break;
                        } else if (wayColor == null && isDefaultLines()) {
                            AreaElement mpArea = Utils.find(mpElemStyles.a, AreaElement.class);
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
                    for (StyleElement s : p.a) {
                        if (s.isProperLineStyle()) {
                            hasLineStyle = true;
                            break;
                        }
                    }
                    if (!hasLineStyle) {
                        p.a = new StyleElementList(p.a, LineElement.createSimpleLineStyle(wayColor, true));
                    }
                }
                return p;
            }

            if (!isDefaultLines()) return p;

            for (IPrimitive referrer : osm.getReferrers()) {
                IRelation<?> ref = (IRelation<?>) referrer;
                if (!drawMultipolygon || !ref.isMultipolygon() || !ref.isUsable() || !(ref instanceof Relation)) {
                    continue;
                }
                final Multipolygon multipolygon = MultipolygonCache.getInstance().get((Relation) ref);

                if (multipolygon.getInnerWays().contains(osm)) {
                    p = generateStyles(osm, scale, false);
                    boolean hasIndependentElemStyle = false;
                    for (StyleElement s : p.a) {
                        if (s.isProperLineStyle() || s instanceof AreaElement) {
                            hasIndependentElemStyle = true;
                            break;
                        }
                    }
                    if (!hasIndependentElemStyle && !multipolygon.getOuterWays().isEmpty()) {
                        Color mpColor = null;
                        StyleElementList mpElemStyles;
                        synchronized (ref) {
                            mpElemStyles = get(ref, scale, nc);
                        }
                        for (StyleElement mpS : mpElemStyles) {
                            if (mpS instanceof AreaElement) {
                                mpColor = ((AreaElement) mpS).color;
                                break;
                            }
                        }
                        p.a = new StyleElementList(p.a, LineElement.createSimpleLineStyle(mpColor, true));
                    }
                    return p;
                }
            }
            return p;
        } else if (osm instanceof IRelation) {
            return generateStyles(osm, scale, true);
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
     * @since 13810 (signature)
     */
    public Pair<StyleElementList, Range> generateStyles(IPrimitive osm, double scale, boolean pretendWayIsClosed) {

        List<StyleElement> sl = new ArrayList<>();
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
            if (osm instanceof IWay) {
                AreaElement areaStyle = AreaElement.create(env);
                addIfNotNull(sl, areaStyle);
                addIfNotNull(sl, RepeatImageElement.create(env));
                addIfNotNull(sl, LineElement.createLine(env));
                addIfNotNull(sl, LineElement.createLeftCasing(env));
                addIfNotNull(sl, LineElement.createRightCasing(env));
                addIfNotNull(sl, LineElement.createCasing(env));
                addIfNotNull(sl, AreaIconElement.create(env));
                addIfNotNull(sl, TextElement.create(env));
                if (areaStyle != null) {
                    //TODO: Warn about this, or even remove it completely
                    addIfNotNull(sl, TextElement.createForContent(env));
                }
            } else if (osm instanceof INode) {
                NodeElement nodeStyle = NodeElement.create(env);
                if (nodeStyle != null) {
                    sl.add(nodeStyle);
                    addIfNotNull(sl, BoxTextElement.create(env, nodeStyle.getBoxProvider()));
                } else {
                    addIfNotNull(sl, BoxTextElement.create(env, DefaultStyles.SIMPLE_NODE_ELEMSTYLE_BOXPROVIDER));
                }
            } else if (osm instanceof IRelation) {
                if (((IRelation<?>) osm).isMultipolygon()) {
                    AreaElement areaStyle = AreaElement.create(env);
                    addIfNotNull(sl, areaStyle);
                    addIfNotNull(sl, RepeatImageElement.create(env));
                    addIfNotNull(sl, LineElement.createLine(env));
                    addIfNotNull(sl, LineElement.createCasing(env));
                    addIfNotNull(sl, AreaIconElement.create(env));
                    addIfNotNull(sl, TextElement.create(env));
                    if (areaStyle != null) {
                        //TODO: Warn about this, or even remove it completely
                        addIfNotNull(sl, TextElement.createForContent(env));
                    }
                } else if (osm.hasTag("type", "restriction")) {
                    addIfNotNull(sl, NodeElement.create(env));
                }
            }
        }
        return new Pair<>(new StyleElementList(sl), mc.range);
    }

    private static <T> void addIfNotNull(List<T> list, T obj) {
        if (obj != null) {
            list.add(obj);
        }
    }

    /**
     * Draw a default node symbol for nodes that have no style?
     * @return {@code true} if default node symbol must be drawn
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
     * @return {@code true} if default line must be drawn
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

    /**
     * Determines whether multipolygons must be drawn.
     * @return whether multipolygons must be drawn.
     */
    public boolean isDrawMultipolygon() {
        return drawMultipolygon;
    }

    /**
     * Sets whether multipolygons must be drawn.
     * @param drawMultipolygon whether multipolygons must be drawn
     */
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
     * @param style style source to add
     */
    void add(StyleSource style) {
        styleSources.add(Objects.requireNonNull(style));
    }

    /**
     * remove a style source; only accessed from MapPaintStyles
     * @param style style source to remove
     * @return {@code true} if this list contained the specified element
     */
    boolean remove(StyleSource style) {
        return styleSources.remove(Objects.requireNonNull(style));
    }

    /**
     * set the style sources; only accessed from MapPaintStyles
     * @param sources new style sources
     */
    void setStyleSources(Collection<StyleSource> sources) {
        styleSources.clear();
        sources.forEach(this::add);
    }

    /**
     * Returns the first AreaElement for a given primitive.
     * @param p the OSM primitive
     * @param pretendWayIsClosed For styles that require the way to be closed,
     * we pretend it is. This is useful for generating area styles from the (segmented)
     * outer ways of a multipolygon.
     * @return first AreaElement found or {@code null}.
     * @since 13810 (signature)
     */
    public static AreaElement getAreaElemStyle(IPrimitive p, boolean pretendWayIsClosed) {
        MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
        try {
            if (MapPaintStyles.getStyles() == null)
                return null;
            for (StyleElement s : MapPaintStyles.getStyles().generateStyles(p, 1.0, pretendWayIsClosed).a) {
                if (s instanceof AreaElement)
                    return (AreaElement) s;
            }
            return null;
        } finally {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
        }
    }

    /**
     * Determines whether primitive has an AreaElement.
     * @param p the OSM primitive
     * @param pretendWayIsClosed For styles that require the way to be closed,
     * we pretend it is. This is useful for generating area styles from the (segmented)
     * outer ways of a multipolygon.
     * @return {@code true} if primitive has an AreaElement
     * @since 13810 (signature)
     */
    public static boolean hasAreaElemStyle(IPrimitive p, boolean pretendWayIsClosed) {
        return getAreaElemStyle(p, pretendWayIsClosed) != null;
    }

    /**
     * Determines whether primitive has area-type {@link StyleElement}s, but
     * no line-type StyleElements.
     *
     * {@link TextElement} is ignored, as it can be both line and area-type.
     * @param p the OSM primitive
     * @return {@code true} if primitive has area elements, but no line elements
     * @since 12700
     * @since 13810 (signature)
     */
    public static boolean hasOnlyAreaElements(IPrimitive p) {
        MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
        try {
            if (MapPaintStyles.getStyles() == null)
                return false;
            StyleElementList styles = MapPaintStyles.getStyles().generateStyles(p, 1.0, false).a;
            boolean hasAreaElement = false;
            for (StyleElement s : styles) {
                if (s instanceof TextElement) {
                    continue;
                }
                if (s instanceof AreaElement) {
                    hasAreaElement = true;
                } else {
                    return false;
                }
            }
            return hasAreaElement;
        } finally {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
        }
    }

    /**
     * Looks up a preference value and ensures the style cache is invalidated
     * as soon as this preference value is changed by the user.
     *
     * In addition, it adds an intermediate cache for the preference values,
     * as frequent preference lookup (using <code>Config.getPref().get()</code>) for
     * each primitive can be slow during rendering.
     *
     * @param key preference key
     * @param def default value
     * @return the corresponding preference value
     * @see org.openstreetmap.josm.data.Preferences#get(String, String)
     */
    public String getPreferenceCached(String key, String def) {
        String res;
        if (preferenceCache.containsKey(key)) {
            res = preferenceCache.get(key);
        } else {
            res = Config.getPref().get(key, null);
            preferenceCache.put(key, res);
        }
        return res != null ? res : def;
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (preferenceCache.containsKey(e.getKey())) {
            clearCached();
        }
    }
}
