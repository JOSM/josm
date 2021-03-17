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

    /**
     * This exception type is for debugging #8997 and can later be replaced by AssertionError
     */
    public static class RangeViolatedError extends RuntimeException {
        /**
         * Constructs a new {@code RangeViolatedError}
         * @param message error message
         */
        public RangeViolatedError(String message) {
            super(message);
        }
    }

    /* list of boundaries for the scale ranges */
    private final List<Range> ranges;
    /* data objects for each scale range */
    private final List<T> data;

    protected DividedScale() {
        ranges = new ArrayList<>();
        ranges.add(Range.ZERO_TO_INFINITY);
        data = new ArrayList<>();
        data.add(null);
    }

    protected DividedScale(DividedScale<T> s) {
        ranges = new ArrayList<>(s.ranges);
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
            Range range = ranges.get(i);
            if (range.contains(scale)) {
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
            Range range = ranges.get(i);
            if (range.contains(scale)) {
                return new Pair<>(data.get(i), range);
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
     *    data[i-1]      data[i]      data[i+1
     * |--------------|------------|--------------|
     * (--range[i-1]--]
     *                (--range[i]--]
     *                             (--range[i+1]--]
     *                       (--------]
     *                     lower     upper
     * @param o data object
     * @param lower lower bound
     * @param upper upper bound
     */
    private void putImpl(T o, double lower, double upper) {
        int i = 0;
        while (ranges.get(i).getUpper() <= lower) {
            ++i;
        }
        Range split = ranges.get(i);
        if (split.getUpper() < upper) {
            throw new RangeViolatedError("the new range must be within a single subrange");
        } else if (data.get(i) != null) {
            throw new RangeViolatedError("the new range must be within a subrange that has no data");
        } else if (split.getLower() == lower && split.getUpper() == upper) {
            data.set(i, o);
        } else if (split.getLower() == lower) {
            ranges.set(i, new Range(split.getLower(), upper));
            ranges.add(i + 1, new Range(upper, split.getUpper()));
            data.add(i, o);
        } else if (split.getUpper() == upper) {
            ranges.set(i, new Range(split.getLower(), lower));
            ranges.add(i + 1, new Range(lower, split.getUpper()));
            data.add(i + 1, o);
        } else {
            ranges.set(i, new Range(split.getLower(), lower));
            ranges.add(i + 1, new Range(lower, upper));
            ranges.add(i + 2, new Range(upper, split.getUpper()));
            data.add(i + 1, o);
            data.add(i + 2, null);
        }
    }

    /**
     * Runs a consistency test.
     * @throws AssertionError When an invariant is broken.
     */
    public void consistencyTest() {
        if (ranges.size() < 1) throw new AssertionError(ranges);
        if (data.isEmpty()) throw new AssertionError(data);
        if (ranges.size() != data.size()) throw new AssertionError();
        if (ranges.get(0).getLower() != 0) throw new AssertionError();
        if (ranges.get(ranges.size() - 1).getUpper() != Double.POSITIVE_INFINITY) throw new AssertionError();
        for (int i = 0; i < data.size() - 1; ++i) {
            if (ranges.get(i).getUpper() != ranges.get(i + 1).getLower()) throw new AssertionError();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DividedScale<?> that = (DividedScale<?>) obj;
        return Objects.equals(ranges, that.ranges) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ranges, data);
    }

    @Override
    public String toString() {
        return "DS{" + ranges + ' ' + data + '}';
    }
}
