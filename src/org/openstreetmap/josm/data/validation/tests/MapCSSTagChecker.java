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
import java.util.Map;
import java.util.Set;
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
import org.openstreetmap.josm.data.validation.FixableTestError;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.ClassCondition;
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
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorPreference;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorTagCheckerRulesPreference;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Predicate;
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
            final int prime = 31;
            int result = 1;
            result = prime * result + ((declaration == null) ? 0 : declaration.hashCode());
            result = prime * result + ((selectors == null) ? 0 : selectors.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof GroupedMapCSSRule))
                return false;
            GroupedMapCSSRule other = (GroupedMapCSSRule) obj;
            if (declaration == null) {
                if (other.declaration != null)
                    return false;
            } else if (!declaration.equals(other.declaration))
                return false;
            if (selectors == null) {
                if (other.selectors != null)
                    return false;
            } else if (!selectors.equals(other.selectors))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "GroupedMapCSSRule [selectors=" + selectors + ", declaration=" + declaration + "]";
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
    abstract static class FixCommand {
        /**
         * Creates the fixing {@link Command} for the given primitive. The {@code matchingSelector} is used to
         * evaluate placeholders (cf. {@link org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.TagCheck#insertArguments(Selector, String, OsmPrimitive)}).
         */
        abstract Command createCommand(final OsmPrimitive p, final Selector matchingSelector);

        private static void checkObject(final Object obj) {
            CheckParameterUtil.ensureThat(obj instanceof Expression || obj instanceof String, "instance of Exception or String expected, but got " + obj);
        }

        /**
         * Evaluates given object as {@link Expression} or {@link String} on the matched {@link OsmPrimitive} and {@code matchingSelector}.
         */
        private static String evaluateObject(final Object obj, final OsmPrimitive p, final Selector matchingSelector) {
            final String s;
            if (obj instanceof Expression) {
                s = (String) ((Expression) obj).evaluate(new Environment().withPrimitive(p));
            } else if (obj instanceof String) {
                s = (String) obj;
            } else {
                return null;
            }
            return TagCheck.insertArguments(matchingSelector, s, p);
        }

        /**
         * Creates a fixing command which executes a {@link ChangePropertyCommand} on the specified tag.
         */
        static FixCommand fixAdd(final Object obj) {
            checkObject(obj);
            return new FixCommand() {
                @Override
                Command createCommand(OsmPrimitive p, Selector matchingSelector) {
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
         */
        static FixCommand fixRemove(final Object obj) {
            checkObject(obj);
            return new FixCommand() {
                @Override
                Command createCommand(OsmPrimitive p, Selector matchingSelector) {
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
         */
        static FixCommand fixChangeKey(final String oldKey, final String newKey) {
            return new FixCommand() {
                @Override
                Command createCommand(OsmPrimitive p, Selector matchingSelector) {
                    return new ChangePropertyKeyCommand(p, oldKey, newKey);
                }

                @Override
                public String toString() {
                    return "fixChangeKey: " + oldKey + " => " + newKey;
                }
            };
        }
    }

    final MultiMap<String, TagCheck> checks = new MultiMap<>();

    static class TagCheck implements Predicate<OsmPrimitive> {
        protected final GroupedMapCSSRule rule;
        protected final List<FixCommand> fixCommands = new ArrayList<>();
        protected final List<String> alternatives = new ArrayList<>();
        protected final Map<Instruction.AssignmentInstruction, Severity> errors = new HashMap<>();
        protected final Map<String, Boolean> assertions = new HashMap<>();
        protected final Set<String> setClassExpressions = new HashSet<>();
        protected boolean deletion = false;

        TagCheck(GroupedMapCSSRule rule) {
            this.rule = rule;
        }

        private static final String POSSIBLE_THROWS = possibleThrows();

        static final String possibleThrows() {
            StringBuffer sb = new StringBuffer();
            for (Severity s : Severity.values()) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append("throw")
                .append(s.name().charAt(0))
                .append(s.name().substring(1).toLowerCase());
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
                    final String val = ai.val instanceof Expression
                            ? (String) ((Expression) ai.val).evaluate(new Environment())
                            : ai.val instanceof String
                            ? (String) ai.val
                            : ai.val instanceof Keyword
                            ? ((Keyword) ai.val).val
                            : null;
                    if (ai.key.startsWith("throw")) {
                        try {
                            final Severity severity = Severity.valueOf(ai.key.substring("throw".length()).toUpperCase());
                            check.errors.put(ai, severity);
                        } catch (IllegalArgumentException e) {
                            Main.warn("Unsupported "+ai.key+" instruction. Allowed instructions are "+POSSIBLE_THROWS);
                        }
                    } else if ("fixAdd".equals(ai.key)) {
                        check.fixCommands.add(FixCommand.fixAdd(ai.val));
                    } else if ("fixRemove".equals(ai.key)) {
                        CheckParameterUtil.ensureThat(!(ai.val instanceof String) || !(val != null && val.contains("=")),
                                "Unexpected '='. Please only specify the key to remove!");
                        check.fixCommands.add(FixCommand.fixRemove(ai.val));
                    } else if ("fixChangeKey".equals(ai.key) && val != null) {
                        CheckParameterUtil.ensureThat(val.contains("=>"), "Separate old from new key by '=>'!");
                        final String[] x = val.split("=>", 2);
                        check.fixCommands.add(FixCommand.fixChangeKey(Tag.removeWhiteSpaces(x[0]), Tag.removeWhiteSpaces(x[1])));
                    } else if ("fixDeleteObject".equals(ai.key) && val != null) {
                        CheckParameterUtil.ensureThat(val.equals("this"), "fixDeleteObject must be followed by 'this'");
                        check.deletion = true;
                    } else if ("suggestAlternative".equals(ai.key) && val != null) {
                        check.alternatives.add(val);
                    } else if ("assertMatch".equals(ai.key) && val != null) {
                        check.assertions.put(val, true);
                    } else if ("assertNoMatch".equals(ai.key) && val != null) {
                        check.assertions.put(val, false);
                    } else {
                        throw new IllegalDataException("Cannot add instruction " + ai.key + ": " + ai.val + "!");
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

        static List<TagCheck> readMapCSS(Reader css) throws ParseException {
            CheckParameterUtil.ensureParameterNotNull(css, "css");

            final MapCSSStyleSource source = new MapCSSStyleSource("");
            final MapCSSParser preprocessor = new MapCSSParser(css, MapCSSParser.LexicalState.PREPROCESSOR);

            css = new StringReader(preprocessor.pp_root(source));
            final MapCSSParser parser = new MapCSSParser(css, MapCSSParser.LexicalState.DEFAULT);
            parser.sheet(source);
            assert source.getErrors().isEmpty();
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
            List<TagCheck> result = new ArrayList<>();
            for (Map.Entry<Declaration, List<Selector>> map : g.entrySet()) {
                try {
                    result.add(TagCheck.ofMapCSSRule(
                            new GroupedMapCSSRule(map.getValue(), map.getKey())));
                } catch (IllegalDataException e) {
                    Main.error("Cannot add MapCss rule: "+e.getMessage());
                }
            }
            return result;
        }

        private static void removeMetaRules(MapCSSStyleSource source) {
            for (Iterator<MapCSSRule> it = source.rules.iterator(); it.hasNext(); ) {
                MapCSSRule x = it.next();
                if (x.selector instanceof GeneralSelector) {
                    GeneralSelector gs = (GeneralSelector) x.selector;
                    if ("meta".equals(gs.base) && gs.getConditions().isEmpty()) {
                        it.remove();
                    }
                }
            }
        }

        @Override
        public boolean evaluate(OsmPrimitive primitive) {
            // Tests whether the primitive contains a deprecated tag which is represented by this MapCSSTagChecker.
            return whichSelectorMatchesPrimitive(primitive) != null;
        }

        Selector whichSelectorMatchesPrimitive(OsmPrimitive primitive) {
            return whichSelectorMatchesEnvironment(new Environment().withPrimitive(primitive));
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
         */
        static String determineArgument(Selector.GeneralSelector matchingSelector, int index, String type, OsmPrimitive p) {
            try {
                final Condition c = matchingSelector.getConditions().get(index);
                final Tag tag = c instanceof Condition.KeyCondition
                        ? ((Condition.KeyCondition) c).asTag(p)
                        : c instanceof Condition.SimpleKeyValueCondition
                        ? ((Condition.SimpleKeyValueCondition) c).asTag()
                        : c instanceof Condition.KeyValueCondition
                        ? ((Condition.KeyValueCondition) c).asTag()
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
                if (Main.isDebugEnabled()) {
                    Main.debug(ignore.getMessage());
                }
            }
            return null;
        }

        /**
         * Replaces occurrences of <code>{i.key}</code>, <code>{i.value}</code>, <code>{i.tag}</code> in {@code s} by the corresponding
         * key/value/tag of the {@code index}-th {@link Condition} of {@code matchingSelector}.
         */
        static String insertArguments(Selector matchingSelector, String s, OsmPrimitive p) {
            if (s != null && matchingSelector instanceof Selector.ChildOrParentSelector) {
                return insertArguments(((Selector.ChildOrParentSelector)matchingSelector).right, s, p);
            } else if (s == null || !(matchingSelector instanceof GeneralSelector)) {
                return s;
            }
            final Matcher m = Pattern.compile("\\{(\\d+)\\.(key|value|tag)\\}").matcher(s);
            final StringBuffer sb = new StringBuffer();
            while (m.find()) {
                final String argument = determineArgument((Selector.GeneralSelector) matchingSelector, Integer.parseInt(m.group(1)), m.group(2), p);
                try {
                    // Perform replacement with null-safe + regex-safe handling
                    m.appendReplacement(sb, String.valueOf(argument).replace("^(", "").replace(")$", ""));
                } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
                    Main.error(tr("Unable to replace argument {0} in {1}: {2}", argument, sb, e.getMessage()));
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
            if (deletion) {
                cmds.add(new DeleteCommand(p));
            }
            return new SequenceCommand(tr("Fix of {0}", getDescriptionForMatchingSelector(p, matchingSelector)), cmds);
        }

        /**
         * Constructs a (localized) message for this deprecation check.
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
                                ? ((Expression) val).evaluate(new Environment().withPrimitive(p))
                                : val
                );
            }
        }

        /**
         * Constructs a (localized) description for this deprecation check.
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
            final Environment env = new Environment().withPrimitive(p);
            return getErrorForPrimitive(p, whichSelectorMatchesEnvironment(env), env);
        }

        TestError getErrorForPrimitive(OsmPrimitive p, Selector matchingSelector, Environment env) {
            if (matchingSelector != null && !errors.isEmpty()) {
                final Command fix = fixPrimitive(p);
                final String description = getDescriptionForMatchingSelector(p, matchingSelector);
                final List<OsmPrimitive> primitives;
                if (env.child != null) {
                    primitives = Arrays.asList(p, env.child);
                } else {
                    primitives = Collections.singletonList(p);
                }
                if (fix != null) {
                    return new FixableTestError(null, getSeverity(), description, null, matchingSelector.toString(), 3000, primitives, fix);
                } else {
                    return new TestError(null, getSeverity(), description, null, matchingSelector.toString(), 3000, primitives);
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
            Set<TagCheck> result = new HashSet<MapCSSTagChecker.TagCheck>();
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
                    for (Condition c : ((AbstractSelector)s).getConditions()) {
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
        public boolean equals(Object obj) {
            return super.equals(obj)
                    || (obj instanceof TagCheck && rule.equals(((TagCheck) obj).rule))
                    || (obj instanceof GroupedMapCSSRule && rule.equals(obj));
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((rule == null) ? 0 : rule.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "MapCSSTagCheckerAndRule [rule=" + rule + "]";
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
                if (Severity.OTHER.equals(check.getSeverity()) && !includeOtherSeverity) {
                    continue;
                }
                final Selector selector = check.whichSelectorMatchesEnvironment(env);
                if (selector != null) {
                    check.rule.declaration.execute(env);
                    final TestError error = check.getErrorForPrimitive(p, selector, env);
                    if (error != null) {
                        error.setTester(new MapCSSTagCheckerAndRule(check.rule));
                        r.add(error);
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
        errors.addAll(getErrorsForPrimitive(p, ValidatorPreference.PREF_OTHER.get()));
    }

    /**
     * Adds a new MapCSS config file from the given URL.
     * @param url The unique URL of the MapCSS config file
     * @throws ParseException if the config file does not match MapCSS syntax
     * @throws IOException if any I/O error occurs
     * @since 7275
     */
    public synchronized void addMapCSS(String url) throws ParseException, IOException {
        CheckParameterUtil.ensureParameterNotNull(url, "url");
        CachedFile cache = new CachedFile(url);
        InputStream zip = cache.findZipEntryInputStream("validator.mapcss", "");
        try (InputStream s = zip != null ? zip : cache.getInputStream()) {
            List<TagCheck> tagchecks = TagCheck.readMapCSS(new BufferedReader(UTFInputStreamReader.create(s)));
            checks.remove(url);
            checks.putAll(url, tagchecks);
            // Check assertions, useful for development of local files
            if (Main.pref.getBoolean("validator.check_assert_local_rules", false) && Utils.isLocalUrl(url)) {
                for (String msg : checkAsserts(tagchecks)) {
                    Main.warn(msg);
                }
            }
        }
    }

    @Override
    public synchronized void initialize() throws Exception {
        checks.clear();
        for (SourceEntry source : new ValidatorTagCheckerRulesPreference.RulePrefHelper().get()) {
            if (!source.active) {
                continue;
            }
            String i = source.url;
            try {
                if (!i.startsWith("resource:")) {
                    Main.info(tr("Adding {0} to tag checker", i));
                } else if (Main.isDebugEnabled()) {
                    Main.debug(tr("Adding {0} to tag checker", i));
                }
                addMapCSS(i);
                if (Main.pref.getBoolean("validator.auto_reload_local_rules", true) && source.isLocal()) {
                    try {
                        Main.fileWatcher.registerValidatorRule(source);
                    } catch (IOException e) {
                        Main.error(e);
                    }
                }
            } catch (IOException ex) {
                Main.warn(tr("Failed to add {0} to tag checker", i));
                Main.warn(ex, false);
            } catch (Exception ex) {
                Main.warn(tr("Failed to add {0} to tag checker", i));
                Main.warn(ex);
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
            if (Main.isDebugEnabled()) {
                Main.debug("Check: "+check);
            }
            for (final Map.Entry<String, Boolean> i : check.assertions.entrySet()) {
                if (Main.isDebugEnabled()) {
                    Main.debug("- Assertion: "+i);
                }
                final OsmPrimitive p = OsmUtils.createPrimitive(i.getKey());
                // Build minimal ordered list of checks to run to test the assertion
                List<Set<TagCheck>> checksToRun = new ArrayList<Set<TagCheck>>();
                Set<TagCheck> checkDependencies = check.getTagCheckDependencies(schecks);
                if (!checkDependencies.isEmpty()) {
                    checksToRun.add(checkDependencies);
                }
                checksToRun.add(Collections.singleton(check));
                // Add primitive to dataset to avoid DataIntegrityProblemException when evaluating selectors
                ds.addPrimitive(p);
                final Collection<TestError> pErrors = getErrorsForPrimitive(p, true, checksToRun);
                if (Main.isDebugEnabled()) {
                    Main.debug("- Errors: "+pErrors);
                }
                final boolean isError = Utils.exists(pErrors, new Predicate<TestError>() {
                    @Override
                    public boolean evaluate(TestError e) {
                        //noinspection EqualsBetweenInconvertibleTypes
                        return e.getTester().equals(check.rule);
                    }
                });
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
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((checks == null) ? 0 : checks.hashCode());
        return result;
    }

    @Override
    public synchronized boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof MapCSSTagChecker))
            return false;
        MapCSSTagChecker other = (MapCSSTagChecker) obj;
        if (checks == null) {
            if (other.checks != null)
                return false;
        } else if (!checks.equals(other.checks))
            return false;
        return true;
    }
}
