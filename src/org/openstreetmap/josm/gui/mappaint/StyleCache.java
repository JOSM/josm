// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.Storage;

/**
 * Caches styles for a single primitive.
 */
public final class StyleCache extends DividedScale<StyleElementList> {

    // TODO: clean up the intern pool from time to time (after purge or layer removal)
    private static final Storage<StyleCache> internPool = new Storage<>();

    public static final StyleCache EMPTY_STYLECACHE = (new StyleCache()).intern();

    private StyleCache(StyleCache sc) {
        super(sc);
    }

    private StyleCache() {
        super();
    }

    /**
     * Add data object which is valid for the given range.
     *
     * This is only possible, if there is no data for the given range yet.
     *
     * @param o data object
     * @param r the valid range
     * @return a new, updated, <code>DividedScale</code> object
     */
    @Override
    public StyleCache put(StyleElementList o, Range r) {
        StyleCache s = new StyleCache(this);
        s.putImpl(o, r.getLower(), r.getUpper());
        s.consistencyTest();
        s.intern();
        return s;
    }

    /**
     * Like String.intern() (reduce memory consumption).
     * StyleCache must not be changed after it has been added to the intern pool.
     * @return style cache
     */
    private StyleCache intern() {
        return internPool.putUnique(this);
    }
}
