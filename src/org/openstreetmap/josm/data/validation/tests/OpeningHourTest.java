// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.ParseException;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.Util;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test.TagTest;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Tests the correct usage of the opening hour syntax of the tags
 * {@code opening_hours}, {@code collection_times}, {@code service_times} according to
 * <a href="https://github.com/simonpoole/OpeningHoursParser">OpeningHoursParser</a>.
 *
 * @since 6370 (using opening_hours.js), 15978 (using OpeningHoursParser)
 */
public class OpeningHourTest extends TagTest {

    /**
     * Constructs a new {@code OpeningHourTest}.
     */
    public OpeningHourTest() {
        super(tr("Opening hours syntax"),
                tr("This test checks the correct usage of the opening hours syntax."));
    }

    /**
     * An error concerning invalid syntax for an "opening_hours"-like tag.
     */
    public class OpeningHoursTestError {
        private final Severity severity;
        private final String message;
        private final String prettifiedValue;

        /**
         * Constructs a new {@code OpeningHoursTestError} with a known prettified value.
         * @param message The error message
         * @param severity The error severity
         * @param prettifiedValue The prettified value
         */
        public OpeningHoursTestError(String message, Severity severity, String prettifiedValue) {
            this.message = message;
            this.severity = severity;
            this.prettifiedValue = prettifiedValue;
        }

        /**
         * Returns the real test error given to JOSM validator.
         * @param p The incriminated OSM primitive.
         * @param key The incriminated key, used for display.
         * @return The real test error given to JOSM validator. Can be fixable or not if a prettified values has been determined.
         */
        public TestError getTestError(final OsmPrimitive p, final String key) {
            final TestError.Builder error = TestError.builder(OpeningHourTest.this, severity, 2901)
                    .message(tr("Opening hours syntax"), message) // todo obtain English message for ignore functionality
                    .primitives(p);
            if (prettifiedValue == null || prettifiedValue.equals(p.get(key))) {
                return error.build();
            } else {
                return error.fix(() -> new ChangePropertyCommand(p, key, prettifiedValue)).build();
            }
        }

        /**
         * Returns the error message.
         * @return The error message.
         */
        public String getMessage() {
            return message;
        }

        /**
         * Returns the prettified value.
         * @return The prettified value.
         */
        public String getPrettifiedValue() {
            return prettifiedValue;
        }

        /**
         * Returns the error severity.
         * @return The error severity.
         */
        public Severity getSeverity() {
            return severity;
        }

        @Override
        public String toString() {
            return getMessage() + " => " + getPrettifiedValue();
        }
    }

    /**
     * Checks for a correct usage of the opening hour syntax of the {@code value} given,
     * and returns a list containing validation errors or an empty list. Null values result in an empty list.
     * @param key the OSM key (should be "opening_hours", "collection_times" or "service_times"). Used in error message
     * @param value the opening hour value to be checked.
     * @return a list of {@link TestError} or an empty list
     */
    public List<OpeningHoursTestError> checkOpeningHourSyntax(final String key, final String value) {
        return checkOpeningHourSyntax(key, value, false, Locale.getDefault());
    }

    /**
     * Checks for a correct usage of the opening hour syntax of the {@code value} given,
     * and returns a list containing validation errors or an empty list. Null values result in an empty list.
     * @param key the OSM key (should be "opening_hours", "collection_times" or "service_times").
     * @param value the opening hour value to be checked.
     * @param ignoreOtherSeverity whether to ignore errors with {@link Severity#OTHER}.
     * @param locale the locale code used for localizing messages
     * @return a list of {@link TestError} or an empty list
     */
    public List<OpeningHoursTestError> checkOpeningHourSyntax(final String key, final String value, boolean ignoreOtherSeverity, Locale locale) {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }

        ch.poole.openinghoursparser.I18n.setLocale(locale);
        String prettifiedValue = null;
        try {
            final List<Rule> rules = new OpeningHoursParser(new StringReader(value)).rules(false);
            prettifiedValue = Util.rulesToOpeningHoursString(rules);
            if (!Objects.equals(value, prettifiedValue)) {
                // parse again in strict mode for detailed message
                new OpeningHoursParser(new StringReader(value)).rules(true);
            }
        } catch (ParseException e) {
            return Collections.singletonList(new OpeningHoursTestError(e.getMessage(), Severity.WARNING, prettifiedValue));
        }

        if (ignoreOtherSeverity || Objects.equals(value, prettifiedValue)) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(
                    new OpeningHoursTestError(tr("{0} value can be prettified", key), Severity.OTHER, prettifiedValue));
        }
    }

    protected void check(final OsmPrimitive p, final String key) {
        for (OpeningHoursTestError e : checkOpeningHourSyntax(key, p.get(key))) {
            errors.add(e.getTestError(p, key));
        }
    }

    @Override
    public void check(final OsmPrimitive p) {
        check(p, "opening_hours");
        check(p, "collection_times");
        check(p, "service_times");
    }
}
