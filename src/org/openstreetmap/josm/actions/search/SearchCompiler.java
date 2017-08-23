// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.search;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.PushbackReader;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.PushbackTokenizer.Range;
import org.openstreetmap.josm.actions.search.PushbackTokenizer.Token;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSeparator;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 Implements a google-like search.
 <br>
 Grammar:
<pre>
expression =
  fact | expression
  fact expression
  fact

fact =
 ( expression )
 -fact
 term?
 term=term
 term:term
 term
 </pre>

 @author Imi
 */
public class SearchCompiler {

    private final boolean caseSensitive;
    private final boolean regexSearch;
    private static String rxErrorMsg = marktr("The regex \"{0}\" had a parse error at offset {1}, full error:\n\n{2}");
    private static String rxErrorMsgNoPos = marktr("The regex \"{0}\" had a parse error, full error:\n\n{1}");
    private final PushbackTokenizer tokenizer;
    private static Map<String, SimpleMatchFactory> simpleMatchFactoryMap = new HashMap<>();
    private static Map<String, UnaryMatchFactory> unaryMatchFactoryMap = new HashMap<>();
    private static Map<String, BinaryMatchFactory> binaryMatchFactoryMap = new HashMap<>();

    public SearchCompiler(boolean caseSensitive, boolean regexSearch, PushbackTokenizer tokenizer) {
        this.caseSensitive = caseSensitive;
        this.regexSearch = regexSearch;
        this.tokenizer = tokenizer;

        // register core match factories at first instance, so plugins should never be able to generate a NPE
        if (simpleMatchFactoryMap.isEmpty()) {
            addMatchFactory(new CoreSimpleMatchFactory());
        }
        if (unaryMatchFactoryMap.isEmpty()) {
            addMatchFactory(new CoreUnaryMatchFactory());
        }
    }

    /**
     * Add (register) MatchFactory with SearchCompiler
     * @param factory match factory
     */
    public static void addMatchFactory(MatchFactory factory) {
        for (String keyword : factory.getKeywords()) {
            final MatchFactory existing;
            if (factory instanceof SimpleMatchFactory) {
                existing = simpleMatchFactoryMap.put(keyword, (SimpleMatchFactory) factory);
            } else if (factory instanceof UnaryMatchFactory) {
                existing = unaryMatchFactoryMap.put(keyword, (UnaryMatchFactory) factory);
            } else if (factory instanceof BinaryMatchFactory) {
                existing = binaryMatchFactoryMap.put(keyword, (BinaryMatchFactory) factory);
            } else
                throw new AssertionError("Unknown match factory");
            if (existing != null) {
                Logging.warn("SearchCompiler: for key ''{0}'', overriding match factory ''{1}'' with ''{2}''", keyword, existing, factory);
            }
        }
    }

    public class CoreSimpleMatchFactory implements SimpleMatchFactory {
        private final Collection<String> keywords = Arrays.asList("id", "version", "type", "user", "role",
                "changeset", "nodes", "ways", "tags", "areasize", "waylength", "modified", "deleted", "selected",
                "incomplete", "untagged", "closed", "new", "indownloadedarea",
                "allindownloadedarea", "inview", "allinview", "timestamp", "nth", "nth%", "hasRole", "preset");

        @Override
        public Match get(String keyword, PushbackTokenizer tokenizer) throws ParseError {
            switch(keyword) {
            case "modified":
                return new Modified();
            case "deleted":
                return new Deleted();
            case "selected":
                return new Selected();
            case "incomplete":
                return new Incomplete();
            case "untagged":
                return new Untagged();
            case "closed":
                return new Closed();
            case "new":
                return new New();
            case "indownloadedarea":
                return new InDataSourceArea(false);
            case "allindownloadedarea":
                return new InDataSourceArea(true);
            case "inview":
                return new InView(false);
            case "allinview":
                return new InView(true);
            default:
                if (tokenizer != null) {
                    switch (keyword) {
                    case "id":
                        return new Id(tokenizer);
                    case "version":
                        return new Version(tokenizer);
                    case "type":
                        return new ExactType(tokenizer.readTextOrNumber());
                    case "preset":
                        return new Preset(tokenizer.readTextOrNumber());
                    case "user":
                        return new UserMatch(tokenizer.readTextOrNumber());
                    case "role":
                        return new RoleMatch(tokenizer.readTextOrNumber());
                    case "changeset":
                        return new ChangesetId(tokenizer);
                    case "nodes":
                        return new NodeCountRange(tokenizer);
                    case "ways":
                        return new WayCountRange(tokenizer);
                    case "tags":
                        return new TagCountRange(tokenizer);
                    case "areasize":
                        return new AreaSize(tokenizer);
                    case "waylength":
                        return new WayLength(tokenizer);
                    case "nth":
                        return new Nth(tokenizer, false);
                    case "nth%":
                        return new Nth(tokenizer, true);
                    case "hasRole":
                        return new HasRole(tokenizer);
                    case "timestamp":
                        // add leading/trailing space in order to get expected split (e.g. "a--" => {"a", ""})
                        String rangeS = ' ' + tokenizer.readTextOrNumber() + ' ';
                        String[] rangeA = rangeS.split("/");
                        if (rangeA.length == 1) {
                            return new KeyValue(keyword, rangeS.trim(), regexSearch, caseSensitive);
                        } else if (rangeA.length == 2) {
                            String rangeA1 = rangeA[0].trim();
                            String rangeA2 = rangeA[1].trim();
                            final long minDate;
                            final long maxDate;
                            try {
                                // if min timestap is empty: use lowest possible date
                                minDate = DateUtils.fromString(rangeA1.isEmpty() ? "1980" : rangeA1).getTime();
                            } catch (UncheckedParseException ex) {
                                throw new ParseError(tr("Cannot parse timestamp ''{0}''", rangeA1), ex);
                            }
                            try {
                                // if max timestamp is empty: use "now"
                                maxDate = rangeA2.isEmpty() ? System.currentTimeMillis() : DateUtils.fromString(rangeA2).getTime();
                            } catch (UncheckedParseException ex) {
                                throw new ParseError(tr("Cannot parse timestamp ''{0}''", rangeA2), ex);
                            }
                            return new TimestampRange(minDate, maxDate);
                        } else {
                            throw new ParseError("<html>" + tr("Expecting {0} after {1}", "<i>min</i>/<i>max</i>", "<i>timestamp</i>"));
                        }
                    }
                } else {
                    throw new ParseError("<html>" + tr("Expecting {0} after {1}", "<code>:</code>", "<i>" + keyword + "</i>"));
                }
            }
            throw new IllegalStateException("Not expecting keyword " + keyword);
        }

