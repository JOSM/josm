// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matches the root cause of a {@code Throwable}.
 * This has been rejected from JUnit developers (see https://github.com/junit-team/junit4/pull/778),
 * But this is really useful, thanks to pimterry for implementing it.
 *
 * @param <T> Throwable type
 * @see <a href="https://github.com/junit-team/junit4/pull/778">Github pull request</a>
 */
public class ThrowableRootCauseMatcher<T extends Throwable> extends TypeSafeMatcher<T> {

    private final Matcher<T> fMatcher;

    /**
     * Constructs a new {@code ThrowableRootCauseMatcher}.
     * @param matcher matcher
     */
    public ThrowableRootCauseMatcher(Matcher<T> matcher) {
        fMatcher = matcher;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("exception with cause ");
        description.appendDescriptionOf(fMatcher);
    }

    @Override
    protected boolean matchesSafely(T item) {
        Throwable exception = item;
        while (exception.getCause() != null) {
            exception = exception.getCause();
        }
        return fMatcher.matches(exception);
    }

    @Override
    protected void describeMismatchSafely(T item, Description description) {
        description.appendText("cause ");
        fMatcher.describeMismatch(item.getCause(), description);
    }

    /**
     * Returns a new {@code ThrowableRootCauseMatcher} instance.
     * @param <T> Throwable type
     * @param matcher matcher
     * @return new {@code ThrowableRootCauseMatcher} instance
     */
    public static <T extends Throwable> Matcher<T> hasRootCause(final Matcher<T> matcher) {
        return new ThrowableRootCauseMatcher<>(matcher);
    }
}
