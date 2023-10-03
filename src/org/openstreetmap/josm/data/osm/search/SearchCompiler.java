// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.PushbackReader;
import java.io.StringReader;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.PushbackTokenizer.Range;
import org.openstreetmap.josm.data.osm.search.PushbackTokenizer.Token;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSeparator;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Implements a google-like search.
 * <br>
 * Grammar:
 * <pre>
 * expression =
 *   fact | expression
 *   fact expression
 *   fact
 *
 * fact =
 *  ( expression )
 *  -fact
 *  term?
 *  term=term
 *  term:term
 *  term
 *  </pre>
 *
 * @author Imi
 * @since 12656 (moved from actions.search package)
 */
public class SearchCompiler {

    private final boolean caseSensitive;
    private final boolean regexSearch;
    private static final String rxErrorMsg = marktr("The regex \"{0}\" had a parse error at offset {1}, full error:\n\n{2}");
    private static final String rxErrorMsgNoPos = marktr("The regex \"{0}\" had a parse error, full error:\n\n{1}");
    private final PushbackTokenizer tokenizer;
    private static final Map<String, SimpleMatchFactory> simpleMatchFactoryMap = new HashMap<>();
    private static final Map<String, UnaryMatchFactory> unaryMatchFactoryMap = new HashMap<>();
    private static final Map<String, BinaryMatchFactory> binaryMatchFactoryMap = new HashMap<>();

    static {
        addMatchFactory(new CoreSimpleMatchFactory());
        addMatchFactory(new CoreUnaryMatchFactory());
    }

    /**
     * Constructs a new {@code SearchCompiler}.
     * @param caseSensitive {@code true} to perform a case-sensitive search
     * @param regexSearch {@code true} to perform a regex-based search
     * @param tokenizer to split the search string into tokens
     */
    public SearchCompiler(boolean caseSensitive, boolean regexSearch, PushbackTokenizer tokenizer) {
        this.caseSensitive = caseSensitive;
        this.regexSearch = regexSearch;
        this.tokenizer = tokenizer;
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

    /**
     * The core factory for "simple" {@link Match} objects
     */
    public static class CoreSimpleMatchFactory implements SimpleMatchFactory {
        private final Collection<String> keywords = Arrays.asList("id", "version", "type", "user", "role",
                "changeset", "nodes", "ways", "members", "tags", "areasize", "waylength", "modified", "deleted", "selected",
                "incomplete", "untagged", "closed", "new", "indownloadedarea",
                "allindownloadedarea", "timestamp", "nth", "nth%", "hasRole", "preset");

        @Override
        public Match get(String keyword, boolean caseSensitive, boolean regexSearch, PushbackTokenizer tokenizer) throws SearchParseError {
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
            default:
                if (tokenizer != null) {
                    return getTokenizer(keyword, caseSensitive, regexSearch, tokenizer);
                } else {
                    throw new SearchParseError("<html>" + tr("Expecting {0} after {1}", "<code>:</code>", "<i>" + keyword + "</i>") + "</html>");
                }
            }
        }

        private static Match getTokenizer(String keyword, boolean caseSensitive, boolean regexSearch, PushbackTokenizer tokenizer)
                throws SearchParseError {
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
                case "members":
                    return new MemberCountRange(tokenizer);
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
                    String[] rangeA = rangeS.split("/", -1);
                    if (rangeA.length == 1) {
                        return new KeyValue(keyword, rangeS.trim(), regexSearch, caseSensitive);
                    } else if (rangeA.length == 2) {
                        return TimestampRange.create(rangeA);
                    } else {
                        throw new SearchParseError("<html>" + tr("Expecting {0} after {1}", "<i>min</i>/<i>max</i>", "<i>timestamp</i>")
                                + "</html>");
                    }
                default:
                    throw new IllegalStateException("Not expecting keyword " + keyword);
            }
        }

