// License: GPL. For details, see LICENSE file.
package org;

import java.awt.geom.Point2D;
import java.util.Collection;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Predicate;

/**
 * Custom matchers for unit tests.
 */
@Ignore("no test")
public final class CustomMatchers {

    private CustomMatchers() {
        // Hide constructor for utility classes
    }

    /**
     * Matcher for a predicate.
     * @param predicate the predicate
     * @return matcher for a predicate
     */
    public static <T> Matcher<? extends T> forPredicate(final Predicate<T> predicate) {
        return new TypeSafeMatcher<T>() {

            @Override
            protected boolean matchesSafely(T item) {
                return predicate.evaluate(item);
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
        return new CustomTypeSafeMatcher<Point2D>("the same Point2D") {
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
        return new CustomTypeSafeMatcher<LatLon>("the same LatLon") {
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
        return new CustomTypeSafeMatcher<EastNorth>("the same EastNorth") {
            @Override
            protected boolean matchesSafely(EastNorth actual) {
                return Math.abs(expected.getX() - actual.getX()) <= LatLon.MAX_SERVER_PRECISION
                        && Math.abs(expected.getY() - actual.getY()) <= LatLon.MAX_SERVER_PRECISION;
            }
        };
    }
}
