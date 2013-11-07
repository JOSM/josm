// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
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
                tr("This plugin checks for correct usage of opening hours syntax."));
    }

    @Override
    public void initialize() throws Exception {
        super.initialize();
        if (ENGINE != null) {
            ENGINE.eval(new InputStreamReader(new MirroredInputStream("resource://data/opening_hours.js"), "UTF-8"));
            // fake country/state to not get errors on holidays
            ENGINE.eval("var nominatiomJSON = {address: {state: 'Bayern', country_code: 'de'}};");
            ENGINE.eval("var oh = function (value, mode) {return new opening_hours(value, nominatiomJSON, mode);};");
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
    protected List<Object> getList(Object obj) {
        if (obj == null || "".equals(obj)) {
            return Arrays.asList();
        } else if (obj instanceof String) {
            final Object[] strings = ((String) obj).split("\\n");
            return Arrays.asList(strings);
        } else if (obj instanceof List) {
            return (List<Object>) obj;
        } else if ("sun.org.mozilla.javascript.internal.NativeArray".equals(obj.getClass().getName())) {
            List<Object> list = new ArrayList<Object>();
            try {
                Method getIds = obj.getClass().getMethod("getIds");
                Method get = obj.getClass().getMethod("get", long.class);
                Object[] ids = (Object[]) getIds.invoke(obj);
                for (Object id : ids) {
                    list.add(get.invoke(obj, id));
                }
            } catch (NoSuchMethodException e) {
                Main.error("Unable to run OpeningHourTest because of NoSuchMethodException by reflection: "+e.getMessage());
            } catch (IllegalArgumentException e) {
                Main.error("Unable to run OpeningHourTest because of IllegalArgumentException by reflection: "+e.getMessage());
            } catch (IllegalAccessException e) {
                Main.error("Unable to run OpeningHourTest because of IllegalAccessException by reflection: "+e.getMessage());
            } catch (InvocationTargetException e) {
                Main.error("Unable to run OpeningHourTest because of InvocationTargetException by reflection: "+e.getMessage());
            }
            return list;
        } else {
            throw new IllegalArgumentException("Not expecting class " + obj.getClass());
        }
    }

    /**
     * Checks for a correct usage of the opening hour syntax of the {@code value} given according to
     * <a href="https://github.com/ypid/opening_hours.js">opening_hours.js</a> and returns a list containing
     * validation errors or an empty list. Null values result in an empty list.
     * @param value the opening hour value to be checked.
     * @return a list of {@link TestError} or an empty list
     */
    public List<TestError> checkOpeningHourSyntax(final String value, CheckMode mode) {
        if (ENGINE == null || value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            final Object r = parse(value, mode);
            final List<TestError> errors = new ArrayList<TestError>();
            for (final Object i : getList(((Invocable) ENGINE).invokeMethod(r, "getWarnings"))) {
                errors.add(new TestError(this, Severity.WARNING, i.toString(), 2901, Collections.<OsmPrimitive>emptyList()));
            }
            return errors;
        } catch (ScriptException ex) {
            final String message = ex.getMessage()
                    .replaceAll("[^:]*Exception: ", "opening_hours - ")
                    .replaceAll("\\(<Unknown source.*", "")
                    .trim();
            return Arrays.asList(new TestError(this, Severity.ERROR, message, 2901, Collections.<OsmPrimitive>emptyList()));
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<TestError> checkOpeningHourSyntax(final String value) {
        return checkOpeningHourSyntax(value, CheckMode.TIME_RANGE);
    }

    protected void check(final OsmPrimitive p, final String tagValue, CheckMode mode) {
        for (TestError e : checkOpeningHourSyntax(tagValue, mode)) {
            e.setPrimitives(Collections.singletonList(p));
            errors.add(e);
        }
    }

    protected void check(final OsmPrimitive p) {
        check(p, p.get("opening_hours"), CheckMode.TIME_RANGE);
        check(p, p.get("collection_times"), CheckMode.BOTH);
        check(p, p.get("service_times"), CheckMode.BOTH);
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
