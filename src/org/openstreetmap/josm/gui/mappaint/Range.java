// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Objects;

/**
 * A scale interval of the form "lower &lt; x &lt;= upper" where 0 &lt;= lower &lt; upper.
 * (upper can be Double.POSITIVE_INFINITY)
 * immutable class
 */
public class Range {
    private final double lower;
    private final double upper;

    /**
     * The full scale range from zero to infinity
     */
    public static final Range ZERO_TO_INFINITY = new Range(0.0, Double.POSITIVE_INFINITY);

    /**
     * Constructs a new {@code Range}.
     * @param lower Lower bound. Must be positive or zero
     * @param upper Upper bound
     * @throws IllegalArgumentException if the range is invalid ({@code lower < 0 || lower >= upper})
     */
    public Range(double lower, double upper) {
        if (lower < 0 || lower >= upper || Double.isNaN(lower) || Double.isNaN(upper)) {
            throw new IllegalArgumentException("Invalid range: "+lower+'-'+upper);
        }
        this.lower = lower;
        this.upper = upper;
    }

    /**
     * Check if a number is contained in this range
     * @param x The number to test
     * @return <code>true</code> if it is in this range
     */
    public boolean contains(double x) {
        return lower < x && x <= upper;
    }

    /**
     * provides the intersection of 2 overlapping ranges
     * @param a first range
     * @param b second range
     * @return intersection of {@code a} and {@code b}
     */
    public static Range cut(Range a, Range b) {
        if (b.lower >= a.upper || b.upper <= a.lower)
            throw new IllegalArgumentException("Ranges do not overlap: "+a+" - "+b);
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
     * @param x value
     * @param other other range
     * @return reduced range
     */
    public Range reduceAround(double x, Range other) {
        if (!contains(x))
            throw new IllegalArgumentException(x+" is not inside "+this);
        if (other.contains(x))
            throw new IllegalArgumentException(x+" is inside "+other);

        if (x < other.lower && other.lower < upper)
            return new Range(lower, other.lower);

        if (this.lower < other.upper && other.upper < x)
            return new Range(other.upper, this.upper);

        return this;
    }

    /**
     * Gets the lower bound
     * @return The lower, exclusive, bound
     */
    public double getLower() {
        return lower;
    }

    /**
     * Gets the upper bound
     * @return The upper, inclusive, bound
     */
    public double getUpper() {
        return upper;
    }

    @Override
    public String toString() {
        return String.format("|z%.4f-%.4f", lower, upper);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Range range = (Range) o;
        return Double.compare(range.lower, lower) == 0 &&
                Double.compare(range.upper, upper) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper);
    }
}
