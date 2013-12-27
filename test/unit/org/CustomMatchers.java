// License: GPL. For details, see LICENSE file.
package org;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Ignore;
import org.openstreetmap.josm.tools.Predicate;

import java.util.Collection;

@Ignore("no test")
public class CustomMatchers {

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

}
