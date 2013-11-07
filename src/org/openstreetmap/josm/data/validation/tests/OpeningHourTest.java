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
 * @author frsantos
 */
public class OpeningHourTest extends Test {

    /**
     * Javascript Rhino engine
     */
    public static final ScriptEngine ENGINE = new ScriptEngineManager().getEngineByName("rhino");

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
        ENGINE.eval(new InputStreamReader(new MirroredInputStream("resource://data/opening_hours.js"), "UTF-8"));
        ENGINE.eval("var oh = function (x, y) {return new opening_hours(x, y);};");
    }

    protected Object parse(String value) throws ScriptException, NoSuchMethodException {
        return ((Invocable) ENGINE).invokeFunction("oh", value);
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
    public List<TestError> checkOpeningHourSyntax(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            final Object r = parse(value);
            final List<TestError> errors = new ArrayList<TestError>();
            for (final Object i : getList(((Invocable) ENGINE).invokeMethod(r, "getWarnings"))) {
                errors.add(new TestError(this, Severity.WARNING, i.toString(), 2901, Collections.<OsmPrimitive>emptyList()));
            }
            return errors;
        } catch (ScriptException ex) {
            final String message = ex.getMessage()
                    .replace("sun.org.mozilla.javascript.JavaScriptException: ", "")
                    .replaceAll("\\(<Unknown source.*", "")
                    .trim();
            return Arrays.asList(new TestError(this, Severity.ERROR, message, 2901, Collections.<OsmPrimitive>emptyList()));
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected void check(final OsmPrimitive p, final String tagValue) {
        for (TestError e : checkOpeningHourSyntax(tagValue)) {
            e.setPrimitives(Collections.singletonList(p));
            errors.add(e);
        }
    }

    protected void check(final OsmPrimitive p) {
        check(p, p.get("opening_hours"));
        // unsupported, cf. https://github.com/AMDmi3/opening_hours.js/issues/12
        //check(p, p.get("collection_times"));
        //check(p, p.get("service_times"));
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
