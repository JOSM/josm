// License: GPL. For details, see LICENSE file.
package org;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Custom matchers for unit tests.
 */
@Ignore("no test")
public final class CustomMatchers {

    /**
     * Error mode, denoting different ways to calculate the error of a number relative to an expected value.
     */
    public enum ErrorMode {
        /**
         * absolute error (difference of actual and expected value)
         */
        ABSOLUTE,

        /**
         * relative error (difference divided by the expected value)
         */
        RELATIVE
    }

    private CustomMatchers() {
        // Hide constructor for utility classes
    }

    /**
     * Matcher for a predicate.
     * @param <T> type of elements
     * @param predicate the predicate
     * @return matcher for a predicate
     */
    public static <T> Matcher<? extends T> forPredicate(final Predicate<T> predicate) {
        return new TypeSafeMatcher<T>() {

            @Override
            protected boolean matchesSafely(T item) {
                return predicate.test(item);
            }

            @Override
            public void describeTo(Description description) {
                description.appendValue(predicate);
            }
        };
    }

    /**
     * Matcher for a collection of a given size.
     * @param size of collection
     * @return matcher for a collection of a given size
     */
    public static Matcher<Collection<?>> hasSize(final int size) {
        return new TypeSafeMatcher<Collection<?>>() {
            @Override
            protected boolean matchesSafely(Collection<?> collection) {
                return collection != null && collection.size() == size;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("hasSize(").appendValue(size).appendText(")");
            }
        };
    }

    /**
     * Matcher for an empty collection.
     * @return matcher for an empty collection
     */
    public static Matcher<Collection<?>> isEmpty() {
        return new TypeSafeMatcher<Collection<?>>() {
            @Override
            protected boolean matchesSafely(Collection<?> collection) {
                return collection != null && collection.isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("isEmpty()");
            }
        };
    }

    /**
     * Matcher for a point at a given location.
     * @param expected expected location
     * @return matcher for a point at a given location
     */
    public static Matcher<? super Point2D> is(final Point2D expected) {
        return new CustomTypeSafeMatcher<Point2D>(Objects.toString(expected)) {
            @Override
            protected boolean matchesSafely(Point2D actual) {
                return expected.distance(actual) <= 0.0000001;
            }
        };
    }

    /**
     * Matcher for a point at a given location.
     * @param expected expected location
     * @return matcher for a point at a given location
     */
    public static Matcher<? super LatLon> is(final LatLon expected) {
        return new CustomTypeSafeMatcher<LatLon>(Objects.toString(expected)) {
            @Override
            protected boolean matchesSafely(LatLon actual) {
                return Math.abs(expected.getX() - actual.getX()) <= LatLon.MAX_SERVER_PRECISION
                        && Math.abs(expected.getY() - actual.getY()) <= LatLon.MAX_SERVER_PRECISION;
            }
        };
    }

    /**
     * Matcher for a point at a given location.
     * @param expected expected location
     * @return matcher for a point at a given location
     */
    public static Matcher<? super EastNorth> is(final EastNorth expected) {
        return new CustomTypeSafeMatcher<EastNorth>(Objects.toString(expected)) {
            @Override
            protected boolean matchesSafely(EastNorth actual) {
                return Math.abs(expected.getX() - actual.getX()) <= LatLon.MAX_SERVER_PRECISION
                        && Math.abs(expected.getY() - actual.getY()) <= LatLon.MAX_SERVER_PRECISION;
            }
        };
    }

    /**
     * Matcher for a {@link Bounds} object
     * @param expected expected bounds
     * @param tolerance acceptable deviation (epsilon)
     * @return Matcher for a {@link Bounds} object
     */
    public static Matcher<Bounds> is(final Bounds expected, double tolerance) {
        return new TypeSafeMatcher<Bounds>() {
           @Override
           public void describeTo(Description description) {
              description.appendText("is ")
                      .appendValue(expected)
                      .appendText(" (tolarance: " + tolerance + ")");
           }

           @Override
           protected void describeMismatchSafely(Bounds bounds, Description mismatchDescription) {
              mismatchDescription.appendText("was ").appendValue(bounds);
           }

           @Override
           protected boolean matchesSafely(Bounds bounds) {
              return Math.abs(expected.getMinLon() - bounds.getMinLon()) <= tolerance &&
                    Math.abs(expected.getMinLat() - bounds.getMinLat()) <= tolerance &&
                    Math.abs(expected.getMaxLon() - bounds.getMaxLon()) <= tolerance &&
                    Math.abs(expected.getMaxLat() - bounds.getMaxLat()) <= tolerance;
           }
        };
    }

    /**
     * Matcher for a floating point number.
     * @param expected expected value
     * @param errorMode the error mode
     * @param tolerance admissible error
     * @return Matcher for a floating point number
     */
    public static Matcher<Double> isFP(final double expected, ErrorMode errorMode, double tolerance) {
        return new TypeSafeMatcher<Double>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is ")
                        .appendValue(expected)
                        .appendText(" (tolarance")
                        .appendText(errorMode == ErrorMode.RELATIVE ? ", relative:" : ":")
                        .appendText(Double.toString(tolerance))
                        .appendText(")");
            }

            @Override
            protected void describeMismatchSafely(Double was, Description mismatchDescription) {
                mismatchDescription.appendText("was ").appendValue(was);
                if (errorMode == ErrorMode.RELATIVE) {
                    mismatchDescription.appendText(" (actual relative error: ")
                            .appendText(String.format(Locale.US, "%.2e", Math.abs((was - expected) / expected)))
                            .appendText(")");
                }
            }

            @Override
            protected boolean matchesSafely(Double x) {
                switch (errorMode) {
                    case ABSOLUTE:
                        return Math.abs(x - expected) <= tolerance;
                    case RELATIVE:
                        return Math.abs((x - expected) / expected) <= tolerance;
                    default:
                        throw new AssertionError();
                }
            }
        };
    }

    /**
     * Matcher for a floating point number.
     * @param expected expected value
     * @param tolerance admissible error (absolute)
     * @return Matcher for a floating point number
     */
    public static Matcher<Double> isFP(final double expected, double tolerance) {
        return isFP(expected, ErrorMode.ABSOLUTE, tolerance);
    }

    /**
     * Matcher for a floating point number.
     * @param expected expected value
     * @return Matcher for a floating point number
     */
    public static Matcher<Double> isFP(final double expected) {
        return isFP(expected, 1e-8);
    }
}
