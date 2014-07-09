// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.FixableTestError;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule.Declaration;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorPreference;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorTagCheckerRulesPreference;
import org.openstreetmap.josm.io.CachedFile;
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
        final public List<Selector> selectors;
        /** MapCSS declaration **/
        final public Declaration declaration;

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

    final MultiMap<String, TagCheck> checks = new MultiMap<>();

    static class TagCheck implements Predicate<OsmPrimitive> {
        protected final GroupedMapCSSRule rule;
        protected final List<PrimitiveToTag> change = new ArrayList<>();
        protected final Map<String, String> keyChange = new LinkedHashMap<>();
        protected final List<String> alternatives = new ArrayList<>();
        protected final Map<Instruction.AssignmentInstruction, Severity> errors = new HashMap<>();
        protected final Map<String, Boolean> assertions = new HashMap<>();

        TagCheck(GroupedMapCSSRule rule) {
            this.rule = rule;
        }

        /**
         * A function mapping the matched {@link OsmPrimitive} to a {@link Tag}.
         */
        abstract static class PrimitiveToTag implements Utils.Function<OsmPrimitive, Tag> {

            private PrimitiveToTag() {
                // Hide implicit public constructor for utility class
            }

            /**
             * Creates a new mapping from an {@code MapCSS} object.
             * In case of an {@link Expression}, that is evaluated on the matched {@link OsmPrimitive}.
             * In case of a {@link String}, that is "compiled" to a {@link Tag} instance.
             */
            static PrimitiveToTag ofMapCSSObject(final Object obj, final boolean keyOnly) {
                if (obj instanceof Expression) {
                    return new PrimitiveToTag() {
                        @Override
                        public Tag apply(OsmPrimitive p) {
                            final String s = (String) ((Expression) obj).evaluate(new Environment().withPrimitive(p));
                            return keyOnly? new Tag(s) : Tag.ofString(s);
                        }
                    };
                } else if (obj instanceof String) {
                    final Tag tag = keyOnly ? new Tag((String) obj) : Tag.ofString((String) obj);
                    return new PrimitiveToTag() {
                        @Override
                        public Tag apply(OsmPrimitive ignore) {
                            return tag;
                        }
                    };
                } else {
                    return null;
                }
            }
        }

        static final String POSSIBLE_THROWS = possibleThrows();

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

        static TagCheck ofMapCSSRule(final GroupedMapCSSRule rule) {
            final TagCheck check = new TagCheck(rule);
            boolean containsSetClassExpression = false;
            for (Instruction i : rule.declaration.instructions) {
                if (i instanceof Instruction.AssignmentInstruction) {
                    final Instruction.AssignmentInstruction ai = (Instruction.AssignmentInstruction) i;
                    if (ai.isSetInstruction) {
                        containsSetClassExpression = true;
                        continue;
                    }
                    final String val = ai.val instanceof Expression
                            ? (String) ((Expression) ai.val).evaluate(new Environment())
                            : ai.val instanceof String
                            ? (String) ai.val
                            : null;
                    if (ai.key.startsWith("throw")) {
                        try {
                            final Severity severity = Severity.valueOf(ai.key.substring("throw".length()).toUpperCase());
                            check.errors.put(ai, severity);
                        } catch (IllegalArgumentException e) {
                            Main.warn("Unsupported "+ai.key+" instruction. Allowed instructions are "+POSSIBLE_THROWS);
                        }
                    } else if ("fixAdd".equals(ai.key)) {
                        final PrimitiveToTag toTag = PrimitiveToTag.ofMapCSSObject(ai.val, false);
                        if (toTag != null) {
                            check.change.add(toTag);
                        } else {
                            Main.warn("Invalid value for "+ai.key+": "+ai.val);
                        }
                    } else if ("fixRemove".equals(ai.key)) {
                        CheckParameterUtil.ensureThat(!(ai.val instanceof String) || !(val != null && val.contains("=")),
                                "Unexpected '='. Please only specify the key to remove!");
                        final PrimitiveToTag toTag = PrimitiveToTag.ofMapCSSObject(ai.val, true);
                        if (toTag != null) {
                            check.change.add(toTag);
                        } else {
                            Main.warn("Invalid value for "+ai.key+": "+ai.val);
                        }
                    } else if ("fixChangeKey".equals(ai.key) && val != null) {
                        CheckParameterUtil.ensureThat(val.contains("=>"), "Separate old from new key by '=>'!");
                        final String[] x = val.split("=>", 2);
                        check.keyChange.put(Tag.removeWhiteSpaces(x[0]), Tag.removeWhiteSpaces(x[1]));
                    } else if ("suggestAlternative".equals(ai.key) && val != null) {
                        check.alternatives.add(val);
                    } else if ("assertMatch".equals(ai.key) && val != null) {
                        check.assertions.put(val, true);
                    } else if ("assertNoMatch".equals(ai.key) && val != null) {
                        check.assertions.put(val, false);
                    } else {
                        throw new RuntimeException("Cannot add instruction " + ai.key + ": " + ai.val + "!");
                    }
                }
            }
            if (check.errors.isEmpty() && !containsSetClassExpression) {
                throw new RuntimeException("No "+POSSIBLE_THROWS+" given! You should specify a validation error message for " + rule.selectors);
            } else if (check.errors.size() > 1) {
                throw new RuntimeException("More than one "+POSSIBLE_THROWS+" given! You should specify a single validation error message for " + rule.selectors);
            }
            return check;
        }

        static List<TagCheck> readMapCSS(Reader css) throws ParseException {
            CheckParameterUtil.ensureParameterNotNull(css, "css");
            return readMapCSS(new MapCSSParser(css));
        }

        static List<TagCheck> readMapCSS(MapCSSParser css) throws ParseException {
            CheckParameterUtil.ensureParameterNotNull(css, "css");
            final MapCSSStyleSource source = new MapCSSStyleSource("");
            css.sheet(source);
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
                result.add(TagCheck.ofMapCSSRule(
                        new GroupedMapCSSRule(map.getValue(), map.getKey())));
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
        static String determineArgument(Selector.GeneralSelector matchingSelector, int index, String type) {
            try {
                final Condition c = matchingSelector.getConditions().get(index);
                final Tag tag = c instanceof Condition.KeyCondition
                        ? ((Condition.KeyCondition) c).asTag()
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
                Main.debug(ignore.getMessage());
            }
            return null;
        }

        /**
         * Replaces occurrences of <code>{i.key}</code>, <code>{i.value}</code>, <code>{i.tag}</code> in {@code s} by the corresponding
         * key/value/tag of the {@code index}-th {@link Condition} of {@code matchingSelector}.
         */
        static String insertArguments(Selector matchingSelector, String s) {
            if (s != null && matchingSelector instanceof Selector.ChildOrParentSelector) {
                return  insertArguments(((Selector.ChildOrParentSelector)matchingSelector).right, s);
            } else if (s == null || !(matchingSelector instanceof GeneralSelector)) {
                return s;
            }
            final Matcher m = Pattern.compile("\\{(\\d+)\\.(key|value|tag)\\}").matcher(s);
            final StringBuffer sb = new StringBuffer();
            while (m.find()) {
                final String argument = determineArgument((Selector.GeneralSelector) matchingSelector, Integer.parseInt(m.group(1)), m.group(2));
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
            if (change.isEmpty() && keyChange.isEmpty()) {
                return null;
            }
            final Selector matchingSelector = whichSelectorMatchesPrimitive(p);
            Collection<Command> cmds = new LinkedList<>();
            for (PrimitiveToTag toTag : change) {
                final Tag tag = toTag.apply(p);
                final String key = insertArguments(matchingSelector, tag.getKey());
                final String value = insertArguments(matchingSelector, tag.getValue());
                cmds.add(new ChangePropertyCommand(p, key, value));
            }
            for (Map.Entry<String, String> i : keyChange.entrySet()) {
                final String oldKey = insertArguments(matchingSelector, i.getKey());
                final String newKey = insertArguments(matchingSelector, i.getValue());
                cmds.add(new ChangePropertyKeyCommand(p, oldKey, newKey));
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
            return insertArguments(matchingSelector, getDescription(p));
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
    }

    /**
     * Obtains all {@link TestError}s for the {@link OsmPrimitive} {@code p}.
     * @param p The OSM primitive
     * @param includeOtherSeverity if {@code true}, errors of severity {@link Severity#OTHER} (info) will also be returned
     * @return all errors for the given primitive, with or without those of "info" severity
     */
    public synchronized Collection<TestError> getErrorsForPrimitive(OsmPrimitive p, boolean includeOtherSeverity) {
        final ArrayList<TestError> r = new ArrayList<>();
        final Environment env = new Environment(p, new MultiCascade(), Environment.DEFAULT_LAYER, null);
        for (Set<TagCheck> schecks : checks.values()) {
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
        try (InputStream s = cache.getInputStream()) {
            List<TagCheck> tagchecks = TagCheck.readMapCSS(new BufferedReader(UTFInputStreamReader.create(s)));
            checks.remove(url);
            checks.putAll(url, tagchecks);
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
                if (i.startsWith("resource:")) {
                    Main.debug(tr("Adding {0} to tag checker", i));
                } else {
                    Main.info(tr("Adding {0} to tag checker", i));
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
