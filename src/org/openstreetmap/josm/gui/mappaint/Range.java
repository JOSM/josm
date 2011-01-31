// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

/**
 * An interval of the form "lower < x <= upper" where 0 <= lower < upper.
 * (upper can be Double.POSITIVE_INFINITY)
 * immutable class
 */
public class Range {
    private double lower;
    private double upper;

    public Range() {
        this.lower = 0;
        this.upper = Double.POSITIVE_INFINITY;
    }

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
}