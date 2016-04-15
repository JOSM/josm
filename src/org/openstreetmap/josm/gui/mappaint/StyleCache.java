// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Arrays;

import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.tools.Pair;

/**
 * Caches styles for a single primitive.
 * <p>
 * This object is immutable.
 */
public final class StyleCache {

    // TODO: clean up the intern pool from time to time (after purge or layer removal)
    private static final Storage<StyleCache> internPool = new Storage<>();

    public static final StyleCache EMPTY_STYLECACHE = (new StyleCache()).intern();

    private static final int PLAIN = 0;
    private static final int SELECTED = 1;

    @SuppressWarnings("unchecked")
    private final DividedScale<StyleElementList>[] states = new DividedScale[2];

    private StyleCache(StyleCache sc) {
        states[0] = sc.states[0];
        states[1] = sc.states[1];
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

        int idx = getIndex(selected);
        DividedScale<StyleElementList> ds = s.states[idx];
        if (ds == null) {
            ds = new DividedScale<>();
        }
        s.states[idx] = ds.put(o, r);
        s.intern();
        return s;
    }

    public Pair<StyleElementList, Range> getWithRange(double scale, boolean selected) {
        int idx = getIndex(selected);
        if (states[idx] == null) {
            return Pair.create(null, Range.ZERO_TO_INFINITY);
        }
        return states[idx].getWithRange(scale);
    }

    private static int getIndex(boolean selected) {
        return selected ? SELECTED : PLAIN;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(this.states);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StyleCache other = (StyleCache) obj;
        return Arrays.deepEquals(this.states, other.states);
    }

    /**
     * Like String.intern() (reduce memory consumption).
     * StyleCache must not be changed after it has been added to the intern pool.
     * @return style cache
     */
    private StyleCache intern() {
        return internPool.putUnique(this);
    }

    /**
     * Get the size of the intern pool. Only for tests!
     * @return size of the intern pool
     */
    public static int getInternPoolSize() {
        return internPool.size();
    }
}
