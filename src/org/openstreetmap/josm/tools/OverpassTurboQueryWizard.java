// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.IOException;
import java.io.Reader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.openstreetmap.josm.io.CachedFile;

/**
 * Uses <a href="https://github.com/tyrasd/overpass-wizard/">Overpass Turbo query wizard</a> code (MIT Licensed)
 * to build an Overpass QL from a {@link org.openstreetmap.josm.actions.search.SearchAction} like query.
 *
 * Requires a JavaScript {@link ScriptEngine}.
 * @since 8744
 */
public final class OverpassTurboQueryWizard {

    private static OverpassTurboQueryWizard instance;
    private final ScriptEngine engine = new ScriptEngineManager(null).getEngineByName("JavaScript");

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
        if (engine == null) {
            throw new IllegalStateException("Failed to retrieve JavaScript engine");
        }
        try (CachedFile file = new CachedFile("resource://data/overpass-wizard.js");
             Reader reader = file.getContentReader()) {
            engine.eval("var console = {error: " + Logging.class.getCanonicalName() + ".warn};");
            engine.eval("var global = {};");
            engine.eval(reader);
            engine.eval("var overpassWizard = function(query) {" +
                    "  return global.overpassWizard(query, {" +
                    "    comment: false," +
                    "    outputFormat: 'xml'," +
                    "    outputMode: 'recursive_meta'" +
                    "  });" +
                    "}");
        } catch (ScriptException | IOException ex) {
            throw new IllegalStateException("Failed to initialize OverpassTurboQueryWizard", ex);
        }
    }

    /**
     * Builds an Overpass QL from a {@link org.openstreetmap.josm.actions.search.SearchAction} like query.
     * @param search the {@link org.openstreetmap.josm.actions.search.SearchAction} like query
     * @return an Overpass QL query
     * @throws UncheckedParseException when the parsing fails
     */
    public String constructQuery(String search) {
        try {
            final Object result = ((Invocable) engine).invokeFunction("overpassWizard", search);
            if (Boolean.FALSE.equals(result)) {
                throw new UncheckedParseException();
            }
            return (String) result;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        } catch (ScriptException e) {
            throw new UncheckedParseException("Failed to execute OverpassTurboQueryWizard", e);
        }
    }
}
