// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import static org.openstreetmap.josm.testutils.ThrowableRootCauseMatcher.hasRootCause;

import org.hamcrest.Matcher;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * The {@code ExpectedRootException} behaves exactly as JUnit's {@link ExpectedException} rule.
 * This class is needed to add {@link #expectRootCause} method, which has been rejected by JUnit developers,
 * and {@code ExpectedException} cannot be extended because it has a private constructor.
 * @see <a href="https://github.com/junit-team/junit4/pull/778">Github pull request</a>
 * @deprecated Use matchers instead with the return from {@link org.junit.jupiter.api.Assertions#assertThrows}
 */
@Deprecated
public final class ExpectedRootException implements TestRule {

    private final ExpectedException rule = ExpectedException.none();

    /**
     * Returns a {@linkplain TestRule rule} that expects no exception to be thrown (identical to behavior without this rule).
     * @return {@code ExpectedRootException} instance
     */
    @SuppressFBWarnings("NM_CLASS_NOT_EXCEPTION")
    public static ExpectedRootException none() {
        return new ExpectedRootException();
    }

    private ExpectedRootException() {
    }

    /**
     * Specifies the failure message for tests that are expected to throw an exception but do not throw any.
     * You can use a {@code %s} placeholder for the description of the expected exception.
     * E.g. "Test doesn't throw %s." will fail with the error message "Test doesn't throw an instance of foo.".
     *
     * @param message exception detail message
     * @return the rule itself
     */
    public ExpectedRootException reportMissingExceptionWithMessage(String message) {
        rule.reportMissingExceptionWithMessage(message);
        return this;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return rule.apply(base, description);
    }

    /**
     * Verify that your code throws an exception that is an instance of specific {@code type}.
     * <pre> &#064;Test
     * public void throwsExceptionWithSpecificType() {
     *     thrown.expect(NullPointerException.class);
     *     throw new NullPointerException();
     * }</pre>
     * @param type Throwable type
     * @return {@code this}
     */
    public ExpectedRootException expect(Class<? extends Throwable> type) {
        rule.expect(type);
        return this;
    }

    /**
     * Verify that your code throws an exception whose message contains a specific text.
     * <pre> &#064;Test
     * public void throwsExceptionWhoseMessageContainsSpecificText() {
     *     thrown.expectMessage(&quot;happened&quot;);
     *     throw new NullPointerException(&quot;What happened?&quot;);
     * }</pre>
     * @param substring substring to expect in error message
     * @return {@code this}
     */
    public ExpectedRootException expectMessage(String substring) {
        rule.expectMessage(substring);
        return this;
    }

    /**
     * Verify that your code throws an exception whose immediate cause is matched by the given Hamcrest matcher.
     * <pre> &#064;Test
     * public void throwsExceptionWhoseCauseCompliesWithMatcher() {
     *     NullPointerException rootCause = new NullPointerException();
     *     IllegalStateException immediateCause = new IllegalStateException(rootCause);
     *     thrown.expectCause(isA(NullPointerException.class));
     *     throw new IllegalArgumentException(immediateCause);
     * }</pre>
     * @param expectedCause expected cause
     * @return {@code this}
     */
    public ExpectedRootException expectCause(Matcher<? extends Throwable> expectedCause) {
        rule.expectCause(expectedCause);
        return this;
    }

    /**
     * Verify that you code throws an exception whose root cause is matched by the given Hamcrest matcher.
     * <pre> &#064;Test
     * public void throwsExceptionWhoseRootCauseCompliesWithMatcher() {
     *     NullPointerException rootCause = new NullPointerException();
     *     IllegalStateException immediateCause = new IllegalStateException(rootCause);
     *     thrown.expectRootCause(isA(NullPointerException.class));
     *     throw new IllegalArgumentException(immediateCause);
     * }</pre>
     * @param expectedRootCause expected root cause
     * @return {@code this}
     */
    public ExpectedRootException expectRootCause(Matcher<? extends Throwable> expectedRootCause) {
        rule.expect(hasRootCause(expectedRootCause));
        return this;
    }
}
