// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleIndex;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.TokenMgrError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.FileWatcher;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Stopwatch;
import org.openstreetmap.josm.tools.Utils;

/**
 * MapCSS-based tag checker/fixer.
 * @since 6506
 */
public class MapCSSTagChecker extends Test.TagTest {
    private MapCSSStyleIndex indexData;
    private final Map<MapCSSRule, MapCSSTagCheckerAndRule> ruleToCheckMap = new HashMap<>();
    private static final Map<IPrimitive, Area> mpAreaCache = new HashMap<>();
    static final boolean ALL_TESTS = true;
    static final boolean ONLY_SELECTED_TESTS = false;

    /**
     * The preference key for tag checker source entries.
     * @since 6670
     */
    public static final String ENTRIES_PREF_KEY = "validator." + MapCSSTagChecker.class.getName() + ".entries";

    /**
     * Constructs a new {@code MapCSSTagChecker}.
     */
    public MapCSSTagChecker() {
        super(tr("Tag checker (MapCSS based)"), tr("This test checks for errors in tag keys and values."));
    }

    /**
     * Represents a fix to a validation test. The fixing {@link Command} can be obtained by {@link #createCommand(OsmPrimitive, Selector)}.
     */
    @FunctionalInterface
    interface FixCommand {
        /**
         * Creates the fixing {@link Command} for the given primitive. The {@code matchingSelector} is used to evaluate placeholders
         * (cf. {@link MapCSSTagChecker.TagCheck#insertArguments(Selector, String, OsmPrimitive)}).
         * @param p OSM primitive
         * @param matchingSelector  matching selector
         * @return fix command
         */
        Command createCommand(OsmPrimitive p, Selector matchingSelector);

        /**
         * Checks that object is either an {@link Expression} or a {@link String}.
         * @param obj object to check
         * @throws IllegalArgumentException if object is not an {@code Expression} or a {@code String}
         */
        static void checkObject(final Object obj) {
            CheckParameterUtil.ensureThat(obj instanceof Expression || obj instanceof String,
                    () -> "instance of Exception or String expected, but got " + obj);
        }

        /**
         * Evaluates given object as {@link Expression} or {@link String} on the matched {@link OsmPrimitive} and {@code matchingSelector}.
         * @param obj object to evaluate ({@link Expression} or {@link String})
         * @param p OSM primitive
         * @param matchingSelector matching selector
         * @return result string
         */
        static String evaluateObject(final Object obj, final OsmPrimitive p, final Selector matchingSelector) {
            final String s;
            if (obj instanceof Expression) {
                s = (String) ((Expression) obj).evaluate(new Environment(p));
            } else if (obj instanceof String) {
                s = (String) obj;
            } else {
                return null;
            }
            return TagCheck.insertArguments(matchingSelector, s, p);
        }

        /**
         * Creates a fixing command which executes a {@link ChangePropertyCommand} on the specified tag.
         * @param obj object to evaluate ({@link Expression} or {@link String})
         * @return created fix command
         */
        static FixCommand fixAdd(final Object obj) {
            checkObject(obj);
            return new FixCommand() {
                @Override
                public Command createCommand(OsmPrimitive p, Selector matchingSelector) {
                    final Tag tag = Tag.ofString(FixCommand.evaluateObject(obj, p, matchingSelector));
                    return new ChangePropertyCommand(p, tag.getKey(), tag.getValue());
                }

                @Override
                public String toString() {
                    return "fixAdd: " + obj;
                }
            };
        }

        /**
         * Creates a fixing command which executes a {@link ChangePropertyCommand} to delete the specified key.
         * @param obj object to evaluate ({@link Expression} or {@link String})
         * @return created fix command
         */
        static FixCommand fixRemove(final Object obj) {
            checkObject(obj);
            return new FixCommand() {
                @Override
                public Command createCommand(OsmPrimitive p, Selector matchingSelector) {
                    final String key = FixCommand.evaluateObject(obj, p, matchingSelector);
                    return new ChangePropertyCommand(p, key, "");
                }

                @Override
                public String toString() {
                    return "fixRemove: " + obj;
                }
            };
        }

        /**
         * Creates a fixing command which executes a {@link ChangePropertyKeyCommand} on the specified keys.
         * @param oldKey old key
         * @param newKey new key
         * @return created fix command
         */
        static FixCommand fixChangeKey(final String oldKey, final String newKey) {
            return new FixCommand() {
                @Override
                public Command createCommand(OsmPrimitive p, Selector matchingSelector) {
                    return new ChangePropertyKeyCommand(p,
                            TagCheck.insertArguments(matchingSelector, oldKey, p),
                            TagCheck.insertArguments(matchingSelector, newKey, p));
                }

                @Override
                public String toString() {
                    return "fixChangeKey: " + oldKey + " => " + newKey;
                }
            };
        }
    }

