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
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test.TagTest;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

import ch.poole.openinghoursparser.OpeningHoursParseException;
import ch.poole.openinghoursparser.OpeningHoursParser;
import ch.poole.openinghoursparser.Rule;
import ch.poole.openinghoursparser.Util;

/**
 * Tests the correct usage of the opening hour syntax of the tags
 * {@code opening_hours}, {@code collection_times}, {@code service_times} according to
 * <a href="https://github.com/simonpoole/OpeningHoursParser">OpeningHoursParser</a>.
 *
 * @since 6370 (using opening_hours.js), 15978 (using OpeningHoursParser)
 */
public class OpeningHourTest extends TagTest {

    private static final Collection<String> KEYS_TO_CHECK = Arrays.asList("opening_hours", "collection_times", "service_times");
    private static final BooleanProperty PREF_STRICT_MODE =
            new BooleanProperty(ValidatorPrefHelper.PREFIX + "." + OpeningHourTest.class.getSimpleName() + "." + "strict", false);
    private final JCheckBox checkboxStrictMode = new JCheckBox(tr("Enable strict mode."));

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
     * @param value The incriminated value, used for comparison with prettified value.
     * @param prettifiedValue The prettified value
     * @param p The incriminated OSM primitive.
     * @return The real test error given to JOSM validator. Can be fixable or not if a prettified values has been determined.
     */
    private TestError createTestError(Severity severity, String message, String key, String value, String prettifiedValue, OsmPrimitive p) {
        final TestError.Builder error = TestError.builder(this, severity, 2901)
                .message(tr("Opening hours syntax"), message) // todo obtain English message for ignore functionality
                .primitives(p != null ? new OsmPrimitive[] {p} : new OsmPrimitive[] {});
        if (p == null || prettifiedValue == null || prettifiedValue.equals(value)) {
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
        if (Utils.isEmpty(value)) {
            return Collections.emptyList();
        }

        ch.poole.openinghoursparser.I18n.setLocale(locale);
        String prettifiedValue = null;
        try {
            final boolean strict = PREF_STRICT_MODE.get();
            final List<Rule> rules = new OpeningHoursParser(new StringReader(value)).rules(strict, false);
            prettifiedValue = Util.rulesToOpeningHoursString(rules);
            if (!Objects.equals(value, prettifiedValue) && !strict) {
                // parse again in strict mode for detailed message
                new OpeningHoursParser(new StringReader(value)).rules(true, false);
            }
        } catch (OpeningHoursParseException e) {
            String message = e.getExceptions().stream()
                    .map(OpeningHoursParseException::getMessage)
                    .distinct()
                    .collect(Collectors.joining("; "));
            return Collections.singletonList(createTestError(Severity.WARNING, message, key, value, prettifiedValue, p));
        }

        if (!includeOtherSeverityChecks() || Objects.equals(value, prettifiedValue)) {
            return Collections.emptyList();
        } else {
            final String message = tr("{0} value can be prettified", key);
            return Collections.singletonList(createTestError(Severity.OTHER, message, key, value, prettifiedValue, p));
        }
    }

    @Override
    public void check(final OsmPrimitive p) {
        addErrorsForPrimitive(p, this.errors);
    }

    /**
     * Checks the tags of the given primitive and adds validation errors to the given list.
     * @param p The primitive to test
     * @param errors The list to add validation errors to
     * @since 17643
     */
    public void addErrorsForPrimitive(OsmPrimitive p, Collection<TestError> errors) {
        if (p.isTagged()) {
            for (String key : KEYS_TO_CHECK) {
                errors.addAll(checkOpeningHourSyntax(key, p.get(key), p, Locale.getDefault()));
            }
            // COVID-19, a few additional values are permitted, see #19048, see https://wiki.openstreetmap.org/wiki/Key:opening_hours:covid19
            final String keyCovid19 = "opening_hours:covid19";
            if (p.hasTag(keyCovid19) && !p.hasTag(keyCovid19, "same", "restricted", "open", "off")) {
                errors.addAll(checkOpeningHourSyntax(keyCovid19, p.get(keyCovid19), p, Locale.getDefault()));
            }
        }
    }

    @Override
    public void addGui(JPanel testPanel) {
        super.addGui(testPanel);
        checkboxStrictMode.setSelected(PREF_STRICT_MODE.get());
        testPanel.add(checkboxStrictMode, GBC.eol().insets(20, 0, 0, 0));
    }

    @Override
    public boolean ok() {
        super.ok();
        PREF_STRICT_MODE.put(checkboxStrictMode.isSelected());
        return false;
    }
}
