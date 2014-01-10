// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.FixableTestError;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tests the correct usage of the opening hour syntax of the tags
 * {@code opening_hours}, {@code collection_times}, {@code service_times} according to
 * <a href="https://github.com/ypid/opening_hours.js">opening_hours.js</a>.
 *
 * @since 6370
 */
public class OpeningHourTest extends Test.TagTest {

    /**
     * Javascript engine
     */
    public static final ScriptEngine ENGINE = new ScriptEngineManager().getEngineByName("JavaScript");

    /**
     * Constructs a new {@code OpeningHourTest}.
     */
    public OpeningHourTest() {
        super(tr("Opening hours syntax"),
                tr("This test checks the correct usage of the opening hours syntax."));
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        if (ENGINE != null) {
            ENGINE.eval(new InputStreamReader(new MirroredInputStream("resource://data/validator/opening_hours.js"), Utils.UTF_8));
            // fake country/state to not get errors on holidays
            ENGINE.eval("var nominatimJSON = {address: {state: 'Bayern', country_code: 'de'}};");
            ENGINE.eval("" +
                    "var oh = function (value, mode) {" +
                    " try {" +
                    "    var r= new opening_hours(value, nominatimJSON, mode);" +
                    "    r.getErrors = function() {return [];};" +
                    "    return r;" +
                    "  } catch(err) {" +
                    "    return {" +
                    "      getWarnings: function() {return [];}," +
                    "      getErrors: function() {return [err.toString()]}" +
                    "    };" +
                    "  }" +
                    "};");
        } else {
            Main.warn("Unable to initialize OpeningHourTest because no JavaScript engine has been found");
        }
    }

    static enum CheckMode {
        TIME_RANGE(0), POINTS_IN_TIME(1), BOTH(2);
        final int code;

        CheckMode(int code) {
            this.code = code;
        }
    }

    protected Object parse(String value, CheckMode mode) throws ScriptException, NoSuchMethodException {
        return ((Invocable) ENGINE).invokeFunction("oh", value, mode.code);
    }

    @SuppressWarnings("unchecked")
    protected List<Object> getList(Object obj) throws ScriptException, NoSuchMethodException {
        if (obj == null || "".equals(obj)) {
            return Arrays.asList();
        } else if (obj instanceof String) {
            final Object[] strings = ((String) obj).split("\\\\n");
            return Arrays.asList(strings);
        } else if (obj instanceof List) {
            return (List<Object>) obj;
        } else {
            // recursively call getList() with argument converted to newline-separated string
            return getList(((Invocable) ENGINE).invokeMethod(obj, "join", "\\n"));
        }
    }

    /**
     * An error concerning invalid syntax for an "opening_hours"-like tag.
     */
    public class OpeningHoursTestError {
        final Severity severity;
        final String message, prettifiedValue;

        /**
         * Constructs a new {@code OpeningHoursTestError} with a known pretiffied value.
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
            if (prettifiedValue == null) {
                return new TestError(OpeningHourTest.this, severity, message, 2901, p);
            } else {
                return new FixableTestError(OpeningHourTest.this, severity, message, 2901, p,
                        new ChangePropertyCommand(p, key, prettifiedValue));
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
     * Checks for a correct usage of the opening hour syntax of the {@code value} given according to
     * <a href="https://github.com/ypid/opening_hours.js">opening_hours.js</a> and returns a list containing
     * validation errors or an empty list. Null values result in an empty list.
     * @param key the OSM key (should be "opening_hours", "collection_times" or "service_times"). Used in error message
     * @param value the opening hour value to be checked.
     * @param mode whether to validate {@code value} as a time range, or points in time, or both.
     * @return a list of {@link TestError} or an empty list
     */
    public List<OpeningHoursTestError> checkOpeningHourSyntax(final String key, final String value, CheckMode mode) {
        return checkOpeningHourSyntax(key, value, mode, false);
    }

    /**
     * Checks for a correct usage of the opening hour syntax of the {@code value} given according to
     * <a href="https://github.com/ypid/opening_hours.js">opening_hours.js</a> and returns a list containing
     * validation errors or an empty list. Null values result in an empty list.
     * @param key the OSM key (should be "opening_hours", "collection_times" or "service_times").
     * @param value the opening hour value to be checked.
     * @param mode whether to validate {@code value} as a time range, or points in time, or both.
     * @param ignoreOtherSeverity whether to ignore errors with {@link Severity#OTHER}.
     * @return a list of {@link TestError} or an empty list
     */
    public List<OpeningHoursTestError> checkOpeningHourSyntax(final String key, final String value, CheckMode mode, boolean ignoreOtherSeverity) {
        if (ENGINE == null || value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        final List<OpeningHoursTestError> errors = new ArrayList<OpeningHoursTestError>();
        try {
            final Object r = parse(value, mode);
            String prettifiedValue = null;
            try {
                prettifiedValue = (String) ((Invocable) ENGINE).invokeMethod(r, "prettifyValue");
            } catch (Exception e) {
                Main.debug(e.getMessage());
            }
            for (final Object i : getList(((Invocable) ENGINE).invokeMethod(r, "getErrors"))) {
                errors.add(new OpeningHoursTestError(key + " - " + i.toString().trim(), Severity.ERROR, prettifiedValue));
            }
            for (final Object i : getList(((Invocable) ENGINE).invokeMethod(r, "getWarnings"))) {
                errors.add(new OpeningHoursTestError(i.toString().trim(), Severity.WARNING, prettifiedValue));
            }
            if (!ignoreOtherSeverity && errors.isEmpty() && prettifiedValue != null && !value.equals(prettifiedValue)) {
                errors.add(new OpeningHoursTestError(tr("opening_hours value can be prettified"), Severity.OTHER, prettifiedValue));
            }
        } catch (ScriptException ex) {
            Main.error(ex);
        } catch (NoSuchMethodException ex) {
            Main.error(ex);
        }
        return errors;
    }

    /**
     * Checks for a correct usage of the opening hour syntax of the {@code value} given, in time range mode, according to
     * <a href="https://github.com/ypid/opening_hours.js">opening_hours.js</a> and returns a list containing
     * validation errors or an empty list. Null values result in an empty list.
     * @param key the OSM key (should be "opening_hours", "collection_times" or "service_times"). Used in error message
     * @param value the opening hour value to be checked.
     * @return a list of {@link TestError} or an empty list
     */
    public List<OpeningHoursTestError> checkOpeningHourSyntax(final String key, final String value) {
        return checkOpeningHourSyntax(key, value, "opening_hours".equals(key) ? CheckMode.TIME_RANGE : CheckMode.BOTH);
    }

    protected void check(final OsmPrimitive p, final String key, CheckMode mode) {
        for (OpeningHoursTestError e : checkOpeningHourSyntax(key, p.get(key), mode)) {
            errors.add(e.getTestError(p, key));
        }
    }

    @Override
    public void check(final OsmPrimitive p) {
        check(p, "opening_hours", CheckMode.TIME_RANGE);
        check(p, "collection_times", CheckMode.BOTH);
        check(p, "service_times", CheckMode.BOTH);
    }
}