    final MultiMap<String, TagCheck> checks = new MultiMap<>();

    /** maps the source URL for a test to the title shown in the dialog where known */
    private final Map<String, String> urlTitles = new HashMap<>();

    /**
     * Result of {@link TagCheck#readMapCSS}
     * @since 8936
     */
    public static class ParseResult {
        /** Checks successfully parsed */
        public final List<TagCheck> parseChecks;
        /** Errors that occurred during parsing */
        public final Collection<Throwable> parseErrors;

        /**
         * Constructs a new {@code ParseResult}.
         * @param parseChecks Checks successfully parsed
         * @param parseErrors Errors that occurred during parsing
         */
        public ParseResult(List<TagCheck> parseChecks, Collection<Throwable> parseErrors) {
            this.parseChecks = parseChecks;
            this.parseErrors = parseErrors;
        }
    }

    /**
     * Tag check.
     */
    public static class TagCheck implements Predicate<OsmPrimitive> {
        /** The selector of this {@code TagCheck} */
        protected final MapCSSRule rule;
        /** Commands to apply in order to fix a matching primitive */
        protected final List<FixCommand> fixCommands;
        /** Tags (or arbitrary strings) of alternatives to be presented to the user */
        protected final List<String> alternatives;
        /** An {@link org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.AssignmentInstruction}-{@link Severity} pair.
         * Is evaluated on the matching primitive to give the error message. Map is checked to contain exactly one element. */
        protected final Map<Instruction.AssignmentInstruction, Severity> errors;
        /** MapCSS Classes to set on matching primitives */
        protected final Collection<String> setClassExpressions;
        /** Denotes whether the object should be deleted for fixing it */
        protected boolean deletion;
        /** A string used to group similar tests */
        protected String group;

        TagCheck(MapCSSRule rule) {
            this.rule = rule;
            this.fixCommands = new ArrayList<>();
            this.alternatives = new ArrayList<>();
            this.errors = new HashMap<>();
            this.setClassExpressions = new HashSet<>();
        }

        TagCheck(TagCheck check) {
            this.rule = check.rule;
            this.fixCommands = Utils.toUnmodifiableList(check.fixCommands);
            this.alternatives = Utils.toUnmodifiableList(check.alternatives);
            this.errors = Utils.toUnmodifiableMap(check.errors);
            this.setClassExpressions = Utils.toUnmodifiableList(check.setClassExpressions);
            this.deletion = check.deletion;
            this.group = check.group;
        }

        TagCheck toImmutable() {
            return new TagCheck(this);
        }

        private static final String POSSIBLE_THROWS = "throwError/throwWarning/throwOther";

