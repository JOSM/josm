// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.osm.search.SearchParseError;

/**
 * Builds an Overpass QL from a {@link org.openstreetmap.josm.actions.search.SearchAction} query.
 *
 * @since 8744 (using tyrasd/overpass-wizard), 16262 (standalone)
 */
public final class OverpassTurboQueryWizard {

    private static final OverpassTurboQueryWizard instance = new OverpassTurboQueryWizard();

    /**
     * Replies the unique instance of this class.
     *
     * @return the unique instance of this class
     */
    public static OverpassTurboQueryWizard getInstance() {
        return instance;
    }

    private OverpassTurboQueryWizard() {
        // private constructor for utility class
    }

    /**
     * Builds an Overpass QL from a {@link org.openstreetmap.josm.actions.search.SearchAction} like query.
     * @param search the {@link org.openstreetmap.josm.actions.search.SearchAction} like query
     * @return an Overpass QL query
     * @throws UncheckedParseException when the parsing fails
     */
    public String constructQuery(final String search) {
        try {
            Matcher matcher = Pattern.compile("\\s+GLOBAL\\s*$", Pattern.CASE_INSENSITIVE).matcher(search);
            if (matcher.find()) {
                final Match match = SearchCompiler.compile(matcher.replaceFirst(""));
                return constructQuery(match, ";", "");
            }

            matcher = Pattern.compile("\\s+IN BBOX\\s*$", Pattern.CASE_INSENSITIVE).matcher(search);
            if (matcher.find()) {
                final Match match = SearchCompiler.compile(matcher.replaceFirst(""));
                return constructQuery(match, "[bbox:{{bbox}}];", "");
            }

            matcher = Pattern.compile("\\s+(?<mode>IN|AROUND)\\s+(?<area>[^\" ]+|\"[^\"]+\")\\s*$", Pattern.CASE_INSENSITIVE).matcher(search);
            if (matcher.find()) {
                final Match match = SearchCompiler.compile(matcher.replaceFirst(""));
                final String mode = matcher.group("mode").toUpperCase(Locale.ENGLISH);
                final String area = Utils.strip(matcher.group("area"), "\"");
                if ("IN".equals(mode)) {
                    return constructQuery(match, ";\n{{geocodeArea:" + area + "}}->.searchArea;", "(area.searchArea)");
                } else if ("AROUND".equals(mode)) {
                    return constructQuery(match, ";\n{{radius=1000}}", "(around:{{radius}},{{geocodeCoords:" + area + "}})");
                } else {
                    throw new IllegalStateException(mode);
                }
            }
            
            final Match match = SearchCompiler.compile(search);
            return constructQuery(match, "[bbox:{{bbox}}];", "");
        } catch (SearchParseError | UnsupportedOperationException e) {
            throw new UncheckedParseException(e);
        }
    }

    private String constructQuery(final Match match, final String bounds, final String queryLineSuffix) {
        final List<Match> normalized = normalizeToDNF(match);
        final List<String> queryLines = new ArrayList<>();
        queryLines.add("[out:xml][timeout:90]" + bounds);
        queryLines.add("(");
        for (Match conjunction : normalized) {
            final EnumSet<OsmPrimitiveType> types = EnumSet.noneOf(OsmPrimitiveType.class);
            final String query = constructQuery(conjunction, types);
            (types.isEmpty() || types.size() == 3
                    ? Stream.of("nwr")
                    : types.stream().map(OsmPrimitiveType::getAPIName))
                    .forEach(type -> queryLines.add("  " + type + query + queryLineSuffix + ";"));
        }
        queryLines.add(");");
        queryLines.add("(._;>;);");
        queryLines.add("out meta;");
        return String.join("\n", queryLines);
    }