        @Override
        public Collection<String> getKeywords() {
            return keywords;
        }
    }

    /**
     * The core {@link UnaryMatch} factory
     */
    public static class CoreUnaryMatchFactory implements UnaryMatchFactory {
        private static final Collection<String> keywords = Arrays.asList("parent", "child");

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

    /**
     * A factory for getting {@link Match} objects
     */
    public interface SimpleMatchFactory extends MatchFactory {
        /**
         * Get the {@link Match} object
         * @param keyword The keyword to get/create the correct {@link Match} object
         * @param caseSensitive {@code true} if the search is case-sensitive
         * @param regexSearch {@code true} if the search is regex-based
         * @param tokenizer May be used to construct the {@link Match} object
         * @return The {@link Match} object for the keyword and its arguments
         * @throws SearchParseError If the {@link Match} object could not be constructed.
         */
        Match get(String keyword, boolean caseSensitive, boolean regexSearch, PushbackTokenizer tokenizer) throws SearchParseError;
    }

    /**
     * A factory for getting {@link UnaryMatch} objects
     */
    public interface UnaryMatchFactory extends MatchFactory {
        /**
         * Get the {@link UnaryMatch} object
         * @param keyword The keyword to get/create the correct {@link UnaryMatch} object
         * @param matchOperand May be used to construct the {@link UnaryMatch} object
         * @param tokenizer May be used to construct the {@link UnaryMatch} object
         * @return The {@link UnaryMatch} object for the keyword and its arguments
         * @throws SearchParseError If the {@link UnaryMatch} object could not be constructed.
         */
        UnaryMatch get(String keyword, Match matchOperand, PushbackTokenizer tokenizer) throws SearchParseError;
    }

    /**
     * A factor for getting {@link AbstractBinaryMatch} objects
     */
    public interface BinaryMatchFactory extends MatchFactory {
        /**
         * Get the {@link AbstractBinaryMatch} object
         * @param keyword The keyword to get/create the correct {@link AbstractBinaryMatch} object
         * @param lhs May be used to construct the {@link AbstractBinaryMatch} object (see {@link AbstractBinaryMatch#getLhs()})
         * @param rhs May be used to construct the {@link AbstractBinaryMatch} object (see {@link AbstractBinaryMatch#getRhs()})
         * @param tokenizer May be used to construct the {@link AbstractBinaryMatch} object
         * @return The {@link AbstractBinaryMatch} object for the keyword and its arguments
         * @throws SearchParseError If the {@link AbstractBinaryMatch} object could not be constructed.
         */
        AbstractBinaryMatch get(String keyword, Match lhs, Match rhs, PushbackTokenizer tokenizer) throws SearchParseError;
    }

    /**
     * Classes implementing this interface can provide Match instances themselves and do not rely on {@link #compile(String)}.
     *
     * @since 15764
     */
    @FunctionalInterface
    public interface MatchSupplier extends Supplier<Match> {
        @Override
        Match get();
    }

    /**
     * Base class for all search criteria. If the criterion only depends on an object's tags,
     * inherit from {@link org.openstreetmap.josm.data.osm.search.SearchCompiler.TaggedMatch}.
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
            return tagged instanceof OsmPrimitive && match((OsmPrimitive) tagged);
        }

        @Override
        public final boolean test(OsmPrimitive object) {
            return match(object);
        }

        /**
         * Check if this is a valid match object
         * @return {@code this}, for easy chaining
         * @throws SearchParseError If the match is not valid
         */
        public Match validate() throws SearchParseError {
            // Default to no-op
            return this;
        }
    }

    /**
     * A common subclass of {@link Match} for matching against tags
     */
    public abstract static class TaggedMatch extends Match {

        @Override
        public abstract boolean match(Tagged tags);

        @Override
        public final boolean match(OsmPrimitive osm) {
            return match((Tagged) osm);
        }

        protected static Pattern compilePattern(String regex, int flags) throws SearchParseError {
            try {
                return Pattern.compile(regex, flags);
            } catch (PatternSyntaxException e) {
                throw new SearchParseError(tr(rxErrorMsg, e.getPattern(), e.getIndex(), e.getMessage()), e);
            } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                // StringIndexOutOfBoundsException caught because of https://bugs.openjdk.java.net/browse/JI-9044959
                // See #13870: To remove after we switch to a version of Java which resolves this bug
                throw new SearchParseError(tr(rxErrorMsgNoPos, regex, e.getMessage()), e);
            }
        }
    }

    /**
     * A unary search operator which may take data parameters.
     */
    public abstract static class UnaryMatch extends Match {
        @Nonnull
        protected final Match match;

        protected UnaryMatch(@Nullable Match match) {
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

        @Override
        public int hashCode() {
            return 31 + match.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            UnaryMatch other = (UnaryMatch) obj;
            return match.equals(other.match);
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
        protected AbstractBinaryMatch(Match lhs, Match rhs) {
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

        /**
         * First applies {@code mapper} to both sides and then applies {@code operator} on the two results.
         * @param mapper the mapping function
         * @param operator the operator
         * @param <T> the type of the intermediate result
         * @param <U> the type of the result
         * @return {@code operator.apply(mapper.apply(lhs), mapper.apply(rhs))}
         */
        public <T, U> U map(Function<Match, T> mapper, BiFunction<T, T, U> operator) {
            return operator.apply(mapper.apply(lhs), mapper.apply(rhs));
        }

        protected static String parenthesis(Match m) {
            return '(' + m.toString() + ')';
        }

        @Override
        public int hashCode() {
            return Objects.hash(lhs, rhs);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            AbstractBinaryMatch other = (AbstractBinaryMatch) obj;
            return Objects.equals(lhs, other.lhs) && Objects.equals(rhs, other.rhs);
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
    public static class BooleanMatch extends TaggedMatch {
        private final String key;
        private final boolean defaultValue;

        BooleanMatch(String key, boolean defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String getKey() {
            return key;
        }

        @Override
        public boolean match(Tagged osm) {
            return Optional.ofNullable(OsmUtils.getOsmBoolean(osm.get(key))).orElse(defaultValue);
        }

        @Override
        public String toString() {
            return key + '?';
        }

        @Override
        public int hashCode() {
            return Objects.hash(defaultValue, key);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            BooleanMatch other = (BooleanMatch) obj;
            if (defaultValue != other.defaultValue)
                return false;
            return Objects.equals(key, other.key);
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
            return map(m -> m instanceof AbstractBinaryMatch && !(m instanceof And) ? parenthesis(m) : m, (s1, s2) -> s1 + " && " + s2);
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
            return map(m -> m instanceof AbstractBinaryMatch && !(m instanceof Or) ? parenthesis(m) : m, (s1, s2) -> s1 + " || " + s2);
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
            return map(m -> m instanceof AbstractBinaryMatch && !(m instanceof Xor) ? parenthesis(m) : m, (s1, s2) -> s1 + " ^ " + s2);
        }
    }

    /**
     * Matches objects with ID in the given range.
     */
    private static class Id extends RangeMatch {
        Id(Range range) {
            super(range);
        }

        Id(PushbackTokenizer tokenizer) throws SearchParseError {
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

        ChangesetId(PushbackTokenizer tokenizer) throws SearchParseError {
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

        Version(PushbackTokenizer tokenizer) throws SearchParseError {
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
    public static class KeyValue extends TaggedMatch {
        private final String key;
        private final Pattern keyPattern;
        private final String value;
        private final Pattern valuePattern;
        private final boolean caseSensitive;

        KeyValue(String key, String value, boolean regexSearch, boolean caseSensitive) throws SearchParseError {
            this.caseSensitive = caseSensitive;
            if (regexSearch) {
                int searchFlags = regexFlags(caseSensitive);
                this.keyPattern = compilePattern(key, searchFlags);
                this.valuePattern = compilePattern(value, searchFlags);
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
                if (osm.hasKeys()) {
                    // The string search will just get a key like 'highway' and look that up as osm.get(key).
                    // But since we're doing a regex match we'll have to loop over all the keys to see if they match our regex,
                    // and only then try to match against the value
                    return osm.keys()
                            .anyMatch(k -> keyPattern.matcher(k).find() && valuePattern.matcher(osm.get(k)).find());
                }
            } else {
                String mv = getMv(osm);
                if (mv != null) {
                    String v1 = Normalizer.normalize(caseSensitive ? mv : mv.toLowerCase(Locale.ENGLISH), Normalizer.Form.NFC);
                    String v2 = Normalizer.normalize(caseSensitive ? value : value.toLowerCase(Locale.ENGLISH), Normalizer.Form.NFC);
                    return v1.contains(v2);
                }
            }
            return false;
        }

        private String getMv(Tagged osm) {
            String mv;
            if ("timestamp".equals(key) && osm instanceof OsmPrimitive) {
                mv = ((OsmPrimitive) osm).getInstant().toString();
            } else {
                mv = osm.get(key);
                if (!caseSensitive && mv == null) {
                    mv = osm.keys().filter(key::equalsIgnoreCase).findFirst().map(osm::get).orElse(null);
                }
            }
            return mv;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return key + '=' + value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(caseSensitive, key, keyPattern, value, valuePattern);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            KeyValue other = (KeyValue) obj;
            return caseSensitive == other.caseSensitive
                    && Objects.equals(key, other.key)
                    && Objects.equals(keyPattern, other.keyPattern)
                    && Objects.equals(value, other.value)
                    && Objects.equals(valuePattern, other.valuePattern);
        }
    }

    /**
     * Match a primitive based off of a value comparison. This currently supports:
     * <ul>
     *     <li>ISO8601 dates (YYYY-MM-DD)</li>
     *     <li>Numbers</li>
     *     <li>Alpha-numeric comparison</li>
     * </ul>
     */
    public static class ValueComparison extends TaggedMatch {
        private final String key;
        private final String referenceValue;
        private final Double referenceNumber;
        private final int compareMode;
        private static final Pattern ISO8601 = Pattern.compile("\\d+-\\d+-\\d+");

        /**
         * Create a new {@link ValueComparison} object
         * @param key The key to get the value from
         * @param referenceValue The value to compare to
         * @param compareMode The compare mode to use; {@code < 0} is {@code currentValue < referenceValue} and
         *                    {@code > 0} is {@code currentValue > referenceValue}. {@code 0} is effectively an equality check.
         */
        public ValueComparison(String key, String referenceValue, int compareMode) {
            this.key = key;
            this.referenceValue = referenceValue;
            Double v = null;
            try {
                if (referenceValue != null) {
                    v = Double.valueOf(referenceValue);
                }
            } catch (NumberFormatException numberFormatException) {
                Logging.trace(numberFormatException);
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

        @Override
        public int hashCode() {
            return Objects.hash(compareMode, key, referenceNumber, referenceValue);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            ValueComparison other = (ValueComparison) obj;
            if (compareMode != other.compareMode)
                return false;
            return Objects.equals(key, other.key)
                    && Objects.equals(referenceNumber, other.referenceNumber)
                    && Objects.equals(referenceValue, other.referenceValue);
        }

        @Override
        public Match validate() throws SearchParseError {
            if (this.referenceValue == null) {
                final String referenceType;
                if (this.compareMode == +1) {
                    referenceType = ">";
                } else if (this.compareMode == -1) {
                    referenceType = "<";
                } else {
                    referenceType = "<unknown>";
                }
                throw new SearchParseError(tr("Reference value for ''{0}'' expected", referenceType));
            }
            return this;
        }
    }

    /**
     * Matches objects with the exact given key-value pair.
     */
    public static class ExactKeyValue extends TaggedMatch {

        /**
         * The mode to use for the comparison
         */
        public enum Mode {
            /** Matches everything */
            ANY,
            /** Any key with the specified value will match */
            ANY_KEY,
            /** Any value with the specified key will match */
            ANY_VALUE,
            /** A key with the specified value will match */
            EXACT,
            /** Nothing matches */
            NONE,
            /** The key does not exist */
            MISSING_KEY,
            /** Similar to {@link #ANY_KEY}, but the value matches a regex */
            ANY_KEY_REGEXP,
            /** Similar to {@link #ANY_VALUE}, but the key matches a regex */
            ANY_VALUE_REGEXP,
            /** Both the key and the value matches their respective regex */
            EXACT_REGEXP,
            /** No key matching the regex exists */
            MISSING_KEY_REGEXP
        }

        private final String key;
        private final String value;
        private final Pattern keyPattern;
        private final Pattern valuePattern;
        private final Mode mode;

        /**
         * Constructs a new {@code ExactKeyValue}.
         * @param regexp regular expression
         * @param caseSensitive {@code true} to perform a case-sensitive search
         * @param key key
         * @param value value
         * @throws SearchParseError if a parse error occurs
         */
        public ExactKeyValue(boolean regexp, boolean caseSensitive, String key, String value) throws SearchParseError {
            if ("".equals(key))
                throw new SearchParseError(tr("Key cannot be empty when tag operator is used. Sample use: key=value"));
            this.key = key;
            this.value = value == null ? "" : value;
            if (this.value.isEmpty() && "*".equals(key)) {
                mode = Mode.NONE;
            } else if (this.value.isEmpty()) {
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
                keyPattern = compilePattern(key, regexFlags(caseSensitive));
            } else {
                keyPattern = null;
            }
            if (regexp && !this.value.isEmpty() && !"*".equals(this.value)) {
                valuePattern = compilePattern(this.value, regexFlags(caseSensitive));
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
                return !osm.hasTag(key);
            case ANY:
                return true;
            case ANY_VALUE:
                return osm.hasKey(key);
            case ANY_KEY:
                return osm.getKeys().values().stream().anyMatch(v -> v.equals(value));
            case EXACT:
                return value.equals(osm.get(key));
            case ANY_KEY_REGEXP:
                return osm.getKeys().values().stream().anyMatch(v -> valuePattern.matcher(v).matches());
            case ANY_VALUE_REGEXP:
            case EXACT_REGEXP:
                return osm.keys().anyMatch(k -> keyPattern.matcher(k).matches()
                        && (mode == Mode.ANY_VALUE_REGEXP || valuePattern.matcher(osm.get(k)).matches()));
            case MISSING_KEY_REGEXP:
                return osm.keys().noneMatch(k -> keyPattern.matcher(k).matches());
            }
            throw new AssertionError("Missed state");
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public Mode getMode() {
            return mode;
        }

        @Override
        public String toString() {
            return key + '=' + value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, keyPattern, mode, value, valuePattern);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            ExactKeyValue other = (ExactKeyValue) obj;
            if (mode != other.mode)
                return false;
            return Objects.equals(key, other.key)
                    && Objects.equals(value, other.value)
                    && Objects.equals(keyPattern, other.keyPattern)
                    && Objects.equals(valuePattern, other.valuePattern);
        }
    }

    /**
     * Match a string in any tags (key or value), with optional regex and case insensitivity.
     */
    private static class Any extends TaggedMatch {
        private final String search;
        private final Pattern searchRegex;
        private final boolean caseSensitive;

        Any(String s, boolean regexSearch, boolean caseSensitive) throws SearchParseError {
            s = Normalizer.normalize(s, Normalizer.Form.NFC);
            this.caseSensitive = caseSensitive;
            if (regexSearch) {
                this.searchRegex = compilePattern(s, regexFlags(caseSensitive));
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

            for (Map.Entry<String, String> entry: osm.getKeys().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
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

                    if (key.contains(search) || value.contains(search))
                        return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return search;
        }

        @Override
        public int hashCode() {
            return Objects.hash(caseSensitive, search, searchRegex);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Any other = (Any) obj;
            if (caseSensitive != other.caseSensitive)
                return false;
            return Objects.equals(search, other.search)
                    && Objects.equals(searchRegex, other.searchRegex);
        }
    }

    /**
     * Filter OsmPrimitives based off of the base primitive type
     */
    public static class ExactType extends Match {
        private final OsmPrimitiveType type;

        ExactType(String type) throws SearchParseError {
            this.type = OsmPrimitiveType.from(type);
            if (this.type == null)
                throw new SearchParseError(tr("Unknown primitive type: {0}. Allowed values are node, way or relation", type));
        }

        public OsmPrimitiveType getType() {
            return type;
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return type == osm.getType();
        }

        @Override
        public String toString() {
            return "type=" + type;
        }

        @Override
        public int hashCode() {
            return 31 + ((type == null) ? 0 : type.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            ExactType other = (ExactType) obj;
            return type == other.type;
        }
    }

    /**
     * Matches objects last changed by the given username.
     */
    public static class UserMatch extends Match {
        private final String user;

        UserMatch(String user) {
            if ("anonymous".equals(user)) {
                this.user = null;
            } else {
                this.user = user;
            }
        }

        public String getUser() {
            return user;
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

        @Override
        public int hashCode() {
            return 31 + ((user == null) ? 0 : user.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            UserMatch other = (UserMatch) obj;
            return Objects.equals(user, other.user);
        }
    }

    /**
     * Matches objects with the given relation role (i.e. "outer").
     */
    private static class RoleMatch extends Match {
        @Nonnull
        private final String role;

        RoleMatch(String role) {
            if (role == null) {
                this.role = "";
            } else {
                this.role = role;
            }
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return osm.referrers(Relation.class)
                    .filter(ref -> !ref.isIncomplete() && !ref.isDeleted())
                    .flatMap(ref -> ref.getMembers().stream()).filter(m -> m.getMember() == osm)
                    .map(RelationMember::getRole)
                    .anyMatch(testRole -> role.equals(testRole == null ? "" : testRole));
        }

        @Override
        public String toString() {
            return "role=" + role;
        }

        @Override
        public int hashCode() {
            return 31 + role.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            RoleMatch other = (RoleMatch) obj;
            return role.equals(other.role);
        }
    }

    /**
     * Matches the n-th object of a relation and/or the n-th node of a way.
     */
    private static class Nth extends Match {

        private final int nthObject;
        private final boolean modulo;

        Nth(PushbackTokenizer tokenizer, boolean modulo) throws SearchParseError {
            this((int) tokenizer.readNumber(tr("Positive integer expected")), modulo);
        }

        private Nth(int nth, boolean modulo) throws SearchParseError {
            this.nthObject = nth;
            this.modulo = modulo;
            if (this.modulo && this.nthObject == 0) {
                throw new SearchParseError(tr("Non-zero integer expected"));
            }
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
                if (nthObject < 0 && idx - maxIndex == nthObject) {
                    return true;
                } else if (idx == nthObject || (modulo && idx % nthObject == 0))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "Nth{nth=" + nthObject + ", modulo=" + modulo + '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(modulo, nthObject);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Nth other = (Nth) obj;
            return modulo == other.modulo
                   && nthObject == other.nthObject;
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

        @Override
        public int hashCode() {
            return Objects.hash(max, min);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            RangeMatch other = (RangeMatch) obj;
            return max == other.max
                && min == other.min;
        }
    }

    /**
     * Matches ways with a number of nodes in given range
     */
    private static class NodeCountRange extends RangeMatch {
        NodeCountRange(Range range) {
            super(range);
        }

        NodeCountRange(PushbackTokenizer tokenizer) throws SearchParseError {
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

        WayCountRange(PushbackTokenizer tokenizer) throws SearchParseError {
            this(tokenizer.readRange(tr("Range of numbers expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            if (osm instanceof Node) {
                return osm.referrers(Way.class).count();
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

    /*
     * Matches relations with a certain number of members
     */
    private static class MemberCountRange extends RangeMatch {
        MemberCountRange(Range range) {
            super(range);
        }

        MemberCountRange(PushbackTokenizer tokenizer) throws SearchParseError {
            this(tokenizer.readRange(tr("Range of numbers expected")));
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            if (osm instanceof Relation) {
                Relation r = (Relation) osm;
                return (long) r.getMembersCount();
            } else {
                return null;
            }
        }

        @Override
        protected String getString() {
            return "members";
        }
    }

    /**
     * Matches objects with a number of tags in given range
     */
    private static class TagCountRange extends RangeMatch {
        TagCountRange(Range range) {
            super(range);
        }

        TagCountRange(PushbackTokenizer tokenizer) throws SearchParseError {
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

        private static TimestampRange create(String[] range) throws SearchParseError {
            CheckParameterUtil.ensureThat(range.length == 2, "length 2");
            String rangeA1 = range[0].trim();
            String rangeA2 = range[1].trim();
            final long minDate;
            final long maxDate;
            try {
                // if min timestamp is empty: use the lowest possible date
                minDate = DateUtils.parseInstant(rangeA1.isEmpty() ? "1980" : rangeA1).toEpochMilli();
            } catch (UncheckedParseException | DateTimeException ex) {
                throw new SearchParseError(tr("Cannot parse timestamp ''{0}''", rangeA1), ex);
            }
            try {
                // if max timestamp is empty: use "now"
                maxDate = rangeA2.isEmpty() ? System.currentTimeMillis() : DateUtils.parseInstant(rangeA2).toEpochMilli();
            } catch (UncheckedParseException | DateTimeException ex) {
                throw new SearchParseError(tr("Cannot parse timestamp ''{0}''", rangeA2), ex);
            }
            return new TimestampRange(minDate, maxDate);
        }

        @Override
        protected Long getNumber(OsmPrimitive osm) {
            return osm.getInstant().toEpochMilli();
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

        @Override
        public int hashCode() {
            return 31 + ((role == null) ? 0 : role.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            HasRole other = (HasRole) obj;
            return Objects.equals(role, other.role);
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
     * Typically, some members of a relation are incomplete until they are
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
     * fixme, etc.). The complete list of uninteresting tags can be found here:
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
            if (osm instanceof Way) {
                return ((Way) osm).getNodes().stream().anyMatch(match::match);
            } else if (osm instanceof Relation) {
                return ((Relation) osm).getMembers().stream().anyMatch(member -> match.match(member.getMember()));
            } else {
                return false;
            }
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
            return osm.getReferrers().stream().anyMatch(match::match);
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

        AreaSize(PushbackTokenizer tokenizer) throws SearchParseError {
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

        WayLength(PushbackTokenizer tokenizer) throws SearchParseError {
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
    public abstract static class InArea extends Match {

        protected final boolean all;

        /**
         * @param all if true, all way nodes or relation members have to be within source area;if false, one suffices.
         */
        protected InArea(boolean all) {
            this.all = all;
        }

        protected abstract Collection<Bounds> getBounds(OsmPrimitive primitive);

        @Override
        public boolean match(OsmPrimitive osm) {
            if (!osm.isUsable())
                return false;
            else if (osm instanceof Node) {
                Collection<Bounds> allBounds = getBounds(osm);
                return ((Node) osm).isLatLonKnown() && allBounds != null && allBounds.stream().anyMatch(bounds -> bounds.contains((Node) osm));
            } else if (osm instanceof Way) {
                Collection<Node> nodes = ((Way) osm).getNodes();
                return all ? nodes.stream().allMatch(this) : nodes.stream().anyMatch(this);
            } else if (osm instanceof Relation) {
                Collection<OsmPrimitive> primitives = ((Relation) osm).getMemberPrimitivesList();
                return all ? primitives.stream().allMatch(this) : primitives.stream().anyMatch(this);
            } else
                return false;
        }

        @Override
        public int hashCode() {
            return 31 + (all ? 1231 : 1237);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            InArea other = (InArea) obj;
            return all == other.all;
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
     * Unlike {@link InDataSourceArea}, this matches also if no source area is set (e.g., for new layers).
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
            return Utils.isEmpty(bounds) ?
                    Collections.singleton(ProjectionRegistry.getProjection().getWorldBoundsLatLon()) : bounds;
        }

        @Override
        public String toString() {
            return "NotOutsideDataSourceArea";
        }
    }

    /**
     * Matches presets.
     * @since 12464
     */
    private static class Preset extends Match {
        private final List<TaggingPreset> presets;

        Preset(String presetName) throws SearchParseError {

            if (Utils.isEmpty(presetName)) {
                throw new SearchParseError("The name of the preset is required");
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
                throw new SearchParseError(tr("Unknown preset name: ") + presetName);
            }
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return this.presets.stream().anyMatch(preset -> preset.test(osm));
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
                Logging.trace(ex);
                return false;
            }
        }

        @Override
        public int hashCode() {
            return 31 + ((presets == null) ? 0 : presets.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Preset other = (Preset) obj;
            return Objects.equals(presets, other.presets);
        }
    }

    /**
     * Compiles the search expression.
     * @param searchStr the search expression
     * @return a {@link Match} object for the expression
     * @throws SearchParseError if an error has been encountered while compiling
     * @see #compile(SearchSetting)
     */
    public static Match compile(String searchStr) throws SearchParseError {
        return new SearchCompiler(false, false,
                new PushbackTokenizer(
                        new PushbackReader(new StringReader(searchStr))))
                .parse();
    }

    /**
     * Compiles the search expression.
     * @param setting the settings to use
     * @return a {@link Match} object for the expression
     * @throws SearchParseError if an error has been encountered while compiling
     * @see #compile(String)
     */
    public static Match compile(SearchSetting setting) throws SearchParseError {
        if (setting instanceof MatchSupplier) {
            return ((MatchSupplier) setting).get();
        }
        if (setting.mapCSSSearch) {
            return compileMapCSS(setting.text);
        }
        return new SearchCompiler(setting.caseSensitive, setting.regexSearch,
                new PushbackTokenizer(
                        new PushbackReader(new StringReader(setting.text))))
                .parse();
    }

    static Match compileMapCSS(String mapCSS) throws SearchParseError {
        try {
            final List<Selector> selectors = new MapCSSParser(new StringReader(mapCSS)).selectors_for_search();
            return new MapCSSMatch(selectors);
        } catch (ParseException | IllegalArgumentException e) {
            throw new SearchParseError(tr("Failed to parse MapCSS selector"), e);
        }
    }

    private static class MapCSSMatch extends Match {
        private final List<Selector> selectors;

        MapCSSMatch(List<Selector> selectors) {
            this.selectors = selectors;
        }

        @Override
        public boolean match(OsmPrimitive osm) {
            return selectors.stream()
                    .anyMatch(selector -> selector.matches(new Environment(osm)));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapCSSMatch that = (MapCSSMatch) o;
            return Objects.equals(selectors, that.selectors);
        }

        @Override
        public int hashCode() {
            return Objects.hash(selectors);
        }
    }

    /**
     * Parse search string.
     *
     * @return match determined by search string
     * @throws org.openstreetmap.josm.data.osm.search.SearchParseError if search expression cannot be parsed
     */
    public Match parse() throws SearchParseError {
        Match m = Optional.ofNullable(parseExpression()).orElse(Always.INSTANCE);
        if (!tokenizer.readIfEqual(Token.EOF))
            throw new SearchParseError(tr("Unexpected token: {0}", tokenizer.nextToken()));
        Logging.trace("Parsed search expression is {0}", m);
        return m;
    }

    /**
     * Parse expression.
     *
     * @return match determined by parsing expression
     * @throws SearchParseError if search expression cannot be parsed
     */
    private Match parseExpression() throws SearchParseError {
        // Step 1: parse the whole expression and build a list of factors and logical tokens
        List<Object> list = parseExpressionStep1();
        // Step 2: iterate the list in reverse order to build the logical expression
        // This iterative approach avoids StackOverflowError for long expressions (see #14217)
        return parseExpressionStep2(list);
    }

    private List<Object> parseExpressionStep1() throws SearchParseError {
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
                throw new SearchParseError(errorMessage);
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
     * @throws SearchParseError if search expression cannot be parsed
     */
    private Match parseFactor() throws SearchParseError {
        if (tokenizer.readIfEqual(Token.LEFT_PARENT)) {
            Match expression = parseExpression();
            if (!tokenizer.readIfEqual(Token.RIGHT_PARENT))
                throw new SearchParseError(Token.RIGHT_PARENT, tokenizer.nextToken());
            return expression != null ? expression : Always.INSTANCE;
        } else if (tokenizer.readIfEqual(Token.NOT)) {
            return new Not(parseFactor(tr("Missing operator for NOT")));
        } else if (tokenizer.readIfEqual(Token.KEY)) {
            // factor consists of key:value or key=value
            String key = tokenizer.getText();
            if (tokenizer.readIfEqual(Token.EQUALS)) {
                return new ExactKeyValue(regexSearch, caseSensitive, key, tokenizer.readTextOrNumber()).validate();
            } else if (tokenizer.readIfEqual(Token.TILDE)) {
                return new ExactKeyValue(true, caseSensitive, key, tokenizer.readTextOrNumber()).validate();
            } else if (tokenizer.readIfEqual(Token.LESS_THAN)) {
                return new ValueComparison(key, tokenizer.readTextOrNumber(), -1).validate();
            } else if (tokenizer.readIfEqual(Token.GREATER_THAN)) {
                return new ValueComparison(key, tokenizer.readTextOrNumber(), +1).validate();
            } else if (tokenizer.readIfEqual(Token.COLON)) {
                // see if we have a Match that takes a data parameter
                SimpleMatchFactory factory = simpleMatchFactoryMap.get(key);
                if (factory != null)
                    return factory.get(key, caseSensitive, regexSearch, tokenizer);

                UnaryMatchFactory unaryFactory = unaryMatchFactoryMap.get(key);
                if (unaryFactory != null)
                    return getValidate(unaryFactory, key, tokenizer);

                // key:value form where value is a string (may be OSM key search)
                final String value = tokenizer.readTextOrNumber();
                if (value == null) {
                    return new ExactKeyValue(regexSearch, caseSensitive, key, "*").validate();
                }
                return new KeyValue(key, value, regexSearch, caseSensitive).validate();
            } else if (tokenizer.readIfEqual(Token.QUESTION_MARK))
                return new BooleanMatch(key, false);
            else {
                SimpleMatchFactory factory = simpleMatchFactoryMap.get(key);
                if (factory != null)
                    return factory.get(key, caseSensitive, regexSearch, null).validate();

                UnaryMatchFactory unaryFactory = unaryMatchFactoryMap.get(key);
                if (unaryFactory != null)
                    return getValidate(unaryFactory, key, null);

                // match string in any key or value
                return new Any(key, regexSearch, caseSensitive).validate();
            }
        } else
            return null;
    }

    private Match parseFactor(String errorMessage) throws SearchParseError {
        return Optional.ofNullable(parseFactor()).orElseThrow(() -> new SearchParseError(errorMessage));
    }

    private Match getValidate(UnaryMatchFactory unaryFactory, String key, PushbackTokenizer tokenizer)
            throws SearchParseError {
        UnaryMatch match = unaryFactory.get(key, parseFactor(), tokenizer);
        return match != null ? match.validate() : null;
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
        if (Utils.isEmpty(value)) {
            return forKey + '*';
        } else {
            return forKey + '"' + escapeStringForSearch(value) + '"';
        }
    }
}