        static TagCheck ofMapCSSRule(final MapCSSRule rule, AssertionConsumer assertionConsumer) throws IllegalDataException {
            final TagCheck check = new TagCheck(rule);
            final Map<String, Boolean> assertions = new HashMap<>();
            for (Instruction i : rule.declaration.instructions) {
                if (i instanceof Instruction.AssignmentInstruction) {
                    final Instruction.AssignmentInstruction ai = (Instruction.AssignmentInstruction) i;
                    if (ai.isSetInstruction) {
                        check.setClassExpressions.add(ai.key);
                        continue;
                    }
                    try {
                        final String val = ai.val instanceof Expression
                                ? Optional.ofNullable(((Expression) ai.val).evaluate(new Environment()))
                                        .map(Object::toString).map(String::intern).orElse(null)
                                : ai.val instanceof String
                                ? (String) ai.val
                                : ai.val instanceof Keyword
                                ? ((Keyword) ai.val).val
                                : null;
                        if ("throwError".equals(ai.key)) {
                            check.errors.put(ai, Severity.ERROR);
                        } else if ("throwWarning".equals(ai.key)) {
                            check.errors.put(ai, Severity.WARNING);
                        } else if ("throwOther".equals(ai.key)) {
                            check.errors.put(ai, Severity.OTHER);
                        } else if (ai.key.startsWith("throw")) {
                            Logging.log(Logging.LEVEL_WARN,
                                    "Unsupported " + ai.key + " instruction. Allowed instructions are " + POSSIBLE_THROWS + '.', null);
                        } else if ("fixAdd".equals(ai.key)) {
                            check.fixCommands.add(FixCommand.fixAdd(ai.val));
                        } else if ("fixRemove".equals(ai.key)) {
                            CheckParameterUtil.ensureThat(!(ai.val instanceof String) || !(val != null && val.contains("=")),
                                    "Unexpected '='. Please only specify the key to remove in: " + ai);
                            check.fixCommands.add(FixCommand.fixRemove(ai.val));
                        } else if (val != null && "fixChangeKey".equals(ai.key)) {
                            CheckParameterUtil.ensureThat(val.contains("=>"), "Separate old from new key by '=>'!");
                            final String[] x = val.split("=>", 2);
                            check.fixCommands.add(FixCommand.fixChangeKey(Utils.removeWhiteSpaces(x[0]), Utils.removeWhiteSpaces(x[1])));
                        } else if (val != null && "fixDeleteObject".equals(ai.key)) {
                            CheckParameterUtil.ensureThat("this".equals(val), "fixDeleteObject must be followed by 'this'");
                            check.deletion = true;
                        } else if (val != null && "suggestAlternative".equals(ai.key)) {
                            check.alternatives.add(val);
                        } else if (val != null && "assertMatch".equals(ai.key)) {
                            assertions.put(val, Boolean.TRUE);
                        } else if (val != null && "assertNoMatch".equals(ai.key)) {
                            assertions.put(val, Boolean.FALSE);
                        } else if (val != null && "group".equals(ai.key)) {
                            check.group = val;
                        } else if (ai.key.startsWith("-")) {
                            Logging.debug("Ignoring extension instruction: " + ai.key + ": " + ai.val);
                        } else {
                            throw new IllegalDataException("Cannot add instruction " + ai.key + ": " + ai.val + '!');
                        }
                    } catch (IllegalArgumentException e) {
                        throw new IllegalDataException(e);
                    }
                }
            }
            if (check.errors.isEmpty() && check.setClassExpressions.isEmpty()) {
                throw new IllegalDataException(
                        "No "+POSSIBLE_THROWS+" given! You should specify a validation error message for " + rule.selectors);
            } else if (check.errors.size() > 1) {
                throw new IllegalDataException(
                        "More than one "+POSSIBLE_THROWS+" given! You should specify a single validation error message for "
                                + rule.selectors);
            }
            if (assertionConsumer != null) {
                MapCSSTagCheckerAsserts.checkAsserts(check, assertions, assertionConsumer);
            }
            return check.toImmutable();
        }

        static ParseResult readMapCSS(Reader css) throws ParseException {
            return readMapCSS(css, null);
        }

        static ParseResult readMapCSS(Reader css, AssertionConsumer assertionConsumer) throws ParseException {
            CheckParameterUtil.ensureParameterNotNull(css, "css");

            final MapCSSStyleSource source = new MapCSSStyleSource("");
            final MapCSSParser preprocessor = new MapCSSParser(css, MapCSSParser.LexicalState.PREPROCESSOR);
            try (StringReader mapcss = new StringReader(preprocessor.pp_root(source))) {
                new MapCSSParser(mapcss, MapCSSParser.LexicalState.DEFAULT).sheet(source);
            }
            // Ignore "meta" rule(s) from external rules of JOSM wiki
            source.removeMetaRules();
            List<TagCheck> parseChecks = new ArrayList<>();
            for (MapCSSRule rule : source.rules) {
                try {
                    parseChecks.add(TagCheck.ofMapCSSRule(rule, assertionConsumer));
                } catch (IllegalDataException e) {
                    Logging.error("Cannot add MapCSS rule: "+e.getMessage());
                    source.logError(e);
                }
            }
            return new ParseResult(parseChecks, source.getErrors());
        }

        @Override
        public boolean test(OsmPrimitive primitive) {
            // Tests whether the primitive contains a deprecated tag which is represented by this MapCSSTagChecker.
            return whichSelectorMatchesPrimitive(primitive) != null;
        }

        Selector whichSelectorMatchesPrimitive(OsmPrimitive primitive) {
            return whichSelectorMatchesEnvironment(new Environment(primitive));
        }