    private static String constructQuery(Match match, final Set<OsmPrimitiveType> types) {
        final boolean negated;
        if (match instanceof SearchCompiler.Not) {
            negated = true;
            match = ((SearchCompiler.Not) match).getMatch();
        } else {
            negated = false;
        }
        if (match instanceof SearchCompiler.And) {
            return ((SearchCompiler.And) match).map(m -> constructQuery(m, types), (s1, s2) -> s1 + s2);
        } else if (match instanceof SearchCompiler.KeyValue) {
            final String key = ((SearchCompiler.KeyValue) match).getKey();
            final String value = ((SearchCompiler.KeyValue) match).getValue();
            if ("newer".equals(key)) {
                return "(newer:" + quote("{{date:" + value + "}}") + ")";
            }
            return "[~" + quote(key) + "~" + quote(value) + "]";
        } else if (match instanceof SearchCompiler.ExactKeyValue) {
            // https://wiki.openstreetmap.org/wiki/Overpass_API/Language_Guide
            // ["key"]             -- filter objects tagged with this key and any value
            // [!"key"]            -- filter objects not tagged with this key and any value
            // ["key"="value"]     -- filter objects tagged with this key and this value
            // ["key"!="value"]    -- filter objects tagged with this key but not this value, or not tagged with this key
            // ["key"~"value"]     -- filter objects tagged with this key and a value matching a regular expression
            // ["key"!~"value"]    -- filter objects tagged with this key but a value not matching a regular expression
            // [~"key"~"value"]    -- filter objects tagged with a key and a value matching regular expressions
            // [~"key"~"value", i] -- filter objects tagged with a key and a case-insensitive value matching regular expressions
            final String key = ((SearchCompiler.ExactKeyValue) match).getKey();
            final String value = ((SearchCompiler.ExactKeyValue) match).getValue();
            final SearchCompiler.ExactKeyValue.Mode mode = ((SearchCompiler.ExactKeyValue) match).getMode();
            switch (mode) {
                case ANY_VALUE:
                    return "[" + (negated ? "!" : "") + quote(key) + "]";
                case EXACT:
                    return "[" + quote(key) + (negated ? "!=" : "=") + quote(value) + "]";
                case EXACT_REGEXP:
                    final Matcher matcher = Pattern.compile("/(?<regex>.*)/(?<flags>i)?").matcher(value);
                    final String valueQuery = matcher.matches()
                            ? quote(matcher.group("regex")) + Optional.ofNullable(matcher.group("flags")).map(f -> "," + f).orElse("")
                            : quote(value);
                    return "[" + quote(key) + (negated ? "!~" : "~") + valueQuery + "]";
                case MISSING_KEY:
                    // special case for empty values, see https://github.com/drolbr/Overpass-API/issues/53
                    return "[" + quote(key) + (negated ? "!~" : "~") + quote("^$") + "]";
                default:
                    return "";
            }
        } else if (match instanceof SearchCompiler.BooleanMatch) {
            final String key = ((SearchCompiler.BooleanMatch) match).getKey();
            return negated
                    ? "[" + quote(key) + "~\"false|no|0|off\"]"
                    : "[" + quote(key) + "~\"true|yes|1|on\"]";
        } else if (match instanceof SearchCompiler.UserMatch) {
            final String user = ((SearchCompiler.UserMatch) match).getUser();
            return user.matches("\\d+")
                    ? "(uid:" + user + ")"
                    : "(user:" + quote(user) + ")";
        } else if (match instanceof SearchCompiler.ExactType) {
            types.add(((SearchCompiler.ExactType) match).getType());
            return "";
        }
        Logging.warn("Unsupported match type {0}: {1}", match.getClass(), match);
        return "/*" + match + "*/";
    }

    /**
     * Quotes the given string for its use in Overpass QL
     * @param s the string to quote
     * @return the quoted string
     */
    private static String quote(final String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /**
     * Normalizes the match to disjunctive normal form: A∧(B∨C) ⇔ (A∧B)∨(A∧C)
     * @param match the match to normalize
     * @return the match in disjunctive normal form
     */
    private static List<Match> normalizeToDNF(final Match match) {
        if (match instanceof SearchCompiler.And) {
            return ((SearchCompiler.And) match).map(OverpassTurboQueryWizard::normalizeToDNF, (lhs, rhs) -> lhs.stream()
                    .flatMap(l -> rhs.stream().map(r -> new SearchCompiler.And(l, r)))
                    .collect(Collectors.toList()));
        } else if (match instanceof SearchCompiler.Or) {
            return ((SearchCompiler.Or) match).map(OverpassTurboQueryWizard::normalizeToDNF, CompositeList::new);
        } else if (match instanceof SearchCompiler.Xor) {
            throw new UnsupportedOperationException(match.toString());
        } else if (match instanceof SearchCompiler.Not) {
            // only support negated KeyValue or ExactKeyValue matches
            final Match innerMatch = ((SearchCompiler.Not) match).getMatch();
            if (innerMatch instanceof SearchCompiler.BooleanMatch
                    || innerMatch instanceof SearchCompiler.KeyValue
                    || innerMatch instanceof SearchCompiler.ExactKeyValue) {
                return Collections.singletonList(match);
            }
            throw new UnsupportedOperationException(match.toString());
        } else {
            return Collections.singletonList(match);
        }
    }

}
