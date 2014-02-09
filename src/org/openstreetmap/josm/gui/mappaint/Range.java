// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

/**
 * An interval of the form "lower &lt; x &lt;= upper" where 0 &lt;= lower &lt; upper.
 * (upper can be Double.POSITIVE_INFINITY)
 * immutable class
 */
public class Range {
    private double lower;
    private double upper;

    public static final Range ZERO_TO_INFINITY = new Range(0.0, Double.POSITIVE_INFINITY);

    public Range(double lower, double upper) {
        if (lower < 0 || lower >= upper)
            throw new IllegalArgumentException();
        this.lower = lower;
        this.upper = upper;
    }

    public boolean contains(double x) {
        return lower < x && x <= upper;
    }

    /**
     * provides the intersection of 2 overlapping ranges
     */
    public static Range cut(Range a, Range b) {
        if (b.lower >= a.upper || b.upper <= a.lower)
            throw new IllegalArgumentException();
        return new Range(Math.max(a.lower, b.lower), Math.min(a.upper, b.upper));
    }

    /**
     * under the premise, that x is within this range,
     * and not within the other range, it shrinks this range in a way
     * to exclude the other range, but still contain x.
     *
     * x                  |
     *
     * this   (------------------------------]
     *
     * other                   (-------]  or
     *                         (-----------------]
     *
     * result (----------------]
     */
    public Range reduceAround(double x, Range other) {
        if (!contains(x))
            throw new IllegalArgumentException();
        if (other.contains(x))
            throw new IllegalArgumentException();

        if (x < other.lower && other.lower < upper)
            return new Range(lower, other.lower);

        if (this.lower < other.upper && other.upper < x)
            return new Range(other.upper, this.upper);

        return this;
    }

    public double getLower() {
        return lower;
    }

    public double getUpper() {
        return upper;
    }

    @Override
    public String toString() {
        return String.format("|s%s-%s", lower, upper);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range range = (Range) o;

        if (Double.compare(range.lower, lower) != 0) return false;
        if (Double.compare(range.upper, upper) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(lower);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(upper);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}