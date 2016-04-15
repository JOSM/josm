// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.tools.Pair;

/**
 * Splits the range of possible scale values (0 &lt; scale &lt; +Infinity) into
 * multiple subranges, for each scale range it keeps a data object of a certain
 * type T (can be null).
 *
 * Used for caching style information for different zoom levels.
 *
 * Immutable class, equals &amp; hashCode is required (the same for
 * {@link StyleElementList}, {@link StyleElement} and its subclasses).
 *
 * @param <T> the type of the data objects
 */
public class DividedScale<T> {

    // this exception type is for debugging #8997 and can later be replaced
    // by AssertionError
    public static class RangeViolatedError extends Error {
        public RangeViolatedError() {
        }

        public RangeViolatedError(String message) {
            super(message);
        }
    }

    /* list of boundaries for the scale ranges */
    private final List<Double> bd;
    /* data objects for each scale range */
    private final List<T> data;

    protected DividedScale() {
        bd = new ArrayList<>();
        bd.add(0.0);
        bd.add(Double.POSITIVE_INFINITY);
        data = new ArrayList<>();
        data.add(null);
    }

    protected DividedScale(DividedScale<T> s) {
        bd = new ArrayList<>(s.bd);
        data = new ArrayList<>(s.data);
    }

    /**
     * Looks up the data object for a certain scale value.
     *
     * @param scale scale
     * @return the data object at the given scale, can be null
     */
    public T get(double scale) {
        if (scale <= 0)
            throw new IllegalArgumentException("scale must be <= 0 but is "+scale);
        for (int i = 0; i < data.size(); ++i) {
            if (bd.get(i) < scale && scale <= bd.get(i+1)) {
                return data.get(i);
            }
        }
        throw new AssertionError();
    }

    /**
     * Looks up the data object for a certain scale value and additionally returns
     * the scale range where the object is valid.
     *
     * @param scale scale
     * @return pair containing data object and range
     */
    public Pair<T, Range> getWithRange(double scale) {
        if (scale <= 0)
            throw new IllegalArgumentException("scale must be <= 0 but is "+scale);
        for (int i = 0; i < data.size(); ++i) {
            if (bd.get(i) < scale && scale <= bd.get(i+1)) {
                return new Pair<>(data.get(i), new Range(bd.get(i), bd.get(i+1)));
            }
        }
        throw new AssertionError();
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
    public DividedScale<T> put(T o, Range r) {
        DividedScale<T> s = new DividedScale<>(this);
        s.putImpl(o, r.getLower(), r.getUpper());
        s.consistencyTest();
        return s;
    }

    /**
     * Implementation of the <code>put</code> operation.
     *
     * ASCII-art explanation:
     *
     *              data[i]
     *  --|-------|---------|--
     * bd[i-1]  bd[i]    bd[i+1]
     *
     *         (--------]
     *       lower     upper
     * @param o data object
     * @param lower lower bound
     * @param upper upper bound
     */
    private void putImpl(T o, double lower, double upper) {
        int i = 0;
        while (bd.get(i) < lower) {
            ++i;
        }
        if (bd.get(i) == lower) {
            if (upper > bd.get(i+1))
                throw new RangeViolatedError("the new range must be within a single subrange (1)");
            if (data.get(i) != null)
                throw new RangeViolatedError("the new range must be within a subrange that has no data");

            if (bd.get(i+1) == upper) {
                //  --|-------|--------|--
                //   i-1      i       i+1
                //            (--------]
                data.set(i, o);
            } else {
                //  --|-------|--------|--
                //   i-1      i       i+1
                //            (-----]
                bd.add(i+1, upper);
                data.add(i, o);
            }
        } else {
            if (bd.get(i) < upper)
                throw new RangeViolatedError("the new range must be within a single subrange (2)");
            if (data.get(i-1) != null)
                throw new AssertionError();

            //  --|-------|--------|--
            //   i-1      i       i+1
            //       (--]   or
            //       (----]
            bd.add(i, lower);
            data.add(i, o);

            //  --|--|----|--------|--
            //   i-1 i   i+1      i+2
            //       (--]
            if (bd.get(i+1) > upper) {
                bd.add(i+1, upper);
                data.add(i+1, null);
            }
        }
    }

    /**
     * Runs a consistency test.
     * @throws AssertionError When an invariant is broken.
     */
    public void consistencyTest() {
        if (bd.size() < 2) throw new AssertionError(bd);
        if (data.isEmpty()) throw new AssertionError(data);
        if (bd.size() != data.size() + 1) throw new AssertionError();
        if (bd.get(0) != 0) throw new AssertionError();
        if (bd.get(bd.size() - 1) != Double.POSITIVE_INFINITY) throw new AssertionError();
        for (int i = 0; i < data.size() - 1; ++i) {
            if (bd.get(i) >= bd.get(i + 1)) throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DividedScale<?> that = (DividedScale<?>) obj;
        return Objects.equals(bd, that.bd) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bd, data);
    }

    @Override
    public String toString() {
        return "DS{" + bd + ' ' + data + '}';
    }
}