        Selector whichSelectorMatchesEnvironment(Environment env) {
            return rule.selectors.stream()
                    .filter(i -> i.matches(env.clearSelectorMatchingInformation()))
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Determines the {@code index}-th key/value/tag (depending on {@code type}) of the
         * {@link GeneralSelector}.
         * @param matchingSelector matching selector
         * @param index index
         * @param type selector type ("key", "value" or "tag")
         * @param p OSM primitive
         * @return argument value, can be {@code null}
         */
        static String determineArgument(GeneralSelector matchingSelector, int index, String type, OsmPrimitive p) {
            try {
                final Condition c = matchingSelector.getConditions().get(index);
                final Tag tag = c instanceof Condition.ToTagConvertable
                        ? ((Condition.ToTagConvertable) c).asTag(p)
                        : null;
                if (tag == null) {
                    return null;
                } else if ("key".equals(type)) {
                    return tag.getKey();
                } else if ("value".equals(type)) {
                    return tag.getValue();
                } else if ("tag".equals(type)) {
                    return tag.toString();
                }
            } catch (IndexOutOfBoundsException ignore) {
                Logging.debug(ignore);
            }
            return null;
        }

        /**
         * Replaces occurrences of <code>{i.key}</code>, <code>{i.value}</code>, <code>{i.tag}</code> in {@code s} by the corresponding
         * key/value/tag of the {@code index}-th {@link Condition} of {@code matchingSelector}.
         * @param matchingSelector matching selector
         * @param s any string
         * @param p OSM primitive
         * @return string with arguments inserted
         */
        static String insertArguments(Selector matchingSelector, String s, OsmPrimitive p) {
            if (s != null && matchingSelector instanceof Selector.ChildOrParentSelector) {
                return insertArguments(((Selector.ChildOrParentSelector) matchingSelector).right, s, p);
            } else if (s == null || !(matchingSelector instanceof GeneralSelector)) {
                return s;
            }
            final Matcher m = Pattern.compile("\\{(\\d+)\\.(key|value|tag)\\}").matcher(s);
            final StringBuffer sb = new StringBuffer();
            while (m.find()) {
                final String argument = determineArgument((GeneralSelector) matchingSelector,
                        Integer.parseInt(m.group(1)), m.group(2), p);
                try {
                    // Perform replacement with null-safe + regex-safe handling
                    m.appendReplacement(sb, String.valueOf(argument).replace("^(", "").replace(")$", ""));
                } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                    Logging.log(Logging.LEVEL_ERROR, tr("Unable to replace argument {0} in {1}: {2}", argument, sb, e.getMessage()), e);
                }
            }
            m.appendTail(sb);
            return sb.toString();
        }

        /**
         * Constructs a fix in terms of a {@link org.openstreetmap.josm.command.Command} for the {@link OsmPrimitive}
         * if the error is fixable, or {@code null} otherwise.
         *
         * @param p the primitive to construct the fix for
         * @return the fix or {@code null}
         */
        Command fixPrimitive(OsmPrimitive p) {
            if (p.getDataSet() == null || (fixCommands.isEmpty() && !deletion)) {
                return null;
            }
            try {
                final Selector matchingSelector = whichSelectorMatchesPrimitive(p);
                Collection<Command> cmds = fixCommands.stream()
                        .map(fixCommand -> fixCommand.createCommand(p, matchingSelector))
                        .collect(Collectors.toList());
                if (deletion && !p.isDeleted()) {
                    cmds.add(new DeleteCommand(p));
                }
                return new SequenceCommand(tr("Fix of {0}", getDescriptionForMatchingSelector(p, matchingSelector)), cmds);
            } catch (IllegalArgumentException e) {
                Logging.error(e);
                return null;
            }
        }

        /**
         * Constructs a (localized) message for this deprecation check.
         * @param p OSM primitive
         *
         * @return a message
         */
        String getMessage(OsmPrimitive p) {
            if (errors.isEmpty()) {
                // Return something to avoid NPEs
                return rule.declaration.toString();
            } else {
                final Object val = errors.keySet().iterator().next().val;
                return String.valueOf(
                        val instanceof Expression
                                ? ((Expression) val).evaluate(new Environment(p))
                                : val
                );
            }
        }

        /**
         * Constructs a (localized) description for this deprecation check.
         * @param p OSM primitive
         *
         * @return a description (possibly with alternative suggestions)
         * @see #getDescriptionForMatchingSelector
         */
        String getDescription(OsmPrimitive p) {
            if (alternatives.isEmpty()) {
                return getMessage(p);
            } else {
                /* I18N: {0} is the test error message and {1} is an alternative */
                return tr("{0}, use {1} instead", getMessage(p), String.join(tr(" or "), alternatives));
            }
        }

