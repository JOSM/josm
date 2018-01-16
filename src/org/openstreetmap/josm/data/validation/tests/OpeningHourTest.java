// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
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
    public static final ScriptEngine ENGINE = Utils.getJavaScriptEngine();

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
            try (CachedFile cf = new CachedFile("resource://data/validator/opening_hours.js");
                 Reader reader = cf.getContentReader()) {
                ENGINE.eval(reader);
                ENGINE.eval("var opening_hours = require('opening_hours');");
                // fake country/state to not get errors on holidays
                ENGINE.eval("var nominatimJSON = {address: {state: 'Bayern', country_code: 'de'}};");
                ENGINE.eval(
                        "var oh = function (value, tag_key, mode, locale) {" +
                        " try {" +
                        "    var conf = {tag_key: tag_key, locale: locale};" +
                        "    if (mode > -1) {" +
                        "      conf.mode = mode;" +
                        "    }" +
                        "    var r = new opening_hours(value, nominatimJSON, conf);" +
                        "    r.getErrors = function() {return [];};" +
                        "    return r;" +
                        "  } catch (err) {" +
                        "    return {" +
                        "      prettifyValue: function() {return null;}," +
                        "      getWarnings: function() {return [];}," +
                        "      getErrors: function() {return [err.toString()]}" +
                        "    };" +
                        "  }" +
                        "};");
            }
        } else {
            Logging.warn("Unable to initialize OpeningHourTest because no JavaScript engine has been found");
        }
    }

    /**
     * In OSM, the syntax originally designed to describe opening hours, is now used to describe a few other things as well.
     * Some of those other tags work with points in time instead of time ranges.
     * To support this the mode can be specified.
     * @since 13147
     */
    public enum CheckMode {
        /** time ranges (opening_hours, lit, …) default */
        TIME_RANGE(0),
        /** points in time */
        POINTS_IN_TIME(1),
        /** both (time ranges and points in time, used by collection_times, service_times, …) */
        BOTH(2);
        private final int code;

        CheckMode(int code) {
            this.code = code;
        }
    }

    /**
     * Parses the opening hour syntax of the {@code value} given according to
     * <a href="https://github.com/ypid/opening_hours.js">opening_hours.js</a> and returns an object on which
     * methods can be called to extract information.
     * @param value the opening hour value to be checked
     * @param tagKey the OSM key (should be "opening_hours", "collection_times" or "service_times")
     * @param mode whether to validate {@code value} as a time range, or points in time, or both. Can be null
     * @param locale the locale code used for localizing messages
     * @return The value returned by the underlying method. Usually a {@code jdk.nashorn.api.scripting.ScriptObjectMirror}
     * @throws ScriptException if an error occurs during invocation of the underlying method
     * @throws NoSuchMethodException if underlying method with given name or matching argument types cannot be found
     * @since 13147
     */
    public Object parse(String value, String tagKey, CheckMode mode, String locale) throws ScriptException, NoSuchMethodException {
        return ((Invocable) ENGINE).invokeFunction("oh", value, tagKey, mode != null ? mode.code : -1, locale);
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
        private final Severity severity;
        private final String message;
        private final String prettifiedValue;

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
     * Checks for a correct usage of the opening hour syntax of the {@code value} given according to
     * <a href="https://github.com/ypid/opening_hours.js">opening_hours.js</a> and returns a list containing
     * validation errors or an empty list. Null values result in an empty list.
     * @param key the OSM key (should be "opening_hours", "collection_times" or "service_times"). Used in error message
     * @param value the opening hour value to be checked.
     * @return a list of {@link TestError} or an empty list
     */
    public List<OpeningHoursTestError> checkOpeningHourSyntax(final String key, final String value) {
        return checkOpeningHourSyntax(key, value, null, false, LanguageInfo.getJOSMLocaleCode());
    }

    /**
     * Checks for a correct usage of the opening hour syntax of the {@code value} given according to
     * <a href="https://github.com/ypid/opening_hours.js">opening_hours.js</a> and returns a list containing
     * validation errors or an empty list. Null values result in an empty list.
     * @param key the OSM key (should be "opening_hours", "collection_times" or "service_times").
     * @param value the opening hour value to be checked.
     * @param mode whether to validate {@code value} as a time range, or points in time, or both. Can be null
     * @param ignoreOtherSeverity whether to ignore errors with {@link Severity#OTHER}.
     * @param locale the locale code used for localizing messages
     * @return a list of {@link TestError} or an empty list
     */
    public List<OpeningHoursTestError> checkOpeningHourSyntax(final String key, final String value, CheckMode mode,
            boolean ignoreOtherSeverity, String locale) {
        if (ENGINE == null || value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        final List<OpeningHoursTestError> errors = new ArrayList<>();
        try {
            final Object r = parse(value, key, mode, locale);
            String prettifiedValue = null;
            try {
                prettifiedValue = getOpeningHoursPrettifiedValues(r);
            } catch (ScriptException | NoSuchMethodException e) {
                Logging.warn(e);
            }
            for (final Object i : getOpeningHoursErrors(r)) {
                errors.add(new OpeningHoursTestError(getErrorMessage(key, i), Severity.ERROR, prettifiedValue));
            }
            for (final Object i : getOpeningHoursWarnings(r)) {
                errors.add(new OpeningHoursTestError(getErrorMessage(key, i), Severity.WARNING, prettifiedValue));
            }
            if (!ignoreOtherSeverity && errors.isEmpty() && prettifiedValue != null && !value.equals(prettifiedValue)) {
                errors.add(new OpeningHoursTestError(tr("opening_hours value can be prettified"), Severity.OTHER, prettifiedValue));
            }
        } catch (ScriptException | NoSuchMethodException ex) {
            Logging.error(ex);
        }
        return errors;
    }

    /**
     * Returns the prettified value returned by the opening hours parser.
     * @param r result of {@link #parse}
     * @return the prettified value returned by the opening hours parser
     * @throws NoSuchMethodException if method "prettifyValue" or matching argument types cannot be found
     * @throws ScriptException if an error occurs during invocation of the JavaScript method
     * @since 13296
     */
    public final String getOpeningHoursPrettifiedValues(Object r) throws NoSuchMethodException, ScriptException {
        return (String) ((Invocable) ENGINE).invokeMethod(r, "prettifyValue");
    }

    /**
     * Returns the list of errors returned by the opening hours parser.
     * @param r result of {@link #parse}
     * @return the list of errors returned by the opening hours parser
     * @throws NoSuchMethodException if method "getErrors" or matching argument types cannot be found
     * @throws ScriptException if an error occurs during invocation of the JavaScript method
     * @since 13296
     */
    public final List<Object> getOpeningHoursErrors(Object r) throws NoSuchMethodException, ScriptException {
        return getList(((Invocable) ENGINE).invokeMethod(r, "getErrors"));
    }

    /**
     * Returns the list of warnings returned by the opening hours parser.
     * @param r result of {@link #parse}
     * @return the list of warnings returned by the opening hours parser
     * @throws NoSuchMethodException if method "getWarnings" or matching argument types cannot be found
     * @throws ScriptException if an error occurs during invocation of the JavaScript method
     * @since 13296
     */
    public final List<Object> getOpeningHoursWarnings(Object r) throws NoSuchMethodException, ScriptException {
        return getList(((Invocable) ENGINE).invokeMethod(r, "getWarnings"));
    }

    /**
     * Translates and shortens the error/warning message.
     * @param o error/warning message returned by {@link #getOpeningHoursErrors} or {@link #getOpeningHoursWarnings}
     * @return translated/shortened error/warning message
     * @since 13298
     */
    public static String getErrorMessage(Object o) {
        return o.toString().trim()
        .replace("Unexpected token:", tr("Unexpected token:"))
        .replace("Unexpected token (school holiday parser):", tr("Unexpected token (school holiday parser):"))
        .replace("Unexpected token in number range:", tr("Unexpected token in number range:"))
        .replace("Unexpected token in week range:", tr("Unexpected token in week range:"))
        .replace("Unexpected token in weekday range:", tr("Unexpected token in weekday range:"))
        .replace("Unexpected token in month range:", tr("Unexpected token in month range:"))
        .replace("Unexpected token in year range:", tr("Unexpected token in year range:"))
        .replace("This means that the syntax is not valid at that point or it is currently not supported.", tr("Invalid/unsupported syntax."));
    }

    /**
     * Translates and shortens the error/warning message.
     * @param key OSM key
     * @param o error/warning message returned by {@link #getOpeningHoursErrors} or {@link #getOpeningHoursWarnings}
     * @return translated/shortened error/warning message
     */
    static String getErrorMessage(String key, Object o) {
        return key + " - " + getErrorMessage(o);
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
