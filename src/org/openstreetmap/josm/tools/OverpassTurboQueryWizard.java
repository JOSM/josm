// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.openstreetmap.josm.Main;

/**
 * Uses <a href="https://github.com/tyrasd/overpass-wizard/">Overpass Turbo query wizard</a> code (MIT Licensed)
 * to build an Overpass QL from a {@link org.openstreetmap.josm.actions.search.SearchAction} like query.
 *
 * Requires a JavaScript {@link ScriptEngine}.
 * @since 8744
 */
public final class OverpassTurboQueryWizard {

    private static OverpassTurboQueryWizard instance;
    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    /**
     * Replies the unique instance of this class.
     *
     * @return the unique instance of this class
     */
    public static synchronized OverpassTurboQueryWizard getInstance() {
        if (instance == null) {
            instance = new OverpassTurboQueryWizard();
        }
        return instance;
    }

    private OverpassTurboQueryWizard() {
        try (final Reader reader = new InputStreamReader(
                getClass().getResourceAsStream("/data/overpass-wizard.js"), StandardCharsets.UTF_8)) {
            engine.eval("var console = {error: " + Main.class.getCanonicalName() + ".warn};");
            engine.eval("var global = {};");
            engine.eval(reader);
        } catch (ScriptException | IOException ex) {
            throw new RuntimeException("Failed to initialize OverpassTurboQueryWizard", ex);
        }
    }

    /**
     * Builds an Overpass QL from a {@link org.openstreetmap.josm.actions.search.SearchAction} like query.
     * @param search the {@link org.openstreetmap.josm.actions.search.SearchAction} like query
     * @return an Overpass QL query
     * @throws UncheckedParseException when the parsing fails
     */
    public String constructQuery(String search) throws UncheckedParseException {
        try {
            final Object result = ((Invocable) engine).invokeMethod(engine.get("global"),
                    "overpassWizard", search, new HashMap<String, Object>() { {
                        put("comment", false);
                        put("outputFormat", "xml");
                        put("outputMode", "recursive_meta");
                    } }
            );
            if (Boolean.FALSE.equals(result)) {
                throw new UncheckedParseException();
            }
            String query = (String) result;
            query = query.replace("[bbox:{{bbox}}]", "");
            return query;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException();
        } catch (ScriptException e) {
            throw new RuntimeException("Failed to execute OverpassTurboQueryWizard", e);
        }
    }

}