        /**
         * Constructs a (localized) description for this deprecation check
         * where any placeholders are replaced by values of the matched selector.
         *
         * @param matchingSelector matching selector
         * @param p OSM primitive
         * @return a description (possibly with alternative suggestions)
         */
        String getDescriptionForMatchingSelector(OsmPrimitive p, Selector matchingSelector) {
            return insertArguments(matchingSelector, getDescription(p), p);
        }

        Severity getSeverity() {
            return errors.isEmpty() ? null : errors.values().iterator().next();
        }

        @Override
        public String toString() {
            return getDescription(null);
        }

        /**
         * Constructs a {@link TestError} for the given primitive, or returns null if the primitive does not give rise to an error.
         *
         * @param p the primitive to construct the error for
         * @param matchingSelector the matching selector (e.g., obtained via {@link #whichSelectorMatchesPrimitive})
         * @param env the environment
         * @param tester the tester
         * @return an instance of {@link TestError}, or returns null if the primitive does not give rise to an error.
         */
        protected List<TestError> getErrorsForPrimitive(OsmPrimitive p, Selector matchingSelector, Environment env, Test tester) {
            List<TestError> res = new ArrayList<>();
            if (matchingSelector != null && !errors.isEmpty()) {
                final Command fix = fixPrimitive(p);
                final String description = getDescriptionForMatchingSelector(p, matchingSelector);
                final String description1 = group == null ? description : group;
                final String description2 = group == null ? null : description;
                final String selector = matchingSelector.toString();
                TestError.Builder errorBuilder = TestError.builder(tester, getSeverity(), 3000)
                        .messageWithManuallyTranslatedDescription(description1, description2, selector);
                if (fix != null) {
                    errorBuilder.fix(() -> fix);
                }
                if (env.child instanceof OsmPrimitive) {
                    res.add(errorBuilder.primitives(p, (OsmPrimitive) env.child).build());
                } else if (env.children != null) {
                    for (IPrimitive c : env.children) {
                        if (c instanceof OsmPrimitive) {
                            errorBuilder = TestError.builder(tester, getSeverity(), 3000)
                                    .messageWithManuallyTranslatedDescription(description1, description2, selector);
                            if (fix != null) {
                                errorBuilder.fix(() -> fix);
                            }
                            // check if we have special information about highlighted objects */
                            boolean hiliteFound = false;
                            if (env.intersections != null) {
                                Area is = env.intersections.get(c);
                                if (is != null) {
                                    errorBuilder.highlight(is);
                                    hiliteFound = true;
                                }
                            }
                            if (env.crossingWaysMap != null && !hiliteFound) {
                                Map<List<Way>, List<WaySegment>> is = env.crossingWaysMap.get(c);
                                if (is != null) {
                                    Set<WaySegment> toHilite = new HashSet<>();
                                    for (List<WaySegment> wsList : is.values()) {
                                        toHilite.addAll(wsList);
                                    }
                                    errorBuilder.highlightWaySegments(toHilite);
                                }
                            }
                            res.add(errorBuilder.primitives(p, (OsmPrimitive) c).build());
                        }
                    }
                } else {
                    res.add(errorBuilder.primitives(p).build());
                }
            }
            return res;
        }

    }

    static class MapCSSTagCheckerAndRule extends MapCSSTagChecker {
        public final MapCSSRule rule;
        private final TagCheck tagCheck;
        private final String source;

        MapCSSTagCheckerAndRule(MapCSSRule rule) {
            this.rule = rule;
            this.tagCheck = null;
            this.source = "";
        }

        MapCSSTagCheckerAndRule(TagCheck tagCheck, String source) {
            this.rule = tagCheck.rule;
            this.tagCheck = tagCheck;
            this.source = source;
        }

        @Override
        public String toString() {
            return "MapCSSTagCheckerAndRule [rule=" + rule + ']';
        }

        @Override
        public String getSource() {
            return source;
        }
    }

    static MapCSSStyleIndex createMapCSSTagCheckerIndex(MultiMap<String, TagCheck> checks, boolean includeOtherSeverity, boolean allTests) {
        final MapCSSStyleIndex index = new MapCSSStyleIndex();
        final Stream<MapCSSRule> ruleStream = checks.values().stream()
                .flatMap(Collection::stream)
                // Ignore "information" level checks if not wanted, unless they also set a MapCSS class
                .filter(c -> includeOtherSeverity || Severity.OTHER != c.getSeverity() || !c.setClassExpressions.isEmpty())
                .filter(c -> allTests || c.rule.selectors.stream().anyMatch(Selector.ChildOrParentSelector.class::isInstance))
                .map(c -> c.rule);
        index.buildIndex(ruleStream);
        return index;
    }

