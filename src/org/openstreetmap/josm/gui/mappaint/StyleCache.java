// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.openstreetmap.josm.tools.Pair;

/**
 * Caches styles for a single primitive.
 * <p>
 * This object is immutable.
 */
public final class StyleCache {

    // TODO: clean up the intern pool from time to time (after purge or layer removal)
    private static final Map<StyleCache, StyleCache> internPool = new ConcurrentHashMap<>();

    /**
     * An empty style cache entry
     */
    public static final StyleCache EMPTY_STYLECACHE = new StyleCache().intern();

    private DividedScale<StyleElementList> plainStyle;
    private DividedScale<StyleElementList> selectedStyle;

    private StyleCache(StyleCache sc) {
        plainStyle = sc.plainStyle;
        selectedStyle = sc.selectedStyle;
    }

    private StyleCache() {
    }

    /**
     * Creates a new copy of this style cache with a new entry added.
     * @param o The style to cache.
     * @param r The range the style is for.
     * @param selected The style list we should use (selected/unselected)
     * @return The new object.
     */
    public StyleCache put(StyleElementList o, Range r, boolean selected) {
        StyleCache s = new StyleCache(this);

        if (selected) {
            s.selectedStyle = scale(s.selectedStyle).put(o, r);
        } else {
            s.plainStyle = scale(s.plainStyle).put(o, r);
        }
        return s.intern();
    }

    private static DividedScale<StyleElementList> scale(DividedScale<StyleElementList> scale) {
        return scale == null ? new DividedScale<>() : scale;
    }

    /**
     * Get the style for a specific style. Returns the range as well.
     * @param scale The current scale
     * @param selected true to get the state for a selected element,
     * @return The style and the range it is valid for.
     */
    public Pair<StyleElementList, Range> getWithRange(double scale, boolean selected) {
        DividedScale<StyleElementList> style = selected ? selectedStyle : plainStyle;
        return style != null ? style.getWithRange(scale) : Pair.create(null, Range.ZERO_TO_INFINITY);
    }

    @Override
    public String toString() {
        return "StyleCache{PLAIN: " + plainStyle + " SELECTED: " + selectedStyle + "}";
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(plainStyle) + Objects.hashCode(selectedStyle);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final StyleCache other = (StyleCache) obj;
        return Objects.equals(plainStyle, other.plainStyle) && Objects.equals(selectedStyle, other.selectedStyle);
    }

    /**
     * Like String.intern() (reduce memory consumption).
     * StyleCache must not be changed after it has been added to the intern pool.
     * @return style cache
     */
    private StyleCache intern() {
        return internPool.computeIfAbsent(this, Function.identity());
    }

    /**
     * Clears the style cache. This should only be used for testing.
     * It may be removed some day and replaced by a WeakReference implementation that automatically forgets old entries.
     */
    static void clearStyleCachePool() {
        internPool.clear();
    }

    /**
     * Get the size of the intern pool. Only for tests!
     * @return size of the intern pool
     */
    public static int getInternPoolSize() {
        return internPool.size();
    }
}