        @Override
        public Collection<String> getKeywords() {
            return keywords;
        }
    }

    public static class CoreUnaryMatchFactory implements UnaryMatchFactory {
        private static Collection<String> keywords = Arrays.asList("parent", "child");

        @Override
        public UnaryMatch get(String keyword, Match matchOperand, PushbackTokenizer tokenizer) {
            if ("parent".equals(keyword))
                return new Parent(matchOperand);
            else if ("child".equals(keyword))
                return new Child(matchOperand);
            return null;
        }

        @Override
        public Collection<String> getKeywords() {
            return keywords;
        }
    }

    /**
     * Classes implementing this interface can provide Match operators.
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    private interface MatchFactory {
        Collection<String> getKeywords();
    }

    public interface SimpleMatchFactory extends MatchFactory {
        Match get(String keyword, PushbackTokenizer tokenizer) throws ParseError;
    }

    public interface UnaryMatchFactory extends MatchFactory {
        UnaryMatch get(String keyword, Match matchOperand, PushbackTokenizer tokenizer) throws ParseError;
    }

    public interface BinaryMatchFactory extends MatchFactory {
        AbstractBinaryMatch get(String keyword, Match lhs, Match rhs, PushbackTokenizer tokenizer) throws ParseError;
    }

    /**
     * Base class for all search criteria. If the criterion only depends on an object's tags,
     * inherit from {@link org.openstreetmap.josm.actions.search.SearchCompiler.TaggedMatch}.
     */
    public abstract static class Match implements Predicate<OsmPrimitive> {

        /**
         * Tests whether the primitive matches this criterion.
         * @param osm the primitive to test
         * @return true if the primitive matches this criterion
         */
        public abstract boolean match(OsmPrimitive osm);

        /**
         * Tests whether the tagged object matches this criterion.
         * @param tagged the tagged object to test
         * @return true if the tagged object matches this criterion
         */
        public boolean match(Tagged tagged) {
            return false;
        }

        @Override
        public final boolean test(OsmPrimitive object) {
            return match(object);
        }
    }

    public abstract static class TaggedMatch extends Match {

        @Override
        public abstract boolean match(Tagged tags);

        @Override
        public final boolean match(OsmPrimitive osm) {
            return match((Tagged) osm);
        }
    }

    /**
     * A unary search operator which may take data parameters.
     */
    public abstract static class UnaryMatch extends Match {

        protected final Match match;

        public UnaryMatch(Match match) {
            if (match == null) {
                // "operator" (null) should mean the same as "operator()"
                // (Always). I.e. match everything
                this.match = Always.INSTANCE;
            } else {
                this.match = match;
            }
        }

        public Match getOperand() {
            return match;
        }
    }

    /**
     * A binary search operator which may take data parameters.
     */
    public abstract static class AbstractBinaryMatch extends Match {

        protected final Match lhs;
        protected final Match rhs;

        /**
         * Constructs a new {@code BinaryMatch}.
         * @param lhs Left hand side
         * @param rhs Right hand side
         */
        public AbstractBinaryMatch(Match lhs, Match rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        /**
         * Returns left hand side.
         * @return left hand side
         */
        public final Match getLhs() {
            return lhs;
        }

        /**
         * Returns right hand side.
         * @return right hand side
         */
        public final Match getRhs() {
            return rhs;
        }

        protected static String parenthesis(Match m) {
            return '(' + m.toString() + ')';
        }
    }

    /**
     * Matches every OsmPrimitive.
     */
    public static class Always extends TaggedMatch {
        /** The unique instance/ */
        public static final Always INSTANCE = new Always();
        @Override
        public boolean match(Tagged osm) {
            return true;
        }
    }

    /**
     * Never matches any OsmPrimitive.
     */
    public static class Never extends TaggedMatch {
        /** The unique instance/ */
        public static final Never INSTANCE = new Never();
        @Override
        public boolean match(Tagged osm) {
            return false;
        }
    }

    /**
     * Inverts the match.
     */
    public static class Not extends UnaryMatch {
        public Not(Match match) {
            super(match);
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return !match.match(osm);
        }

        @Override
        public boolean match(Tagged osm) {
            return !match.match(osm);
        }

        @Override
        public String toString() {
            return '!' + match.toString();
        }

        public Match getMatch() {
            return match;
        }
    }

    /**
     * Matches if the value of the corresponding key is ''yes'', ''true'', ''1'' or ''on''.
     */
    private static class BooleanMatch extends TaggedMatch {
        private final String key;
        private final boolean defaultValue;

        BooleanMatch(String key, boolean defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        @Override
        public boolean match(Tagged osm) {
            return Optional.ofNullable(OsmUtils.getOsmBoolean(osm.get(key))).orElse(defaultValue);
        }

        @Override
        public String toString() {
            return key + '?';
        }
    }

    /**
     * Matches if both left and right expressions match.
     */
    public static class And extends AbstractBinaryMatch {
        /**
         * Constructs a new {@code And} match.
         * @param lhs left hand side
         * @param rhs right hand side
         */
        public And(Match lhs, Match rhs) {
            super(lhs, rhs);
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return lhs.match(osm) && rhs.match(osm);
        }

        @Override
        public boolean match(Tagged osm) {
            return lhs.match(osm) && rhs.match(osm);
        }

        @Override
        public String toString() {
            return (lhs instanceof AbstractBinaryMatch && !(lhs instanceof And) ? parenthesis(lhs) : lhs) + " && "
                 + (rhs instanceof AbstractBinaryMatch && !(rhs instanceof And) ? parenthesis(rhs) : rhs);
        }
    }

