// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.tools.Pair;

/**
 * Caches styles for a single primitive.
 * Splits the range of possible scale values (0 &lt; scale &lt; +Infinity) into multiple
 * subranges, for each scale range it keeps a list of styles.
 * Immutable class, equals &amp; hashCode is required (the same for StyleList, ElemStyle
 * and its subclasses).
 */
public final class StyleCache {
    /* list of boundaries for the scale ranges */
    private final List<Double> bd;
    /* styles for each scale range */
    private final List<StyleList> data;

    private static final Storage<StyleCache> internPool = new Storage<>(); // TODO: clean up the intern pool from time to time (after purge or layer removal)

    public static final StyleCache EMPTY_STYLECACHE = (new StyleCache()).intern();

    private StyleCache() {
        bd = new ArrayList<>();
        bd.add(0.0);
        bd.add(Double.POSITIVE_INFINITY);
        data = new ArrayList<>();
        data.add(null);
    }

    private StyleCache(StyleCache s) {
        bd = new ArrayList<>(s.bd);
        data = new ArrayList<>(s.data);
    }

    /**
     * List of Styles, immutable
     */
    public static class StyleList implements Iterable<ElemStyle> {
        private List<ElemStyle> lst;

        /**
         * Constructs a new {@code StyleList}.
         */
        public StyleList() {
            lst = new ArrayList<>();
        }

        public StyleList(ElemStyle... init) {
            lst = new ArrayList<>(Arrays.asList(init));
        }

        public StyleList(Collection<ElemStyle> sl) {
            lst = new ArrayList<>(sl);
        }

        public StyleList(StyleList sl, ElemStyle s) {
            lst = new ArrayList<>(sl.lst);
            lst.add(s);
        }

        @Override
        public Iterator<ElemStyle> iterator() {
            return lst.iterator();
        }

        public boolean isEmpty() {
            return lst.isEmpty();
        }

        public int size() {
            return lst.size();
        }

        @Override
        public String toString() {
            return lst.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass())
                return false;
            final StyleList other = (StyleList) obj;
            return Objects.equals(lst, other.lst);
        }

        @Override
        public int hashCode() {
            return lst.hashCode();
        }
    }

    /**
     * looks up styles for a certain scale value
     */
    public StyleList get(double scale) {
        if (scale <= 0)
            throw new IllegalArgumentException("scale must be <= 0 but is "+scale);
        for (int i=0; i<data.size(); ++i) {
            if (bd.get(i) < scale && scale <= bd.get(i+1)) {
                return data.get(i);
            }
        }
        throw new AssertionError();
    }

    /**
     * looks up styles for a certain scale value and additionally returns
     * the scale range for the returned styles
     */
    public Pair<StyleList, Range> getWithRange(double scale) {
        if (scale <= 0)
            throw new IllegalArgumentException("scale must be <= 0 but is "+scale);
        for (int i=0; i<data.size(); ++i) {
            if (bd.get(i) < scale && scale <= bd.get(i+1)) {
                return new Pair<>(data.get(i), new Range(bd.get(i), bd.get(i+1)));
            }
        }
        throw new AssertionError();
    }

    public StyleCache put(StyleList sl, Range r) {
        return put(sl, r.getLower(), r.getUpper());
    }

    /**
     * add a new styles to the cache. this is only possible, if
     * for this scale range, there is nothing in the cache yet.
     */
    public StyleCache put(StyleList sl, double lower, double upper) {
        StyleCache s = new StyleCache(this);
        s.putImpl(sl, lower, upper);
        s.consistencyTest();
        return s.intern();
    }

    // this exception type is for debugging #8997 and can later be replaced
    // by AssertionError
    public static class RangeViolatedError extends Error {
    }

    /**
     * ASCII-art explanation:
     *
     *              data[i]
     *  --|-------|---------|--
     * bd[i-1]  bd[i]    bd[i+1]
     *
     *         (--------]
     *       lower     upper
     */
    private void putImpl(StyleList sl, double lower, double upper) {
        int i=0;
        while (bd.get(i) < lower) {
            ++i;
        }
        if (bd.get(i) == lower) {
            if (upper > bd.get(i+1))
                throw new RangeViolatedError();
            if (data.get(i) != null)
                throw new AssertionError("the new range must be within a subrange that has no data");

            if (bd.get(i+1) == upper) {
                //  --|-------|--------|--
                //   i-1      i       i+1
                //            (--------]
                data.set(i, sl);
            } else {
                //  --|-------|--------|--
                //   i-1      i       i+1
                //            (-----]
                bd.add(i+1, upper);
                data.add(i, sl);
            }
            return;
        } else {
            if (bd.get(i) < upper)
                throw new AssertionError("the new range must be within a single subrange");
            if (data.get(i-1) != null)
                throw new AssertionError();

            //  --|-------|--------|--
            //   i-1      i       i+1
            //       (--]   or
            //       (----]
            bd.add(i, lower);
            data.add(i, sl);

            //  --|--|----|--------|--
            //   i-1 i   i+1      i+2
            //       (--]
            if (bd.get(i+1) > upper) {
                bd.add(i+1, upper);
                data.add(i+1, null);
            }
            return;
        }
    }

    public void consistencyTest() {
        if (bd.size() < 2) throw new AssertionError(bd);
        if (data.isEmpty()) throw new AssertionError(data);
        if (bd.size() != data.size() + 1) throw new AssertionError();
        if (bd.get(0) != 0) throw new AssertionError();
        if (bd.get(bd.size() - 1) != Double.POSITIVE_INFINITY) throw new AssertionError();
        for (int i=0; i<data.size() - 1; ++i) {
            if (bd.get(i) >= bd.get(i + 1)) throw new AssertionError();
        }
    }

    /**
     * Like String.intern() (reduce memory consumption).
     * StyleCache must not be changed after it has
     * been added to the intern pool.
     */
    public StyleCache intern() {
        return internPool.putUnique(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final StyleCache other = (StyleCache) obj;
        return bd.equals(other.bd) && data.equals(other.data);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + bd.hashCode();
        hash = 23 * hash + data.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "SC{" + bd + ' ' + data + '}';
    }
}
