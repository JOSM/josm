// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Uses <a href="https://github.com/tyrasd/overpass-turbo/">Overpass Turbo</a> query wizard code
 * to build an Overpass QL from a {@link org.openstreetmap.josm.actions.search.SearchAction} like query.
 *
 * Requires a JavaScript {@link ScriptEngine}.
 * @since 8744
 */
public final class OverpassTurboQueryWizard {

    private static OverpassTurboQueryWizard instance;
    private final ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

    /**
     * An exception to indicate a failed parse.
     */
    public static class ParseException extends RuntimeException {
    }

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
        // overpass-turbo is MIT Licensed

        try (final Reader reader = new InputStreamReader(
                getClass().getResourceAsStream("/data/overpass-turbo-ffs.js"), StandardCharsets.UTF_8)) {
            //engine.eval("var turbo = {ffs: {noPresets: true}};");
            engine.eval("var console = {log: function(){}};");
            engine.eval(reader);
            engine.eval("var construct_query = turbo.ffs().construct_query;");
        } catch (ScriptException | IOException ex) {
            throw new RuntimeException("Failed to initialize OverpassTurboQueryWizard", ex);
        }
    }

    /**
     * Builds an Overpass QL from a {@link org.openstreetmap.josm.actions.search.SearchAction} like query.
     * @param search the {@link org.openstreetmap.josm.actions.search.SearchAction} like query
     * @return an Overpass QL query
     * @throws ParseException when the parsing fails
     */
    public String constructQuery(String search) throws ParseException {
        try {
            final Object result = ((Invocable) engine).invokeFunction("construct_query", search);
            if (result == Boolean.FALSE) {
                throw new ParseException();
            }
            String query = (String) result;
            query = Pattern.compile("^.*\\[out:json\\]", Pattern.DOTALL).matcher(query).replaceFirst("");
            query = Pattern.compile("^out.*", Pattern.MULTILINE).matcher(query).replaceAll("out meta;");
            query = query.replace("({{bbox}})", "");
            return query;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException();
        } catch (ScriptException e) {
            throw new RuntimeException("Failed to execute OverpassTurboQueryWizard", e);
        }
    }

}
