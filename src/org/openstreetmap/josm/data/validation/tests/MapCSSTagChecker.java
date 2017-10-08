// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
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
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.ClassCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule.Declaration;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.AbstractSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;

/**
 * MapCSS-based tag checker/fixer.
 * @since 6506
 */
public class MapCSSTagChecker extends Test.TagTest {

    /**
     * A grouped MapCSSRule with multiple selectors for a single declaration.
     * @see MapCSSRule
     */
    public static class GroupedMapCSSRule {
        /** MapCSS selectors **/
        public final List<Selector> selectors;
        /** MapCSS declaration **/
        public final Declaration declaration;

        /**
         * Constructs a new {@code GroupedMapCSSRule}.
         * @param selectors MapCSS selectors
         * @param declaration MapCSS declaration
         */
        public GroupedMapCSSRule(List<Selector> selectors, Declaration declaration) {
            this.selectors = selectors;
            this.declaration = declaration;
        }

        @Override
        public int hashCode() {
            return Objects.hash(selectors, declaration);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            GroupedMapCSSRule that = (GroupedMapCSSRule) obj;
            return Objects.equals(selectors, that.selectors) &&
                    Objects.equals(declaration, that.declaration);
        }

        @Override
        public String toString() {
            return "GroupedMapCSSRule [selectors=" + selectors + ", declaration=" + declaration + ']';
        }
    }

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
                    final Tag tag = Tag.ofString(evaluateObject(obj, p, matchingSelector));
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
                    final String key = evaluateObject(obj, p, matchingSelector);
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

    /**
     * Result of {@link TagCheck#readMapCSS}
     * @since 8936
     */
    public static class ParseResult {
        /** Checks successfully parsed */
        public final List<TagCheck> parseChecks;
        /** Errors that occured during parsing */
        public final Collection<Throwable> parseErrors;

        /**
         * Constructs a new {@code ParseResult}.
         * @param parseChecks Checks successfully parsed
         * @param parseErrors Errors that occured during parsing
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
        protected final GroupedMapCSSRule rule;
        /** Commands to apply in order to fix a matching primitive */
        protected final List<FixCommand> fixCommands = new ArrayList<>();
        /** Tags (or arbitraty strings) of alternatives to be presented to the user */
        protected final List<String> alternatives = new ArrayList<>();
        /** An {@link org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.AssignmentInstruction}-{@link Severity} pair.
         * Is evaluated on the matching primitive to give the error message. Map is checked to contain exactly one element. */
        protected final Map<Instruction.AssignmentInstruction, Severity> errors = new HashMap<>();
        /** Unit tests */
        protected final Map<String, Boolean> assertions = new HashMap<>();
        /** MapCSS Classes to set on matching primitives */
        protected final Set<String> setClassExpressions = new HashSet<>();
        /** Denotes whether the object should be deleted for fixing it */
        protected boolean deletion;
        /** A string used to group similar tests */
        protected String group;

        TagCheck(GroupedMapCSSRule rule) {
            this.rule = rule;
        }

        private static final String POSSIBLE_THROWS = possibleThrows();

        static final String possibleThrows() {
            StringBuilder sb = new StringBuilder();
            for (Severity s : Severity.values()) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append("throw")
                .append(s.name().charAt(0))
                .append(s.name().substring(1).toLowerCase(Locale.ENGLISH));
            }
            return sb.toString();
        }

