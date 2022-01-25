// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.TagCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tag check.
 */
final class MapCSSTagCheckerRule implements Predicate<OsmPrimitive> {
    /**
     * The selector of this {@code TagCheck}
     */
    final MapCSSRule rule;
    /**
     * Commands to apply in order to fix a matching primitive
     */
    final List<MapCSSTagCheckerFixCommand> fixCommands;
    /**
     * Tags (or arbitrary strings) of alternatives to be presented to the user
     */
    final List<String> alternatives;
    /**
     * An {@link org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.AssignmentInstruction}-{@link Severity} pair.
     * Is evaluated on the matching primitive to give the error message. Map is checked to contain exactly one element.
     */
    final Map<Instruction.AssignmentInstruction, Severity> errors;
    /**
     * MapCSS Classes to set on matching primitives
     */
    final Collection<String> setClassExpressions;
    /**
     * Denotes whether the object should be deleted for fixing it
     */
    boolean deletion;
    /**
     * A string used to group similar tests
     */
    String group;

    MapCSSTagCheckerRule(MapCSSRule rule) {
        this.rule = rule;
        this.fixCommands = new ArrayList<>();
        this.alternatives = new ArrayList<>();
        this.errors = new HashMap<>();
        this.setClassExpressions = new HashSet<>();
    }

    MapCSSTagCheckerRule(MapCSSTagCheckerRule check) {
        this.rule = check.rule;
        this.fixCommands = Utils.toUnmodifiableList(check.fixCommands);
        this.alternatives = Utils.toUnmodifiableList(check.alternatives);
        this.errors = Utils.toUnmodifiableMap(check.errors);
        this.setClassExpressions = Utils.toUnmodifiableList(check.setClassExpressions);
        this.deletion = check.deletion;
        this.group = check.group;
    }

    MapCSSTagCheckerRule toImmutable() {
        return new MapCSSTagCheckerRule(this);
    }

    private static final String POSSIBLE_THROWS = "throwError/throwWarning/throwOther";

    static MapCSSTagCheckerRule ofMapCSSRule(final MapCSSRule rule, Consumer<String> assertionConsumer) throws IllegalDataException {
        final MapCSSTagCheckerRule check = new MapCSSTagCheckerRule(rule);
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
                        check.fixCommands.add(MapCSSTagCheckerFixCommand.fixAdd(ai.val));
                    } else if ("fixRemove".equals(ai.key)) {
                        CheckParameterUtil.ensureThat(!(ai.val instanceof String) || !(val != null && val.contains("=")),
                                "Unexpected '='. Please only specify the key to remove in: " + ai);
                        check.fixCommands.add(MapCSSTagCheckerFixCommand.fixRemove(ai.val));
                    } else if (val != null && "fixChangeKey".equals(ai.key)) {
                        CheckParameterUtil.ensureThat(val.contains("=>"), "Separate old from new key by '=>'!");
                        final String[] x = val.split("=>", 2);
                        final String oldKey = Utils.removeWhiteSpaces(x[0]);
                        final String newKey = Utils.removeWhiteSpaces(x[1]);
                        check.fixCommands.add(MapCSSTagCheckerFixCommand.fixChangeKey(oldKey, newKey));
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
                    "No " + POSSIBLE_THROWS + " given! You should specify a validation error message for " + rule.selectors);
        } else if (check.errors.size() > 1) {
            throw new IllegalDataException(
                    "More than one " + POSSIBLE_THROWS + " given! You should specify a single validation error message for "
                            + rule.selectors);
        }
        if (assertionConsumer != null) {
            MapCSSTagCheckerAsserts.checkAsserts(check, assertions, assertionConsumer);
        }
        return check.toImmutable();
    }

    static MapCSSTagChecker.ParseResult readMapCSS(Reader css) throws ParseException {
        return readMapCSS(css, null);
    }

    static MapCSSTagChecker.ParseResult readMapCSS(Reader css, Consumer<String> assertionConsumer) throws ParseException {
        CheckParameterUtil.ensureParameterNotNull(css, "css");

        final MapCSSStyleSource source = new MapCSSStyleSource("");
        final MapCSSParser preprocessor = new MapCSSParser(css, MapCSSParser.LexicalState.PREPROCESSOR);
        try (StringReader mapcss = new StringReader(preprocessor.pp_root(source))) {
            new MapCSSParser(mapcss, MapCSSParser.LexicalState.DEFAULT).sheet(source);
        }
        // Ignore "meta" rule(s) from external rules of JOSM wiki
        source.removeMetaRules();
        List<MapCSSTagCheckerRule> parseChecks = new ArrayList<>();
        for (MapCSSRule rule : source.rules) {
            try {
                parseChecks.add(MapCSSTagCheckerRule.ofMapCSSRule(rule, assertionConsumer));
            } catch (IllegalDataException e) {
                Logging.error("Cannot add MapCSS rule: " + e.getMessage());
                source.logError(e);
            }
        }
        return new MapCSSTagChecker.ParseResult(parseChecks, source.getErrors());
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
     * {@link org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector}.
     *
     * @param matchingSelector matching selector
     * @param index            index
     * @param type             selector type ("key", "value" or "tag")
     * @param p                OSM primitive
     * @return argument value, can be {@code null}
     */
    static String determineArgument(Selector.GeneralSelector matchingSelector, int index, String type, OsmPrimitive p) {
        try {
            final Condition c = matchingSelector.getConditions().get(index);
            final Tag tag = c instanceof TagCondition
                    ? ((TagCondition) c).asTag(p)
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
     *
     * @param matchingSelector matching selector
     * @param s                any string
     * @param p                OSM primitive
     * @return string with arguments inserted
     */
    static String insertArguments(Selector matchingSelector, String s, OsmPrimitive p) {
        if (s != null && matchingSelector instanceof Selector.ChildOrParentSelector) {
            return insertArguments(((Selector.ChildOrParentSelector) matchingSelector).right, s, p);
        } else if (s == null || !(matchingSelector instanceof Selector.GeneralSelector)) {
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
        if (p.getDataSet() == null || (fixCommands.isEmpty() && !deletion)) {
            return null;
        }
        try {
            final Selector matchingSelector = whichSelectorMatchesPrimitive(p);
            Collection<Command> cmds = fixCommands.stream()
                    .map(fixCommand -> fixCommand.createCommand(p, matchingSelector))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (deletion && !p.isDeleted()) {
                cmds.add(new DeleteCommand(p));
            }
            return cmds.isEmpty() ? null
                    : new SequenceCommand(tr("Fix of {0}", getDescriptionForMatchingSelector(p, matchingSelector)), cmds);
        } catch (IllegalArgumentException e) {
            Logging.error(e);
            return null;
        }
    }

    /**
     * Constructs a (localized) message for this deprecation check.
     *
     * @param p OSM primitive
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
     *
     * @param p OSM primitive
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
     * @param p                OSM primitive
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
     * @param p                the primitive to construct the error for
     * @param matchingSelector the matching selector (e.g., obtained via {@link #whichSelectorMatchesPrimitive})
     * @param env              the environment
     * @param tester           the tester
     * @return an instance of {@link TestError}, or returns null if the primitive does not give rise to an error.
     */
    List<TestError> getErrorsForPrimitive(OsmPrimitive p, Selector matchingSelector, Environment env, Test tester) {
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
