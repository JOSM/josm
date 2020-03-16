// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
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

    private static final Collection<String> KEYS_TO_CHECK = Arrays.asList("opening_hours", "collection_times", "service_times");

    /**
     * Constructs a new {@code OpeningHourTest}.
     */
    public OpeningHourTest() {
        super(tr("Opening hours syntax"),
                tr("This test checks the correct usage of the opening hours syntax."));
    }

    /**
     * Returns the real test error given to JOSM validator.
     * @param severity The error severity
     * @param message The error message
     * @param key The incriminated key, used for display.
     * @param prettifiedValue The prettified value
     * @param p The incriminated OSM primitive.
     * @return The real test error given to JOSM validator. Can be fixable or not if a prettified values has been determined.
     */
    private TestError createTestError(Severity severity, String message, String key, String prettifiedValue, OsmPrimitive p) {
        final TestError.Builder error = TestError.builder(this, severity, 2901)
                .message(tr("Opening hours syntax"), message) // todo obtain English message for ignore functionality
                .primitives(p);
        if (prettifiedValue == null || prettifiedValue.equals(p.get(key))) {
            return error.build();
        } else {
            return error.fix(() -> new ChangePropertyCommand(p, key, prettifiedValue)).build();
        }
    }

    /**
     * Checks for a correct usage of the opening hour syntax of the {@code value} given,
     * and returns a list containing validation errors or an empty list. Null values result in an empty list.
     * @param key the OSM key (should be "opening_hours", "collection_times" or "service_times"). Used in error message
     * @param value the opening hour value to be checked.
     * @return a list of {@link TestError} or an empty list
     */
    public List<TestError> checkOpeningHourSyntax(final String key, final String value) {
        return checkOpeningHourSyntax(key, value, null, Locale.getDefault());
    }

    /**
     * Checks for a correct usage of the opening hour syntax of the {@code value} given,
     * and returns a list containing validation errors or an empty list. Null values result in an empty list.
     * @param key the OSM key (should be "opening_hours", "collection_times" or "service_times").
     * @param value the opening hour value to be checked.
     * @param p the primitive to check/fix.
     * @param locale the locale code used for localizing messages
     * @return a list of {@link TestError} or an empty list
     */
    List<TestError> checkOpeningHourSyntax(final String key, final String value, OsmPrimitive p, Locale locale) {
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
            return Collections.singletonList(createTestError(Severity.WARNING, e.getMessage(), key, prettifiedValue, p));
        }

        if (!includeOtherSeverityChecks() || Objects.equals(value, prettifiedValue) || p == null) {
            return Collections.emptyList();
        } else {
            final String message = tr("{0} value can be prettified", key);
            return Collections.singletonList(createTestError(Severity.OTHER, message, key, prettifiedValue, p));
        }
    }

    @Override
    public void check(final OsmPrimitive p) {
        if (p.isTagged()) {
            for (String key : KEYS_TO_CHECK) {
                errors.addAll(checkOpeningHourSyntax(key, p.get(key), p, Locale.getDefault()));
            }
        }
    }
}
