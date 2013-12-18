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
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.FixableTestError;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.io.MirroredInputStream;

/**
 * Tests the correct usage of the opening hour syntax of the tags
 * {@code opening_hours}, {@code collection_times}, {@code service_times} according to
 * <a href="https://github.com/ypid/opening_hours.js">opening_hours.js</a>.
 *
 * @since 6370
 */
public class OpeningHourTest extends Test {

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
            ENGINE.eval(new InputStreamReader(new MirroredInputStream("resource://data/validator/opening_hours.js"), "UTF-8"));
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

    public class OpeningHoursTestError {
        final Severity severity;
        final String message, prettifiedValue;

        public OpeningHoursTestError(String message, Severity severity, String prettifiedValue) {
            this.message = message;
            this.severity = severity;
            this.prettifiedValue = prettifiedValue;
        }

        public OpeningHoursTestError(String message, Severity severity) {
            this(message, severity, null);
        }

        public TestError getTestError(final OsmPrimitive p, final String key) {
            if (prettifiedValue == null) {
                return new TestError(OpeningHourTest.this, severity, message, 2901, p);
            } else {
                return new FixableTestError(OpeningHourTest.this, severity, message, 2901, p,
                        new ChangePropertyCommand(p, key, prettifiedValue));
            }
        }

        public String getMessage() {
            return message;
        }

        public String getPrettifiedValue() {
            return prettifiedValue;
        }

        public Severity getSeverity() {
            return severity;
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
        if (ENGINE == null || value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            final Object r = parse(value, mode);
            final List<OpeningHoursTestError> errors = new ArrayList<OpeningHoursTestError>();
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
            return errors;
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<OpeningHoursTestError> checkOpeningHourSyntax(final String key, final String value) {
        return checkOpeningHourSyntax(key, value, CheckMode.TIME_RANGE);
    }

    protected void check(final OsmPrimitive p, final String key, CheckMode mode) {
        for (OpeningHoursTestError e : checkOpeningHourSyntax(key, p.get(key), mode)) {
            errors.add(e.getTestError(p, key));
        }
    }

    protected void check(final OsmPrimitive p) {
        check(p, "opening_hours", CheckMode.TIME_RANGE);
        check(p, "collection_times", CheckMode.BOTH);
        check(p, "service_times", CheckMode.BOTH);
    }

    @Override
    public void visit(final Node n) {
        check(n);
    }

    @Override
    public void visit(final Relation r) {
        check(r);
    }

    @Override
    public void visit(final Way w) {
        check(w);
    }
}