    /**
     * Matches if the left OR the right expression match.
     */
    public static class Or extends AbstractBinaryMatch {
        /**
         * Constructs a new {@code Or} match.
         * @param lhs left hand side
         * @param rhs right hand side
         */
        public Or(Match lhs, Match rhs) {
            super(lhs, rhs);
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return lhs.match(osm) || rhs.match(osm);
        }

        @Override
        public boolean match(Tagged osm) {
            return lhs.match(osm) || rhs.match(osm);
        }

        @Override
        public String toString() {
            return (lhs instanceof AbstractBinaryMatch && !(lhs instanceof Or) ? parenthesis(lhs) : lhs) + " || "
                 + (rhs instanceof AbstractBinaryMatch && !(rhs instanceof Or) ? parenthesis(rhs) : rhs);
        }
    }

    /**
     * Matches if the left OR the right expression match, but not both.
     */
    public static class Xor extends AbstractBinaryMatch {
        /**
         * Constructs a new {@code Xor} match.
         * @param lhs left hand side
         * @param rhs right hand side
         */
        public Xor(Match lhs, Match rhs) {
            super(lhs, rhs);
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return lhs.match(osm) ^ rhs.match(osm);
        }

        @Override
        public boolean match(Tagged osm) {
            return lhs.match(osm) ^ rhs.match(osm);
        }

        @Override
        public String toString() {
            return (lhs instanceof AbstractBinaryMatch && !(lhs instanceof Xor) ? parenthesis(lhs) : lhs) + " ^ "
                 + (rhs instanceof AbstractBinaryMatch && !(rhs instanceof Xor) ? parenthesis(rhs) : rhs);
        }
    }

    /**
     * Matches objects with ID in the given range.
     */
    private static class Id extends RangeMatch {
        Id(Range range) {
            super(range);
        }

