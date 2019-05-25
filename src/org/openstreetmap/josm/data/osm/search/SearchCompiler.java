// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

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
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.UncheckedParseException;
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
    private static String rxErrorMsg = marktr("The regex \"{0}\" had a parse error at offset {1}, full error:\n\n{2}");
    private static String rxErrorMsgNoPos = marktr("The regex \"{0}\" had a parse error, full error:\n\n{1}");
    private final PushbackTokenizer tokenizer;
    private static Map<String, SimpleMatchFactory> simpleMatchFactoryMap = new HashMap<>();
    private static Map<String, UnaryMatchFactory> unaryMatchFactoryMap = new HashMap<>();
    private static Map<String, BinaryMatchFactory> binaryMatchFactoryMap = new HashMap<>();

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

    public static class CoreSimpleMatchFactory implements SimpleMatchFactory {
        private final Collection<String> keywords = Arrays.asList("id", "version", "type", "user", "role",
                "changeset", "nodes", "ways", "tags", "areasize", "waylength", "modified", "deleted", "selected",
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
                                throw new SearchParseError(tr("Cannot parse timestamp ''{0}''", rangeA1), ex);
                            }
                            try {
                                // if max timestamp is empty: use "now"
                                maxDate = rangeA2.isEmpty() ? System.currentTimeMillis() : DateUtils.fromString(rangeA2).getTime();
                            } catch (UncheckedParseException ex) {
                                throw new SearchParseError(tr("Cannot parse timestamp ''{0}''", rangeA2), ex);
                            }
                            return new TimestampRange(minDate, maxDate);
                        } else {
                            throw new SearchParseError("<html>" + tr("Expecting {0} after {1}", "<i>min</i>/<i>max</i>", "<i>timestamp</i>")
                                + "</html>");
                        }
                    }
                } else {
                    throw new SearchParseError("<html>" + tr("Expecting {0} after {1}", "<code>:</code>", "<i>" + keyword + "</i>") + "</html>");
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
        Match get(String keyword, boolean caseSensitive, boolean regexSearch, PushbackTokenizer tokenizer) throws SearchParseError;
    }

    public interface UnaryMatchFactory extends MatchFactory {
        UnaryMatch get(String keyword, Match matchOperand, PushbackTokenizer tokenizer) throws SearchParseError;
    }

    public interface BinaryMatchFactory extends MatchFactory {
        AbstractBinaryMatch get(String keyword, Match lhs, Match rhs, PushbackTokenizer tokenizer) throws SearchParseError;
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
    }

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
                // StringIndexOutOfBoundsException catched because of https://bugs.openjdk.java.net/browse/JI-9044959
                // See #13870: To remove after we switch to a version of Java which resolves this bug
                throw new SearchParseError(tr(rxErrorMsgNoPos, regex, e.getMessage()), e);
            }
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

        @Override
        public int hashCode() {
            return 31 + ((match == null) ? 0 : match.hashCode());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            UnaryMatch other = (UnaryMatch) obj;
            if (match == null) {
                if (other.match != null)
                    return false;
            } else if (!match.equals(other.match))
                return false;
            return true;
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((lhs == null) ? 0 : lhs.hashCode());
            result = prime * result + ((rhs == null) ? 0 : rhs.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            AbstractBinaryMatch other = (AbstractBinaryMatch) obj;
            if (lhs == null) {
                if (other.lhs != null)
                    return false;
            } else if (!lhs.equals(other.lhs))
                return false;
            if (rhs == null) {
                if (other.rhs != null)
                    return false;
            } else if (!rhs.equals(other.rhs))
                return false;
            return true;
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (defaultValue ? 1231 : 1237);
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            return result;
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
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            return true;
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
    private static class KeyValue extends TaggedMatch {
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (caseSensitive ? 1231 : 1237);
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((keyPattern == null) ? 0 : keyPattern.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            result = prime * result + ((valuePattern == null) ? 0 : valuePattern.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            KeyValue other = (KeyValue) obj;
            if (caseSensitive != other.caseSensitive)
                return false;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (keyPattern == null) {
                if (other.keyPattern != null)
                    return false;
            } else if (!keyPattern.equals(other.keyPattern))
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            if (valuePattern == null) {
                if (other.valuePattern != null)
                    return false;
            } else if (!valuePattern.equals(other.valuePattern))
                return false;
            return true;
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + compareMode;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((referenceNumber == null) ? 0 : referenceNumber.hashCode());
            result = prime * result + ((referenceValue == null) ? 0 : referenceValue.hashCode());
            return result;
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
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (referenceNumber == null) {
                if (other.referenceNumber != null)
                    return false;
            } else if (!referenceNumber.equals(other.referenceNumber))
                return false;
            if (referenceValue == null) {
                if (other.referenceValue != null)
                    return false;
            } else if (!referenceValue.equals(other.referenceValue))
                return false;
            return true;
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
         * @throws SearchParseError if a parse error occurs
         */
        public ExactKeyValue(boolean regexp, String key, String value) throws SearchParseError {
            if ("".equals(key))
                throw new SearchParseError(tr("Key cannot be empty when tag operator is used. Sample use: key=value"));
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
                keyPattern = compilePattern(key, regexFlags(false));
            } else {
                keyPattern = null;
            }
            if (regexp && !this.value.isEmpty() && !"*".equals(this.value)) {
                valuePattern = compilePattern(this.value, regexFlags(false));
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
                return osm.hasTag(key);
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((keyPattern == null) ? 0 : keyPattern.hashCode());
            result = prime * result + ((mode == null) ? 0 : mode.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            result = prime * result + ((valuePattern == null) ? 0 : valuePattern.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            ExactKeyValue other = (ExactKeyValue) obj;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (keyPattern == null) {
                if (other.keyPattern != null)
                    return false;
            } else if (!keyPattern.equals(other.keyPattern))
                return false;
            if (mode != other.mode)
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            if (valuePattern == null) {
                if (other.valuePattern != null)
                    return false;
            } else if (!valuePattern.equals(other.valuePattern))
                return false;
            return true;
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (caseSensitive ? 1231 : 1237);
            result = prime * result + ((search == null) ? 0 : search.hashCode());
            result = prime * result + ((searchRegex == null) ? 0 : searchRegex.hashCode());
            return result;
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
            if (search == null) {
                if (other.search != null)
                    return false;
            } else if (!search.equals(other.search))
                return false;
            if (searchRegex == null) {
                if (other.searchRegex != null)
                    return false;
            } else if (!searchRegex.equals(other.searchRegex))
                return false;
            return true;
        }
    }

    private static class ExactType extends Match {
        private final OsmPrimitiveType type;

        ExactType(String type) throws SearchParseError {
            this.type = OsmPrimitiveType.from(type);
            if (this.type == null)
                throw new SearchParseError(tr("Unknown primitive type: {0}. Allowed values are node, way or relation", type));
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
            if (user == null) {
                if (other.user != null)
                    return false;
            } else if (!user.equals(other.user))
                return false;
            return true;
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
            RoleMatch other = (RoleMatch) obj;
            if (role == null) {
                if (other.role != null)
                    return false;
            } else if (!role.equals(other.role))
                return false;
            return true;
        }
    }

    /**
     * Matches the n-th object of a relation and/or the n-th node of a way.
     */
    private static class Nth extends Match {

        private final int nth;
        private final boolean modulo;

        Nth(PushbackTokenizer tokenizer, boolean modulo) throws SearchParseError {
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (modulo ? 1231 : 1237);
            result = prime * result + nth;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Nth other = (Nth) obj;
            return modulo == other.modulo
                   && nth == other.nth;
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
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (max ^ (max >>> 32));
            result = prime * result + (int) (min ^ (min >>> 32));
            return result;
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
            if (role == null) {
                if (other.role != null)
                    return false;
            } else if (!role.equals(other.role))
                return false;
            return true;
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
            return bounds == null || bounds.isEmpty() ?
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

            if (presetName == null || presetName.isEmpty()) {
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
            if (presets == null) {
                if (other.presets != null)
                    return false;
            } else if (!presets.equals(other.presets))
                return false;
            return true;
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
        } catch (ParseException | IllegalArgumentException e) {
            throw new SearchParseError(tr("Failed to parse MapCSS selector"), e);
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
        Logging.debug("Parsed search expression is {0}", m);
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
                    return factory.get(key, caseSensitive, regexSearch, tokenizer);

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
                    return factory.get(key, caseSensitive, regexSearch, null);

                UnaryMatchFactory unaryFactory = unaryMatchFactoryMap.get(key);
                if (unaryFactory != null)
                    return unaryFactory.get(key, parseFactor(), null);

                // match string in any key or value
                return new Any(key, regexSearch, caseSensitive);
            }
        } else
            return null;
    }

    private Match parseFactor(String errorMessage) throws SearchParseError {
        return Optional.ofNullable(parseFactor()).orElseThrow(() -> new SearchParseError(errorMessage));
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