    /**
     * Obtains all {@link TestError}s for the {@link OsmPrimitive} {@code p}.
     * @param p The OSM primitive
     * @param includeOtherSeverity if {@code true}, errors of severity {@link Severity#OTHER} (info) will also be returned
     * @return all errors for the given primitive, with or without those of "info" severity
     */
    public synchronized Collection<TestError> getErrorsForPrimitive(OsmPrimitive p, boolean includeOtherSeverity) {
        final List<TestError> res = new ArrayList<>();
        if (indexData == null) {
            indexData = createMapCSSTagCheckerIndex(checks, includeOtherSeverity, ALL_TESTS);
        }

        Environment env = new Environment(p, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        env.mpAreaCache = mpAreaCache;

        Iterator<MapCSSRule> candidates = indexData.getRuleCandidates(p);
        while (candidates.hasNext()) {
            MapCSSRule r = candidates.next();
            for (Selector selector : r.selectors) {
                env.clearSelectorMatchingInformation();
                if (!selector.matches(env)) { // as side effect env.parent will be set (if s is a child selector)
                    continue;
                }
                MapCSSTagCheckerAndRule test = ruleToCheckMap.computeIfAbsent(r, rule -> checks.entrySet().stream()
                        .map(e -> e.getValue().stream()
                                // rule.selectors might be different due to MapCSSStyleIndex, however, the declarations are the same object
                                .filter(c -> c.rule.declaration == rule.declaration)
                                .findFirst()
                                .map(c -> new MapCSSTagCheckerAndRule(c, getTitle(e.getKey())))
                                .orElse(null))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
                TagCheck check = test == null ? null : test.tagCheck;
                if (check != null) {
                    r.declaration.execute(env);
                    if (!check.errors.isEmpty()) {
                        for (TestError e: check.getErrorsForPrimitive(p, selector, env, test)) {
                            addIfNotSimilar(e, res);
                        }
                    }
                }
            }
        }
        return res;
    }

    private String getTitle(String url) {
        return urlTitles.getOrDefault(url, tr("unknown"));
    }

    /**
     * See #12627
     * Add error to given list if list doesn't already contain a similar error.
     * Similar means same code and description and same combination of primitives and same combination of highlighted objects,
     * but maybe with different orders.
     * @param toAdd the error to add
     * @param errors the list of errors
     */
    private static void addIfNotSimilar(TestError toAdd, List<TestError> errors) {
        final boolean isDup = toAdd.getPrimitives().size() >= 2 && errors.stream()
                .anyMatch(e -> e.getCode() == toAdd.getCode()
                        && e.getMessage().equals(toAdd.getMessage())
                        && e.getPrimitives().size() == toAdd.getPrimitives().size()
                        && e.getPrimitives().containsAll(toAdd.getPrimitives())
                        && highlightedIsEqual(e.getHighlighted(), toAdd.getHighlighted()));
        if (!isDup)
            errors.add(toAdd);
    }

    private static boolean highlightedIsEqual(Collection<?> highlighted, Collection<?> highlighted2) {
        if (highlighted.size() == highlighted2.size()) {
            if (!highlighted.isEmpty()) {
                Object h1 = highlighted.iterator().next();
                Object h2 = highlighted2.iterator().next();
                if (h1 instanceof Area && h2 instanceof Area) {
                    return ((Area) h1).equals((Area) h2);
                }
                return highlighted.containsAll(highlighted2);
            }
            return true;
        }
        return false;
    }

    static Collection<TestError> getErrorsForPrimitive(OsmPrimitive p, boolean includeOtherSeverity,
            Collection<Set<TagCheck>> checksCol) {
        // this variant is only used by the assertion tests
        final List<TestError> r = new ArrayList<>();
        final Environment env = new Environment(p, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        env.mpAreaCache = mpAreaCache;
        for (Set<TagCheck> schecks : checksCol) {
            for (TagCheck check : schecks) {
                boolean ignoreError = Severity.OTHER == check.getSeverity() && !includeOtherSeverity;
                // Do not run "information" level checks if not wanted, unless they also set a MapCSS class
                if (ignoreError && check.setClassExpressions.isEmpty()) {
                    continue;
                }
                final Selector selector = check.whichSelectorMatchesEnvironment(env);
                if (selector != null) {
                    check.rule.declaration.execute(env);
                    if (!ignoreError && !check.errors.isEmpty()) {
                        r.addAll(check.getErrorsForPrimitive(p, selector, env, new MapCSSTagCheckerAndRule(check.rule)));
                    }
                }
            }
        }
        return r;
    }

    /**
     * Visiting call for primitives.
     *
     * @param p The primitive to inspect.
     */
    @Override
    public void check(OsmPrimitive p) {
        for (TestError e : getErrorsForPrimitive(p, ValidatorPrefHelper.PREF_OTHER.get())) {
            addIfNotSimilar(e, errors);
        }
    }

    /**
     * A handler for assertion error messages (for not fulfilled "assertMatch", "assertNoMatch").
     */
    @FunctionalInterface
    interface AssertionConsumer extends Consumer<String> {
    }

    /**
     * Adds a new MapCSS config file from the given URL.
     * @param url The unique URL of the MapCSS config file
     * @return List of tag checks and parsing errors, or null
     * @throws ParseException if the config file does not match MapCSS syntax
     * @throws IOException if any I/O error occurs
     * @since 7275
     */
    public synchronized ParseResult addMapCSS(String url) throws ParseException, IOException {
        // Check assertions, useful for development of local files
        final boolean checkAssertions = Config.getPref().getBoolean("validator.check_assert_local_rules", false) && Utils.isLocalUrl(url);
        return addMapCSS(url, checkAssertions ? Logging::warn : null);
    }

    synchronized ParseResult addMapCSS(String url, AssertionConsumer assertionConsumer) throws ParseException, IOException {
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        ParseResult result;
        try (CachedFile cache = new CachedFile(url);
             InputStream zip = cache.findZipEntryInputStream("validator.mapcss", "");
             InputStream s = zip != null ? zip : cache.getInputStream();
             Reader reader = new BufferedReader(UTFInputStreamReader.create(s))) {
            if (zip != null)
                I18n.addTexts(cache.getFile());
            result = TagCheck.readMapCSS(reader, assertionConsumer);
            checks.remove(url);
            checks.putAll(url, result.parseChecks);
            urlTitles.put(url, findURLTitle(url));
            indexData = null;
        }
        return result;
    }

    /** Find a user friendly string for the url.
     *
     * @param url the source for the set of rules
     * @return a value that can be used in tool tip or progress bar.
     */
    private static String findURLTitle(String url) {
        for (SourceEntry source : new ValidatorPrefHelper().get()) {
            if (url.equals(source.url) && source.title != null && !source.title.isEmpty()) {
                return source.title;
            }
        }
        if (url.endsWith(".mapcss")) // do we have others?
            url = new File(url).getName();
        if (url.length() > 33) {
            url = "..." + url.substring(url.length() - 30);
        }
        return url;
    }

    @Override
    public synchronized void initialize() throws Exception {
        checks.clear();
        urlTitles.clear();
        indexData = null;
        for (SourceEntry source : new ValidatorPrefHelper().get()) {
            if (!source.active) {
                continue;
            }
            String i = source.url;
            try {
                if (!i.startsWith("resource:")) {
                    Logging.info(tr("Adding {0} to tag checker", i));
                } else if (Logging.isDebugEnabled()) {
                    Logging.debug(tr("Adding {0} to tag checker", i));
                }
                addMapCSS(i);
                if (Config.getPref().getBoolean("validator.auto_reload_local_rules", true) && source.isLocal()) {
                    FileWatcher.getDefaultInstance().registerSource(source);
                }
            } catch (IOException | IllegalStateException | IllegalArgumentException ex) {
                Logging.warn(tr("Failed to add {0} to tag checker", i));
                Logging.log(Logging.LEVEL_WARN, ex);
            } catch (ParseException | TokenMgrError ex) {
                Logging.warn(tr("Failed to add {0} to tag checker", i));
                Logging.warn(ex);
            }
        }
        MapCSSTagCheckerAsserts.clear();
    }

    /**
     * Reload tagchecker rule.
     * @param rule tagchecker rule to reload
     * @since 12825
     */
    public static void reloadRule(SourceEntry rule) {
        MapCSSTagChecker tagChecker = OsmValidator.getTest(MapCSSTagChecker.class);
        if (tagChecker != null) {
            try {
                tagChecker.addMapCSS(rule.url);
            } catch (IOException | ParseException | TokenMgrError e) {
                Logging.warn(e);
            }
        }
    }

    @Override
    public synchronized void startTest(ProgressMonitor progressMonitor) {
        super.startTest(progressMonitor);
        super.setShowElements(true);
    }

    @Override
    public synchronized void endTest() {
        // no need to keep the index, it is quickly build and doubles the memory needs
        indexData = null;
        // always clear the cache to make sure that we catch changes in geometry
        mpAreaCache.clear();
        ruleToCheckMap.clear();
        super.endTest();
    }

    @Override
    public void visit(Collection<OsmPrimitive> selection) {
        if (progressMonitor != null) {
            progressMonitor.setTicksCount(selection.size() * checks.size());
        }

        mpAreaCache.clear();

        Set<OsmPrimitive> surrounding = new HashSet<>();
        for (Entry<String, Set<TagCheck>> entry : checks.entrySet()) {
            if (isCanceled()) {
                break;
            }
            visit(entry.getKey(), entry.getValue(), selection, surrounding);
        }
    }

    /**
     * Perform the checks for one check url
     * @param url the url for the checks
     * @param checksForUrl the checks to perform
     * @param selection collection primitives
     * @param surrounding surrounding primitives, evtl. filled by this routine
     */
    private void visit(String url, Set<TagCheck> checksForUrl, Collection<OsmPrimitive> selection,
            Set<OsmPrimitive> surrounding) {
        MultiMap<String, TagCheck> currentCheck = new MultiMap<>();
        currentCheck.putAll(url, checksForUrl);
        indexData = createMapCSSTagCheckerIndex(currentCheck, includeOtherSeverityChecks(), ALL_TESTS);
        Set<OsmPrimitive> tested = new HashSet<>();


        String title = getTitle(url);
        if (progressMonitor != null) {
            progressMonitor.setExtraText(tr(" {0}", title));
        }
        long cnt = 0;
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (OsmPrimitive p : selection) {
            if (isCanceled()) {
                break;
            }
            if (isPrimitiveUsable(p)) {
                check(p);
                if (partialSelection) {
                    tested.add(p);
                }
            }
            if (progressMonitor != null) {
                progressMonitor.worked(1);
                cnt++;
                // add frequently changing info to progress monitor so that it
                // doesn't seem to hang when test takes longer than 0.5 seconds
                if (cnt % 10000 == 0 && stopwatch.elapsed() >= 500) {
                    progressMonitor.setExtraText(tr(" {0}: {1} of {2} elements done", title, cnt, selection.size()));
                }
            }
        }

        if (partialSelection && !tested.isEmpty()) {
            testPartial(currentCheck, tested, surrounding);
        }
    }

    private void testPartial(MultiMap<String, TagCheck> currentCheck, Set<OsmPrimitive> tested,
            Set<OsmPrimitive> surrounding) {

        // #14287: see https://josm.openstreetmap.de/ticket/14287#comment:15
        // execute tests for objects which might contain or cross previously tested elements

        final boolean includeOtherSeverity = includeOtherSeverityChecks();
        // rebuild index with a reduced set of rules (those that use ChildOrParentSelector) and thus may have left selectors
        // matching the previously tested elements
        indexData = createMapCSSTagCheckerIndex(currentCheck, includeOtherSeverity, ONLY_SELECTED_TESTS);
        if (indexData.isEmpty())
            return; // performance: some *.mapcss rule files don't use ChildOrParentSelector

        if (surrounding.isEmpty()) {
            for (OsmPrimitive p : tested) {
                if (p.getDataSet() != null) {
                    surrounding.addAll(p.getDataSet().searchWays(p.getBBox()));
                    surrounding.addAll(p.getDataSet().searchRelations(p.getBBox()));
                }
            }
        }

        for (OsmPrimitive p : surrounding) {
            if (tested.contains(p))
                continue;
            Collection<TestError> additionalErrors = getErrorsForPrimitive(p, includeOtherSeverity);
            for (TestError e : additionalErrors) {
                if (e.getPrimitives().stream().anyMatch(tested::contains))
                    addIfNotSimilar(e, errors);
            }
        }

    }

    /**
     * Execute only the rules for the rules matching the given file name. See #19180
     * @param ruleFile the name of the mapcss file, e.g. deprecated.mapcss
     * @param selection collection of primitives
     * @since 16784
     */
    public void runOnly(String ruleFile, Collection<OsmPrimitive> selection) {
        mpAreaCache.clear();

        Set<OsmPrimitive> surrounding = new HashSet<>();
        for (Entry<String, Set<TagCheck>> entry : checks.entrySet()) {
            if (isCanceled()) {
                break;
            }
            if (entry.getKey().endsWith(ruleFile)) {
                visit(entry.getKey(), entry.getValue(), selection, surrounding);
            }
        }

    }
}
