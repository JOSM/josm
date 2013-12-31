package org.openstreetmap.josm.tools;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

import java.util.regex.Pattern;

/**
 * Utility class for creating {@link Predicate}s.
 */
public final class Predicates {

    private Predicates() {
    }

    /**
     * Returns a {@link Predicate} executing {@link Pattern#matcher(CharSequence)} and {@link java.util.regex.Matcher#matches}.
     */
    public static Predicate<String> stringMatchesPattern(final Pattern pattern) {
        return new Predicate<String>() {
            @Override
            public boolean evaluate(String string) {
                return pattern.matcher(string).matches();
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link Pattern#matcher(CharSequence)} and {@link java.util.regex.Matcher#find}.
     */
    public static Predicate<String> stringContainsPattern(final Pattern pattern) {
        return new Predicate<String>() {
            @Override
            public boolean evaluate(String string) {
                return pattern.matcher(string).find();
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link String#contains(CharSequence)}.
     */
    public static Predicate<String> stringContains(final String pattern) {
        return new Predicate<String>() {
            @Override
            public boolean evaluate(String string) {
                return string.contains(pattern);
            }
        };
    }

    /**
     * Returns a {@link Predicate} executing {@link OsmPrimitive#hasTag(String, String...)}.
     */
    public static Predicate<OsmPrimitive> hasTag(final String key, final String... values) {
        return new Predicate<OsmPrimitive>() {
            @Override
            public boolean evaluate(OsmPrimitive p) {
                return p.hasTag(key, values);
            }
        };
    }
}