        Id(PushbackTokenizer tokenizer) throws ParseError {
            this(tokenizer.readRange(tr("Range of primitive ids expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            return osm.isNew() ? 0 : osm.getUniqueId();
        }

        @Override
        protected String getString() {
            return "id";
        }
    }

    /**
     * Matches objects with a changeset ID in the given range.
     */
    private static class ChangesetId extends RangeMatch {
        ChangesetId(Range range) {
            super(range);
        }

        ChangesetId(PushbackTokenizer tokenizer) throws ParseError {
            this(tokenizer.readRange(tr("Range of changeset ids expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            return (long) osm.getChangesetId();
        }

        @Override
        protected String getString() {
            return "changeset";
        }
    }

    /**
     * Matches objects with a version number in the given range.
     */
    private static class Version extends RangeMatch {
        Version(Range range) {
            super(range);
        }

        Version(PushbackTokenizer tokenizer) throws ParseError {
            this(tokenizer.readRange(tr("Range of versions expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            return (long) osm.getVersion();
        }

        @Override
        protected String getString() {
            return "version";
        }
    }

    /**
     * Matches objects with the given key-value pair.
     */
    private static class KeyValue extends TaggedMatch {
        private final String key;
        private final Pattern keyPattern;
        private final String value;
        private final Pattern valuePattern;
        private final boolean caseSensitive;

        KeyValue(String key, String value, boolean regexSearch, boolean caseSensitive) throws ParseError {
            this.caseSensitive = caseSensitive;
            if (regexSearch) {
                int searchFlags = regexFlags(caseSensitive);

                try {
                    this.keyPattern = Pattern.compile(key, searchFlags);
                } catch (PatternSyntaxException e) {
                    throw new ParseError(tr(rxErrorMsg, e.getPattern(), e.getIndex(), e.getMessage()), e);
                } catch (IllegalArgumentException e) {
                    throw new ParseError(tr(rxErrorMsgNoPos, key, e.getMessage()), e);
                }
                try {
                    this.valuePattern = Pattern.compile(value, searchFlags);
                } catch (PatternSyntaxException e) {
                    throw new ParseError(tr(rxErrorMsg, e.getPattern(), e.getIndex(), e.getMessage()), e);
                } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                    throw new ParseError(tr(rxErrorMsgNoPos, value, e.getMessage()), e);
                }
                this.key = key;
                this.value = value;

            } else {
                this.key = key;
                this.value = value;
                this.keyPattern = null;
                this.valuePattern = null;
            }
        }

        @Override
        public boolean match(Tagged osm) {

            if (keyPattern != null) {
                if (!osm.hasKeys())
                    return false;

                /* The string search will just get a key like
                 * 'highway' and look that up as osm.get(key). But
                 * since we're doing a regex match we'll have to loop
                 * over all the keys to see if they match our regex,
                 * and only then try to match against the value
                 */

                for (String k: osm.keySet()) {
                    String v = osm.get(k);

                    Matcher matcherKey = keyPattern.matcher(k);
                    boolean matchedKey = matcherKey.find();

                    if (matchedKey) {
                        Matcher matcherValue = valuePattern.matcher(v);
                        boolean matchedValue = matcherValue.find();

                        if (matchedValue)
                            return true;
                    }
                }
            } else {
                String mv;

                if ("timestamp".equals(key) && osm instanceof OsmPrimitive) {
                    mv = DateUtils.fromTimestamp(((OsmPrimitive) osm).getRawTimestamp());
                } else {
                    mv = osm.get(key);
                    if (!caseSensitive && mv == null) {
                        for (String k: osm.keySet()) {
                            if (key.equalsIgnoreCase(k)) {
                                mv = osm.get(k);
                                break;
                            }
                        }
                    }
                }

                if (mv == null)
                    return false;

                String v1 = caseSensitive ? mv : mv.toLowerCase(Locale.ENGLISH);
                String v2 = caseSensitive ? value : value.toLowerCase(Locale.ENGLISH);

                v1 = Normalizer.normalize(v1, Normalizer.Form.NFC);
                v2 = Normalizer.normalize(v2, Normalizer.Form.NFC);
                return v1.indexOf(v2) != -1;
            }

            return false;
        }

        @Override
        public String toString() {
            return key + '=' + value;
        }
    }

    public static class ValueComparison extends TaggedMatch {
        private final String key;
        private final String referenceValue;
        private final Double referenceNumber;
        private final int compareMode;
        private static final Pattern ISO8601 = Pattern.compile("\\d+-\\d+-\\d+");

        public ValueComparison(String key, String referenceValue, int compareMode) {
            this.key = key;
            this.referenceValue = referenceValue;
            Double v = null;
            try {
                if (referenceValue != null) {
                    v = Double.valueOf(referenceValue);
                }
            } catch (NumberFormatException ignore) {
                Logging.trace(ignore);
            }
            this.referenceNumber = v;
            this.compareMode = compareMode;
        }

        @Override
        public boolean match(Tagged osm) {
            final String currentValue = osm.get(key);
            final int compareResult;
            if (currentValue == null) {
                return false;
            } else if (ISO8601.matcher(currentValue).matches() || ISO8601.matcher(referenceValue).matches()) {
                compareResult = currentValue.compareTo(referenceValue);
            } else if (referenceNumber != null) {
                try {
                    compareResult = Double.compare(Double.parseDouble(currentValue), referenceNumber);
                } catch (NumberFormatException ignore) {
                    return false;
                }
            } else {
                compareResult = AlphanumComparator.getInstance().compare(currentValue, referenceValue);
            }
            return compareMode < 0 ? compareResult < 0 : compareMode > 0 ? compareResult > 0 : compareResult == 0;
        }

        @Override
        public String toString() {
            return key + (compareMode == -1 ? "<" : compareMode == +1 ? ">" : "") + referenceValue;
        }
    }

    /**
     * Matches objects with the exact given key-value pair.
     */
    public static class ExactKeyValue extends TaggedMatch {

        enum Mode {
            ANY, ANY_KEY, ANY_VALUE, EXACT, NONE, MISSING_KEY,
            ANY_KEY_REGEXP, ANY_VALUE_REGEXP, EXACT_REGEXP, MISSING_KEY_REGEXP;
        }

        private final String key;
        private final String value;
        private final Pattern keyPattern;
        private final Pattern valuePattern;
        private final Mode mode;

        /**
         * Constructs a new {@code ExactKeyValue}.
         * @param regexp regular expression
         * @param key key
         * @param value value
         * @throws ParseError if a parse error occurs
         */
        public ExactKeyValue(boolean regexp, String key, String value) throws ParseError {
            if ("".equals(key))
                throw new ParseError(tr("Key cannot be empty when tag operator is used. Sample use: key=value"));
            this.key = key;
            this.value = value == null ? "" : value;
            if ("".equals(this.value) && "*".equals(key)) {
                mode = Mode.NONE;
            } else if ("".equals(this.value)) {
                if (regexp) {
                    mode = Mode.MISSING_KEY_REGEXP;
                } else {
                    mode = Mode.MISSING_KEY;
                }
            } else if ("*".equals(key) && "*".equals(this.value)) {
                mode = Mode.ANY;
            } else if ("*".equals(key)) {
                if (regexp) {
                    mode = Mode.ANY_KEY_REGEXP;
                } else {
                    mode = Mode.ANY_KEY;
                }
            } else if ("*".equals(this.value)) {
                if (regexp) {
                    mode = Mode.ANY_VALUE_REGEXP;
                } else {
                    mode = Mode.ANY_VALUE;
                }
            } else {
                if (regexp) {
                    mode = Mode.EXACT_REGEXP;
                } else {
                    mode = Mode.EXACT;
                }
            }

            if (regexp && !key.isEmpty() && !"*".equals(key)) {
                try {
                    keyPattern = Pattern.compile(key, regexFlags(false));
                } catch (PatternSyntaxException e) {
                    throw new ParseError(tr(rxErrorMsg, e.getPattern(), e.getIndex(), e.getMessage()), e);
                } catch (IllegalArgumentException e) {
                    throw new ParseError(tr(rxErrorMsgNoPos, key, e.getMessage()), e);
                }
            } else {
                keyPattern = null;
            }
            if (regexp && !this.value.isEmpty() && !"*".equals(this.value)) {
                try {
                    valuePattern = Pattern.compile(this.value, regexFlags(false));
                } catch (PatternSyntaxException e) {
                    throw new ParseError(tr(rxErrorMsg, e.getPattern(), e.getIndex(), e.getMessage()), e);
                } catch (IllegalArgumentException e) {
                    throw new ParseError(tr(rxErrorMsgNoPos, value, e.getMessage()), e);
                }
            } else {
                valuePattern = null;
            }
        }

        @Override
        public boolean match(Tagged osm) {

            if (!osm.hasKeys())
                return mode == Mode.NONE;

            switch (mode) {
            case NONE:
                return false;
            case MISSING_KEY:
                return osm.get(key) == null;
            case ANY:
                return true;
            case ANY_VALUE:
                return osm.get(key) != null;
            case ANY_KEY:
                for (String v:osm.getKeys().values()) {
                    if (v.equals(value))
                        return true;
                }
                return false;
            case EXACT:
                return value.equals(osm.get(key));
            case ANY_KEY_REGEXP:
                for (String v:osm.getKeys().values()) {
                    if (valuePattern.matcher(v).matches())
                        return true;
                }
                return false;
            case ANY_VALUE_REGEXP:
            case EXACT_REGEXP:
                for (String k : osm.keySet()) {
                    if (keyPattern.matcher(k).matches()
                            && (mode == Mode.ANY_VALUE_REGEXP || valuePattern.matcher(osm.get(k)).matches()))
                        return true;
                }
                return false;
            case MISSING_KEY_REGEXP:
                for (String k:osm.keySet()) {
                    if (keyPattern.matcher(k).matches())
                        return false;
                }
                return true;
            }
            throw new AssertionError("Missed state");
        }

        @Override
        public String toString() {
            return key + '=' + value;
        }
    }

    /**
     * Match a string in any tags (key or value), with optional regex and case insensitivity.
     */
    private static class Any extends TaggedMatch {
        private final String search;
        private final Pattern searchRegex;
        private final boolean caseSensitive;

        Any(String s, boolean regexSearch, boolean caseSensitive) throws ParseError {
            s = Normalizer.normalize(s, Normalizer.Form.NFC);
            this.caseSensitive = caseSensitive;
            if (regexSearch) {
                try {
                    this.searchRegex = Pattern.compile(s, regexFlags(caseSensitive));
                } catch (PatternSyntaxException e) {
                    throw new ParseError(tr(rxErrorMsg, e.getPattern(), e.getIndex(), e.getMessage()), e);
                } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                    // StringIndexOutOfBoundsException catched because of https://bugs.openjdk.java.net/browse/JI-9044959
                    // See #13870: To remove after we switch to a version of Java which resolves this bug
                    throw new ParseError(tr(rxErrorMsgNoPos, s, e.getMessage()), e);
                }
                this.search = s;
            } else if (caseSensitive) {
                this.search = s;
                this.searchRegex = null;
            } else {
                this.search = s.toLowerCase(Locale.ENGLISH);
                this.searchRegex = null;
            }
        }

        @Override
        public boolean match(Tagged osm) {
            if (!osm.hasKeys())
                return search.isEmpty();

            for (String key: osm.keySet()) {
                String value = osm.get(key);
                if (searchRegex != null) {

                    value = Normalizer.normalize(value, Normalizer.Form.NFC);

                    Matcher keyMatcher = searchRegex.matcher(key);
                    Matcher valMatcher = searchRegex.matcher(value);

                    boolean keyMatchFound = keyMatcher.find();
                    boolean valMatchFound = valMatcher.find();

                    if (keyMatchFound || valMatchFound)
                        return true;
                } else {
                    if (!caseSensitive) {
                        key = key.toLowerCase(Locale.ENGLISH);
                        value = value.toLowerCase(Locale.ENGLISH);
                    }

                    value = Normalizer.normalize(value, Normalizer.Form.NFC);

                    if (key.indexOf(search) != -1 || value.indexOf(search) != -1)
                        return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return search;
        }
    }

    private static class ExactType extends Match {
        private final OsmPrimitiveType type;

        ExactType(String type) throws ParseError {
            this.type = OsmPrimitiveType.from(type);
            if (this.type == null)
                throw new ParseError(tr("Unknown primitive type: {0}. Allowed values are node, way or relation", type));
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return type.equals(osm.getType());
        }

        @Override
        public String toString() {
            return "type=" + type;
        }
    }

    /**
     * Matches objects last changed by the given username.
     */
    private static class UserMatch extends Match {
        private String user;

        UserMatch(String user) {
            if ("anonymous".equals(user)) {
                this.user = null;
            } else {
                this.user = user;
            }
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            if (osm.getUser() == null)
                return user == null;
            else
                return osm.getUser().hasName(user);
        }

        @Override
        public String toString() {
            return "user=" + (user == null ? "" : user);
        }
    }

    /**
     * Matches objects with the given relation role (i.e. "outer").
     */
    private static class RoleMatch extends Match {
        private String role;

        RoleMatch(String role) {
            if (role == null) {
                this.role = "";
            } else {
                this.role = role;
            }
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            for (OsmPrimitive ref: osm.getReferrers()) {
                if (ref instanceof Relation && !ref.isIncomplete() && !ref.isDeleted()) {
                    for (RelationMember m : ((Relation) ref).getMembers()) {
                        if (m.getMember() == osm) {
                            String testRole = m.getRole();
                            if (role.equals(testRole == null ? "" : testRole))
                                return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "role=" + role;
        }
    }

    /**
     * Matches the n-th object of a relation and/or the n-th node of a way.
     */
    private static class Nth extends Match {

        private final int nth;
        private final boolean modulo;

        Nth(PushbackTokenizer tokenizer, boolean modulo) throws ParseError {
            this((int) tokenizer.readNumber(tr("Positive integer expected")), modulo);
        }

        private Nth(int nth, boolean modulo) {
            this.nth = nth;
            this.modulo = modulo;
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            for (OsmPrimitive p : osm.getReferrers()) {
                final int idx;
                final int maxIndex;
                if (p instanceof Way) {
                    Way w = (Way) p;
                    idx = w.getNodes().indexOf(osm);
                    maxIndex = w.getNodesCount();
                } else if (p instanceof Relation) {
                    Relation r = (Relation) p;
                    idx = r.getMemberPrimitivesList().indexOf(osm);
                    maxIndex = r.getMembersCount();
                } else {
                    continue;
                }
                if (nth < 0 && idx - maxIndex == nth) {
                    return true;
                } else if (idx == nth || (modulo && idx % nth == 0))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Nth{nth=" + nth + ", modulo=" + modulo + '}';
        }
    }

    /**
     * Matches objects with properties in a certain range.
     */
    private abstract static class RangeMatch extends Match {

        private final long min;
        private final long max;

        RangeMatch(long min, long max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
        }

        RangeMatch(Range range) {
            this(range.getStart(), range.getEnd());
        }

        protected abstract Long getNumber(OsmPrimitive osm);

        protected abstract String getString();

        @Override
        public boolean match(OsmPrimitive osm) {
            Long num = getNumber(osm);
            if (num == null)
                return false;
            else
                return (num >= min) && (num <= max);
        }

        @Override
        public String toString() {
            return getString() + '=' + min + '-' + max;
        }
    }

    /**
     * Matches ways with a number of nodes in given range
     */
    private static class NodeCountRange extends RangeMatch {
        NodeCountRange(Range range) {
            super(range);
        }

        NodeCountRange(PushbackTokenizer tokenizer) throws ParseError {
            this(tokenizer.readRange(tr("Range of numbers expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            if (osm instanceof Way) {
                return (long) ((Way) osm).getRealNodesCount();
            } else if (osm instanceof Relation) {
                return (long) ((Relation) osm).getMemberPrimitives(Node.class).size();
            } else {
                return null;
            }
        }

        @Override
        protected String getString() {
            return "nodes";
        }
    }

    /**
     * Matches objects with the number of referring/contained ways in the given range
     */
    private static class WayCountRange extends RangeMatch {
        WayCountRange(Range range) {
            super(range);
        }

        WayCountRange(PushbackTokenizer tokenizer) throws ParseError {
            this(tokenizer.readRange(tr("Range of numbers expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            if (osm instanceof Node) {
                return (long) Utils.filteredCollection(osm.getReferrers(), Way.class).size();
            } else if (osm instanceof Relation) {
                return (long) ((Relation) osm).getMemberPrimitives(Way.class).size();
            } else {
                return null;
            }
        }

        @Override
        protected String getString() {
            return "ways";
        }
    }

    /**
     * Matches objects with a number of tags in given range
     */
    private static class TagCountRange extends RangeMatch {
        TagCountRange(Range range) {
            super(range);
        }

        TagCountRange(PushbackTokenizer tokenizer) throws ParseError {
            this(tokenizer.readRange(tr("Range of numbers expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            return (long) osm.getKeys().size();
        }

        @Override
        protected String getString() {
            return "tags";
        }
    }

    /**
     * Matches objects with a timestamp in given range
     */
    private static class TimestampRange extends RangeMatch {

        TimestampRange(long minCount, long maxCount) {
            super(minCount, maxCount);
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            return osm.getTimestamp().getTime();
        }

        @Override
        protected String getString() {
            return "timestamp";
        }
    }

    /**
     * Matches relations with a member of the given role
     */
    private static class HasRole extends Match {
        private final String role;

        HasRole(PushbackTokenizer tokenizer) {
            role = tokenizer.readTextOrNumber();
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return osm instanceof Relation && ((Relation) osm).getMemberRoles().contains(role);
        }
    }

    /**
     * Matches objects that are new (i.e. have not been uploaded to the server)
     */
    private static class New extends Match {
        @Override
        public boolean match(OsmPrimitive osm) {
            return osm.isNew();
        }

        @Override
        public String toString() {
            return "new";
        }
    }

    /**
     * Matches all objects that have been modified, created, or undeleted
     */
    private static class Modified extends Match {
        @Override
        public boolean match(OsmPrimitive osm) {
            return osm.isModified() || osm.isNewOrUndeleted();
        }

        @Override
        public String toString() {
            return "modified";
        }
    }

    /**
     * Matches all objects that have been deleted
     */
    private static class Deleted extends Match {
        @Override
        public boolean match(OsmPrimitive osm) {
            return osm.isDeleted();
        }

        @Override
        public String toString() {
            return "deleted";
        }
    }

    /**
     * Matches all objects currently selected
     */
    private static class Selected extends Match {
        @Override
        public boolean match(OsmPrimitive osm) {
            return osm.getDataSet().isSelected(osm);
        }

        @Override
        public String toString() {
            return "selected";
        }
    }

    /**
     * Match objects that are incomplete, where only id and type are known.
     * Typically some members of a relation are incomplete until they are
     * fetched from the server.
     */
    private static class Incomplete extends Match {
        @Override
        public boolean match(OsmPrimitive osm) {
            return osm.isIncomplete() || (osm instanceof Relation && ((Relation) osm).hasIncompleteMembers());
        }

        @Override
        public String toString() {
            return "incomplete";
        }
    }

    /**
     * Matches objects that don't have any interesting tags (i.e. only has source,
     * FIXME, etc.). The complete list of uninteresting tags can be found here:
     * org.openstreetmap.josm.data.osm.OsmPrimitive.getUninterestingKeys()
     */
    private static class Untagged extends Match {
        @Override
        public boolean match(OsmPrimitive osm) {
            return !osm.isTagged() && !osm.isIncomplete();
        }

        @Override
        public String toString() {
            return "untagged";
        }
    }

    /**
     * Matches ways which are closed (i.e. first and last node are the same)
     */
    private static class Closed extends Match {
        @Override
        public boolean match(OsmPrimitive osm) {
            return osm instanceof Way && ((Way) osm).isClosed();
        }

        @Override
        public String toString() {
            return "closed";
        }
    }

    /**
     * Matches objects if they are parents of the expression
     */
    public static class Parent extends UnaryMatch {
        public Parent(Match m) {
            super(m);
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            boolean isParent = false;

            if (osm instanceof Way) {
                for (Node n : ((Way) osm).getNodes()) {
                    isParent |= match.match(n);
                }
            } else if (osm instanceof Relation) {
                for (RelationMember member : ((Relation) osm).getMembers()) {
                    isParent |= match.match(member.getMember());
                }
            }
            return isParent;
        }

        @Override
        public String toString() {
            return "parent(" + match + ')';
        }
    }

    /**
     * Matches objects if they are children of the expression
     */
    public static class Child extends UnaryMatch {

        public Child(Match m) {
            super(m);
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            boolean isChild = false;
            for (OsmPrimitive p : osm.getReferrers()) {
                isChild |= match.match(p);
            }
            return isChild;
        }

        @Override
        public String toString() {
            return "child(" + match + ')';
        }
    }

    /**
     * Matches if the size of the area is within the given range
     *
     * @author Ole Jørgen Brønner
     */
    private static class AreaSize extends RangeMatch {

        AreaSize(Range range) {
            super(range);
        }

        AreaSize(PushbackTokenizer tokenizer) throws ParseError {
            this(tokenizer.readRange(tr("Range of numbers expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            final Double area = Geometry.computeArea(osm);
            return area == null ? null : area.longValue();
        }

        @Override
        protected String getString() {
            return "areasize";
        }
    }

    /**
     * Matches if the length of a way is within the given range
     */
    private static class WayLength extends RangeMatch {

        WayLength(Range range) {
            super(range);
        }

        WayLength(PushbackTokenizer tokenizer) throws ParseError {
            this(tokenizer.readRange(tr("Range of numbers expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            if (!(osm instanceof Way))
                return null;
            Way way = (Way) osm;
            return (long) way.getLength();
        }

        @Override
        protected String getString() {
            return "waylength";
        }
    }

    /**
     * Matches objects within the given bounds.
     */
    private abstract static class InArea extends Match {

        protected final boolean all;

        /**
         * @param all if true, all way nodes or relation members have to be within source area;if false, one suffices.
         */
        InArea(boolean all) {
            this.all = all;
        }

        protected abstract Collection<Bounds> getBounds(OsmPrimitive primitive);

        @Override
        public boolean match(OsmPrimitive osm) {
            if (!osm.isUsable())
                return false;
            else if (osm instanceof Node) {
                LatLon coordinate = ((Node) osm).getCoor();
                Collection<Bounds> allBounds = getBounds(osm);
                return coordinate != null && allBounds != null && allBounds.stream().anyMatch(bounds -> bounds.contains(coordinate));
            } else if (osm instanceof Way) {
                Collection<Node> nodes = ((Way) osm).getNodes();
                return all ? nodes.stream().allMatch(this) : nodes.stream().anyMatch(this);
            } else if (osm instanceof Relation) {
                Collection<OsmPrimitive> primitives = ((Relation) osm).getMemberPrimitivesList();
                return all ? primitives.stream().allMatch(this) : primitives.stream().anyMatch(this);
            } else
                return false;
        }
    }

    /**
     * Matches objects within source area ("downloaded area").
     */
    public static class InDataSourceArea extends InArea {

        /**
         * Constructs a new {@code InDataSourceArea}.
         * @param all if true, all way nodes or relation members have to be within source area; if false, one suffices.
         */
        public InDataSourceArea(boolean all) {
            super(all);
        }

        @Override
        protected Collection<Bounds> getBounds(OsmPrimitive primitive) {
            return primitive.getDataSet() != null ? primitive.getDataSet().getDataSourceBounds() : null;
        }

        @Override
        public String toString() {
            return all ? "allindownloadedarea" : "indownloadedarea";
        }
    }

    /**
     * Matches objects which are not outside the source area ("downloaded area").
     * Unlike {@link InDataSourceArea} this matches also if no source area is set (e.g., for new layers).
     */
    public static class NotOutsideDataSourceArea extends InDataSourceArea {

        /**
         * Constructs a new {@code NotOutsideDataSourceArea}.
         */
        public NotOutsideDataSourceArea() {
            super(false);
        }

        @Override
        protected Collection<Bounds> getBounds(OsmPrimitive primitive) {
            final Collection<Bounds> bounds = super.getBounds(primitive);
            return bounds == null || bounds.isEmpty() ? Collections.singleton(Main.getProjection().getWorldBoundsLatLon()) : bounds;
        }

        @Override
        public String toString() {
            return "NotOutsideDataSourceArea";
        }
    }

    /**
     * Matches objects within current map view.
     */
    private static class InView extends InArea {

        InView(boolean all) {
            super(all);
        }

        @Override
        protected Collection<Bounds> getBounds(OsmPrimitive primitive) {
            if (!MainApplication.isDisplayingMapView()) {
                return null;
            }
            return Collections.singleton(MainApplication.getMap().mapView.getRealBounds());
        }

        @Override
        public String toString() {
            return all ? "allinview" : "inview";
        }
    }

    /**
     * Matches presets.
     * @since 12464
     */
    private static class Preset extends Match {
        private final List<TaggingPreset> presets;

        Preset(String presetName) throws ParseError {

            if (presetName == null || presetName.isEmpty()) {
                throw new ParseError("The name of the preset is required");
            }

            int wildCardIdx = presetName.lastIndexOf('*');
            int length = presetName.length() - 1;

            /*
             * Match strictly (simply comparing the names) if there is no '*' symbol
             * at the end of the name or '*' is a part of the preset name.
             */
            boolean matchStrictly = wildCardIdx == -1 || wildCardIdx != length;

            this.presets = TaggingPresets.getTaggingPresets()
                    .stream()
                    .filter(preset -> !(preset instanceof TaggingPresetMenu || preset instanceof TaggingPresetSeparator))
                    .filter(preset -> presetNameMatch(presetName, preset, matchStrictly))
                    .collect(Collectors.toList());

            if (this.presets.isEmpty()) {
                throw new ParseError(tr("Unknown preset name: ") + presetName);
            }
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            for (TaggingPreset preset : this.presets) {
                if (preset.test(osm)) {
                    return true;
                }
            }

            return false;
        }

        private static boolean presetNameMatch(String name, TaggingPreset preset, boolean matchStrictly) {
            if (matchStrictly) {
                return name.equalsIgnoreCase(preset.getRawName());
            }

            try {
                String groupSuffix = name.substring(0, name.length() - 2); // try to remove '/*'
                TaggingPresetMenu group = preset.group;

                return group != null && groupSuffix.equalsIgnoreCase(group.getRawName());
            } catch (StringIndexOutOfBoundsException ex) {
                return false;
            }
        }
    }

    public static class ParseError extends Exception {
        public ParseError(String msg) {
            super(msg);
        }

        public ParseError(String msg, Throwable cause) {
            super(msg, cause);
        }

        public ParseError(Token expected, Token found) {
            this(tr("Unexpected token. Expected {0}, found {1}", expected, found));
        }
    }

    /**
     * Compiles the search expression.
     * @param searchStr the search expression
     * @return a {@link Match} object for the expression
     * @throws ParseError if an error has been encountered while compiling
     * @see #compile(org.openstreetmap.josm.actions.search.SearchAction.SearchSetting)
     */
    public static Match compile(String searchStr) throws ParseError {
        return new SearchCompiler(false, false,
                new PushbackTokenizer(
                        new PushbackReader(new StringReader(searchStr))))
                .parse();
    }

    /**
     * Compiles the search expression.
     * @param setting the settings to use
     * @return a {@link Match} object for the expression
     * @throws ParseError if an error has been encountered while compiling
     * @see #compile(String)
     */
    public static Match compile(SearchAction.SearchSetting setting) throws ParseError {
        if (setting.mapCSSSearch) {
            return compileMapCSS(setting.text);
        }
        return new SearchCompiler(setting.caseSensitive, setting.regexSearch,
                new PushbackTokenizer(
                        new PushbackReader(new StringReader(setting.text))))
                .parse();
    }

    static Match compileMapCSS(String mapCSS) throws ParseError {
        try {
            final List<Selector> selectors = new MapCSSParser(new StringReader(mapCSS)).selectors();
            return new Match() {
                @Override
                public boolean match(OsmPrimitive osm) {
                    for (Selector selector : selectors) {
                        if (selector.matches(new Environment(osm))) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        } catch (ParseException e) {
            throw new ParseError(tr("Failed to parse MapCSS selector"), e);
        }
    }

    /**
     * Parse search string.
     *
     * @return match determined by search string
     * @throws org.openstreetmap.josm.actions.search.SearchCompiler.ParseError if search expression cannot be parsed
     */
    public Match parse() throws ParseError {
        Match m = Optional.ofNullable(parseExpression()).orElse(Always.INSTANCE);
        if (!tokenizer.readIfEqual(Token.EOF))
            throw new ParseError(tr("Unexpected token: {0}", tokenizer.nextToken()));
        Logging.debug("Parsed search expression is {0}", m);
        return m;
    }

    /**
     * Parse expression.
     *
     * @return match determined by parsing expression
     * @throws ParseError if search expression cannot be parsed
     */
    private Match parseExpression() throws ParseError {
        // Step 1: parse the whole expression and build a list of factors and logical tokens
        List<Object> list = parseExpressionStep1();
        // Step 2: iterate the list in reverse order to build the logical expression
        // This iterative approach avoids StackOverflowError for long expressions (see #14217)
        return parseExpressionStep2(list);
    }

    private List<Object> parseExpressionStep1() throws ParseError {
        Match factor;
        String token = null;
        String errorMessage = null;
        List<Object> list = new ArrayList<>();
        do {
            factor = parseFactor();
            if (factor != null) {
                if (token != null) {
                    list.add(token);
                }
                list.add(factor);
                if (tokenizer.readIfEqual(Token.OR)) {
                    token = "OR";
                    errorMessage = tr("Missing parameter for OR");
                } else if (tokenizer.readIfEqual(Token.XOR)) {
                    token = "XOR";
                    errorMessage = tr("Missing parameter for XOR");
                } else {
                    token = "AND";
                    errorMessage = null;
                }
            } else if (errorMessage != null) {
                throw new ParseError(errorMessage);
            }
        } while (factor != null);
        return list;
    }

    private static Match parseExpressionStep2(List<Object> list) {
        Match result = null;
        for (int i = list.size() - 1; i >= 0; i--) {
            Object o = list.get(i);
            if (o instanceof Match && result == null) {
                result = (Match) o;
            } else if (o instanceof String && i > 0) {
                Match factor = (Match) list.get(i-1);
                switch ((String) o) {
                case "OR":
                    result = new Or(factor, result);
                    break;
                case "XOR":
                    result = new Xor(factor, result);
                    break;
                case "AND":
                    result = new And(factor, result);
                    break;
                default: throw new IllegalStateException(tr("Unexpected token: {0}", o));
                }
                i--;
            } else {
                throw new IllegalStateException("i=" + i + "; o=" + o);
            }
        }
        return result;
    }

    /**
     * Parse next factor (a search operator or search term).
     *
     * @return match determined by parsing factor string
     * @throws ParseError if search expression cannot be parsed
     */
    private Match parseFactor() throws ParseError {
        if (tokenizer.readIfEqual(Token.LEFT_PARENT)) {
            Match expression = parseExpression();
            if (!tokenizer.readIfEqual(Token.RIGHT_PARENT))
                throw new ParseError(Token.RIGHT_PARENT, tokenizer.nextToken());
            return expression;
        } else if (tokenizer.readIfEqual(Token.NOT)) {
            return new Not(parseFactor(tr("Missing operator for NOT")));
        } else if (tokenizer.readIfEqual(Token.KEY)) {
            // factor consists of key:value or key=value
            String key = tokenizer.getText();
            if (tokenizer.readIfEqual(Token.EQUALS)) {
                return new ExactKeyValue(regexSearch, key, tokenizer.readTextOrNumber());
            } else if (tokenizer.readIfEqual(Token.LESS_THAN)) {
                return new ValueComparison(key, tokenizer.readTextOrNumber(), -1);
            } else if (tokenizer.readIfEqual(Token.GREATER_THAN)) {
                return new ValueComparison(key, tokenizer.readTextOrNumber(), +1);
            } else if (tokenizer.readIfEqual(Token.COLON)) {
                // see if we have a Match that takes a data parameter
                SimpleMatchFactory factory = simpleMatchFactoryMap.get(key);
                if (factory != null)
                    return factory.get(key, tokenizer);

                UnaryMatchFactory unaryFactory = unaryMatchFactoryMap.get(key);
                if (unaryFactory != null)
                    return unaryFactory.get(key, parseFactor(), tokenizer);

                // key:value form where value is a string (may be OSM key search)
                final String value = tokenizer.readTextOrNumber();
                return new KeyValue(key, value != null ? value : "", regexSearch, caseSensitive);
            } else if (tokenizer.readIfEqual(Token.QUESTION_MARK))
                return new BooleanMatch(key, false);
            else {
                SimpleMatchFactory factory = simpleMatchFactoryMap.get(key);
                if (factory != null)
                    return factory.get(key, null);

                UnaryMatchFactory unaryFactory = unaryMatchFactoryMap.get(key);
                if (unaryFactory != null)
                    return unaryFactory.get(key, parseFactor(), null);

                // match string in any key or value
                return new Any(key, regexSearch, caseSensitive);
            }
        } else
            return null;
    }

    private Match parseFactor(String errorMessage) throws ParseError {
        return Optional.ofNullable(parseFactor()).orElseThrow(() -> new ParseError(errorMessage));
    }

    private static int regexFlags(boolean caseSensitive) {
        int searchFlags = 0;

        // Enables canonical Unicode equivalence so that e.g. the two
        // forms of "\u00e9gal" and "e\u0301gal" will match.
        //
        // It makes sense to match no matter how the character
        // happened to be constructed.
        searchFlags |= Pattern.CANON_EQ;

        // Make "." match any character including newline (/s in Perl)
        searchFlags |= Pattern.DOTALL;

        // CASE_INSENSITIVE by itself only matches US-ASCII case
        // insensitively, but the OSM data is in Unicode. With
        // UNICODE_CASE casefolding is made Unicode-aware.
        if (!caseSensitive) {
            searchFlags |= (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }

        return searchFlags;
    }

    static String escapeStringForSearch(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Builds a search string for the given tag. If value is empty, the existence of the key is checked.
     *
     * @param key   the tag key
     * @param value the tag value
     * @return a search string for the given tag
     */
    public static String buildSearchStringForTag(String key, String value) {
        final String forKey = '"' + escapeStringForSearch(key) + '"' + '=';
        if (value == null || value.isEmpty()) {
            return forKey + '*';
        } else {
            return forKey + '"' + escapeStringForSearch(value) + '"';
        }
    }
}