        static TagCheck ofMapCSSRule(final GroupedMapCSSRule rule) throws IllegalDataException {
            final TagCheck check = new TagCheck(rule);
            for (Instruction i : rule.declaration.instructions) {
                if (i instanceof Instruction.AssignmentInstruction) {
                    final Instruction.AssignmentInstruction ai = (Instruction.AssignmentInstruction) i;
                    if (ai.isSetInstruction) {
                        check.setClassExpressions.add(ai.key);
                        continue;
                    }
                    try {
                        final String val = ai.val instanceof Expression
                                ? (String) ((Expression) ai.val).evaluate(new Environment())
                                : ai.val instanceof String
                                ? (String) ai.val
                                : ai.val instanceof Keyword
                                ? ((Keyword) ai.val).val
                                : null;
                        if (ai.key.startsWith("throw")) {
                            try {
                                check.errors.put(ai, Severity.valueOf(ai.key.substring("throw".length()).toUpperCase(Locale.ENGLISH)));
                            } catch (IllegalArgumentException e) {
                                Logging.log(Logging.LEVEL_WARN,
                                        "Unsupported "+ai.key+" instruction. Allowed instructions are "+POSSIBLE_THROWS+'.', e);
                            }
                        } else if ("fixAdd".equals(ai.key)) {
                            check.fixCommands.add(FixCommand.fixAdd(ai.val));
                        } else if ("fixRemove".equals(ai.key)) {
                            CheckParameterUtil.ensureThat(!(ai.val instanceof String) || !(val != null && val.contains("=")),
                                    "Unexpected '='. Please only specify the key to remove in: " + ai);
                            check.fixCommands.add(FixCommand.fixRemove(ai.val));
                        } else if (val != null && "fixChangeKey".equals(ai.key)) {
                            CheckParameterUtil.ensureThat(val.contains("=>"), "Separate old from new key by '=>'!");
                            final String[] x = val.split("=>", 2);
                            check.fixCommands.add(FixCommand.fixChangeKey(Tag.removeWhiteSpaces(x[0]), Tag.removeWhiteSpaces(x[1])));
                        } else if (val != null && "fixDeleteObject".equals(ai.key)) {
                            CheckParameterUtil.ensureThat("this".equals(val), "fixDeleteObject must be followed by 'this'");
                            check.deletion = true;
                        } else if (val != null && "suggestAlternative".equals(ai.key)) {
                            check.alternatives.add(val);
                        } else if (val != null && "assertMatch".equals(ai.key)) {
                            check.assertions.put(val, Boolean.TRUE);
                        } else if (val != null && "assertNoMatch".equals(ai.key)) {
                            check.assertions.put(val, Boolean.FALSE);
                        } else if (val != null && "group".equals(ai.key)) {
                            check.group = val;
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
            return check;
        }

        static ParseResult readMapCSS(Reader css) throws ParseException {
            CheckParameterUtil.ensureParameterNotNull(css, "css");

            final MapCSSStyleSource source = new MapCSSStyleSource("");
            final MapCSSParser preprocessor = new MapCSSParser(css, MapCSSParser.LexicalState.PREPROCESSOR);
            final StringReader mapcss = new StringReader(preprocessor.pp_root(source));
            final MapCSSParser parser = new MapCSSParser(mapcss, MapCSSParser.LexicalState.DEFAULT);
            parser.sheet(source);
            // Ignore "meta" rule(s) from external rules of JOSM wiki
            removeMetaRules(source);
            // group rules with common declaration block
            Map<Declaration, List<Selector>> g = new LinkedHashMap<>();
            for (MapCSSRule rule : source.rules) {
                if (!g.containsKey(rule.declaration)) {
                    List<Selector> sels = new ArrayList<>();
                    sels.add(rule.selector);
                    g.put(rule.declaration, sels);
                } else {
                    g.get(rule.declaration).add(rule.selector);
                }
            }
            List<TagCheck> parseChecks = new ArrayList<>();
            for (Map.Entry<Declaration, List<Selector>> map : g.entrySet()) {
                try {
                    parseChecks.add(TagCheck.ofMapCSSRule(
                            new GroupedMapCSSRule(map.getValue(), map.getKey())));
                } catch (IllegalDataException e) {
                    Logging.error("Cannot add MapCss rule: "+e.getMessage());
                    source.logError(e);
                }
            }
            return new ParseResult(parseChecks, source.getErrors());
        }

        private static void removeMetaRules(MapCSSStyleSource source) {
            for (Iterator<MapCSSRule> it = source.rules.iterator(); it.hasNext();) {
                MapCSSRule x = it.next();
                if (x.selector instanceof GeneralSelector) {
                    GeneralSelector gs = (GeneralSelector) x.selector;
                    if ("meta".equals(gs.base)) {
                        it.remove();
                    }
                }
            }
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
            for (Selector i : rule.selectors) {
                env.clearSelectorMatchingInformation();
                if (i.matches(env)) {
                    return i;
                }
            }
            return null;
        }

        /**
         * Determines the {@code index}-th key/value/tag (depending on {@code type}) of the
         * {@link org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector}.
         * @param matchingSelector matching selector
         * @param index index
         * @param type selector type ("key", "value" or "tag")
         * @param p OSM primitive
         * @return argument value, can be {@code null}
         */
        static String determineArgument(Selector.GeneralSelector matchingSelector, int index, String type, OsmPrimitive p) {
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
                final String argument = determineArgument((Selector.GeneralSelector) matchingSelector,
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
            if (fixCommands.isEmpty() && !deletion) {
                return null;
            }
            final Selector matchingSelector = whichSelectorMatchesPrimitive(p);
            Collection<Command> cmds = new LinkedList<>();
            for (FixCommand fixCommand : fixCommands) {
                cmds.add(fixCommand.createCommand(p, matchingSelector));
            }
            if (deletion && !p.isDeleted()) {
                cmds.add(new DeleteCommand(p));
            }
            return new SequenceCommand(tr("Fix of {0}", getDescriptionForMatchingSelector(p, matchingSelector)), cmds);
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
                return tr("{0}, use {1} instead", getMessage(p), Utils.join(tr(" or "), alternatives));
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
         * @return an instance of {@link TestError}, or returns null if the primitive does not give rise to an error.
         */
        TestError getErrorForPrimitive(OsmPrimitive p) {
            final Environment env = new Environment(p);
            return getErrorForPrimitive(p, whichSelectorMatchesEnvironment(env), env, null);
        }

        TestError getErrorForPrimitive(OsmPrimitive p, Selector matchingSelector, Environment env, Test tester) {
            if (matchingSelector != null && !errors.isEmpty()) {
                final Command fix = fixPrimitive(p);
                final String description = getDescriptionForMatchingSelector(p, matchingSelector);
                final String description1 = group == null ? description : group;
                final String description2 = group == null ? null : description;
                final List<OsmPrimitive> primitives;
                if (env.child != null) {
                    primitives = Arrays.asList(p, env.child);
                } else {
                    primitives = Collections.singletonList(p);
                }
                final TestError.Builder error = TestError.builder(tester, getSeverity(), 3000)
                        .messageWithManuallyTranslatedDescription(description1, description2, matchingSelector.toString())
                        .primitives(primitives);
                if (fix != null) {
                    return error.fix(() -> fix).build();
                } else {
                    return error.build();
                }
            } else {
                return null;
            }
        }

        /**
         * Returns the set of tagchecks on which this check depends on.
         * @param schecks the collection of tagcheks to search in
         * @return the set of tagchecks on which this check depends on
         * @since 7881
         */
        public Set<TagCheck> getTagCheckDependencies(Collection<TagCheck> schecks) {
            Set<TagCheck> result = new HashSet<>();
            Set<String> classes = getClassesIds();
            if (schecks != null && !classes.isEmpty()) {
                for (TagCheck tc : schecks) {
                    if (this.equals(tc)) {
                        continue;
                    }
                    for (String id : tc.setClassExpressions) {
                        if (classes.contains(id)) {
                            result.add(tc);
                            break;
                        }
                    }
                }
            }
            return result;
        }

        /**
         * Returns the list of ids of all MapCSS classes referenced in the rule selectors.
         * @return the list of ids of all MapCSS classes referenced in the rule selectors
         * @since 7881
         */
        public Set<String> getClassesIds() {
            Set<String> result = new HashSet<>();
            for (Selector s : rule.selectors) {
                if (s instanceof AbstractSelector) {
                    for (Condition c : ((AbstractSelector) s).getConditions()) {
                        if (c instanceof ClassCondition) {
                            result.add(((ClassCondition) c).id);
                        }
                    }
                }
            }
            return result;
        }
    }

    static class MapCSSTagCheckerAndRule extends MapCSSTagChecker {
        public final GroupedMapCSSRule rule;

        MapCSSTagCheckerAndRule(GroupedMapCSSRule rule) {
            this.rule = rule;
        }

        @Override
        public synchronized boolean equals(Object obj) {
            return super.equals(obj)
                    || (obj instanceof TagCheck && rule.equals(((TagCheck) obj).rule))
                    || (obj instanceof GroupedMapCSSRule && rule.equals(obj));
        }

        @Override
        public synchronized int hashCode() {
            return Objects.hash(super.hashCode(), rule);
        }

        @Override
        public String toString() {
            return "MapCSSTagCheckerAndRule [rule=" + rule + ']';
        }
    }

    /**
     * Obtains all {@link TestError}s for the {@link OsmPrimitive} {@code p}.
     * @param p The OSM primitive
     * @param includeOtherSeverity if {@code true}, errors of severity {@link Severity#OTHER} (info) will also be returned
     * @return all errors for the given primitive, with or without those of "info" severity
     */
    public synchronized Collection<TestError> getErrorsForPrimitive(OsmPrimitive p, boolean includeOtherSeverity) {
        return getErrorsForPrimitive(p, includeOtherSeverity, checks.values());
    }

    private static Collection<TestError> getErrorsForPrimitive(OsmPrimitive p, boolean includeOtherSeverity,
            Collection<Set<TagCheck>> checksCol) {
        final List<TestError> r = new ArrayList<>();
        final Environment env = new Environment(p, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        for (Set<TagCheck> schecks : checksCol) {
            for (TagCheck check : schecks) {
                boolean ignoreError = Severity.OTHER.equals(check.getSeverity()) && !includeOtherSeverity;
                // Do not run "information" level checks if not wanted, unless they also set a MapCSS class
                if (ignoreError && check.setClassExpressions.isEmpty()) {
                    continue;
                }
                final Selector selector = check.whichSelectorMatchesEnvironment(env);
                if (selector != null) {
                    check.rule.declaration.execute(env);
                    if (!ignoreError) {
                        final TestError error = check.getErrorForPrimitive(p, selector, env, new MapCSSTagCheckerAndRule(check.rule));
                        if (error != null) {
                            r.add(error);
                        }
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
        errors.addAll(getErrorsForPrimitive(p, ValidatorPrefHelper.PREF_OTHER.get()));
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
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        ParseResult result;
        try (CachedFile cache = new CachedFile(url);
             InputStream zip = cache.findZipEntryInputStream("validator.mapcss", "");
             InputStream s = zip != null ? zip : cache.getInputStream();
             Reader reader = new BufferedReader(UTFInputStreamReader.create(s))) {
            result = TagCheck.readMapCSS(reader);
            checks.remove(url);
            checks.putAll(url, result.parseChecks);
            // Check assertions, useful for development of local files
            if (Config.getPref().getBoolean("validator.check_assert_local_rules", false) && Utils.isLocalUrl(url)) {
                for (String msg : checkAsserts(result.parseChecks)) {
                    Logging.warn(msg);
                }
            }
        }
        return result;
    }

    @Override
    public synchronized void initialize() throws Exception {
        checks.clear();
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
                    Main.fileWatcher.registerSource(source);
                }
            } catch (IOException | IllegalStateException | IllegalArgumentException ex) {
                Logging.warn(tr("Failed to add {0} to tag checker", i));
                Logging.log(Logging.LEVEL_WARN, ex);
            } catch (ParseException ex) {
                Logging.warn(tr("Failed to add {0} to tag checker", i));
                Logging.warn(ex);
            }
        }
    }

    /**
     * Checks that rule assertions are met for the given set of TagChecks.
     * @param schecks The TagChecks for which assertions have to be checked
     * @return A set of error messages, empty if all assertions are met
     * @since 7356
     */
    public Set<String> checkAsserts(final Collection<TagCheck> schecks) {
        Set<String> assertionErrors = new LinkedHashSet<>();
        final DataSet ds = new DataSet();
        for (final TagCheck check : schecks) {
            Logging.debug("Check: {0}", check);
            for (final Map.Entry<String, Boolean> i : check.assertions.entrySet()) {
                Logging.debug("- Assertion: {0}", i);
                final OsmPrimitive p = OsmUtils.createPrimitive(i.getKey());
                // Build minimal ordered list of checks to run to test the assertion
                List<Set<TagCheck>> checksToRun = new ArrayList<>();
                Set<TagCheck> checkDependencies = check.getTagCheckDependencies(schecks);
                if (!checkDependencies.isEmpty()) {
                    checksToRun.add(checkDependencies);
                }
                checksToRun.add(Collections.singleton(check));
                // Add primitive to dataset to avoid DataIntegrityProblemException when evaluating selectors
                ds.addPrimitive(p);
                final Collection<TestError> pErrors = getErrorsForPrimitive(p, true, checksToRun);
                Logging.debug("- Errors: {0}", pErrors);
                @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "EqualsIncompatibleType"})
                final boolean isError = pErrors.stream().anyMatch(e -> e.getTester().equals(check.rule));
                if (isError != i.getValue()) {
                    final String error = MessageFormat.format("Expecting test ''{0}'' (i.e., {1}) to {2} {3} (i.e., {4})",
                            check.getMessage(p), check.rule.selectors, i.getValue() ? "match" : "not match", i.getKey(), p.getKeys());
                    assertionErrors.add(error);
                }
                ds.removePrimitive(p);
            }
        }
        return assertionErrors;
    }

    @Override
    public synchronized int hashCode() {
        return Objects.hash(super.hashCode(), checks);
    }

    @Override
    public synchronized boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        MapCSSTagChecker that = (MapCSSTagChecker) obj;
        return Objects.equals(checks, that.checks);
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
            } catch (IOException | ParseException e) {
                Logging.warn(e);
            }
        }
    }
}
