// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.CollectionProperty;
import org.openstreetmap.josm.data.validation.FixableTestError;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.gui.widgets.EditableList;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * MapCSS-based tag checker/fixer.
 * @since 6506
 */
public class MapCSSTagChecker extends Test {

    /**
     * Constructs a new {@code MapCSSTagChecker}.
     */
    public MapCSSTagChecker() {
        super(tr("Tag checker (MapCSS based)"), tr("This test checks for errors in tag keys and values."));
    }

    final List<TagCheck> checks = new ArrayList<TagCheck>();

    static class TagCheck implements Predicate<OsmPrimitive> {
        protected final List<Selector> selector;
        protected final List<PrimitiveToTag> change = new ArrayList<PrimitiveToTag>();
        protected final Map<String, String> keyChange = new LinkedHashMap<String, String>();
        protected final List<String> alternatives = new ArrayList<String>();
        protected final Map<String, Severity> errors = new HashMap<String, Severity>();
        protected final Map<String, Boolean> assertions = new HashMap<String, Boolean>();

        TagCheck(List<Selector> selector) {
            this.selector = selector;
        }

        /**
         * A function mapping the matched {@link OsmPrimitive} to a {@link Tag}.
         */
        static abstract class PrimitiveToTag implements Utils.Function<OsmPrimitive, Tag> {

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

        static TagCheck ofMapCSSRule(final MapCSSRule rule) {
            final TagCheck check = new TagCheck(rule.selectors);
            for (Instruction i : rule.declaration) {
                if (i instanceof Instruction.AssignmentInstruction) {
                    final Instruction.AssignmentInstruction ai = (Instruction.AssignmentInstruction) i;
                    final String val = ai.val instanceof Expression
                            ? (String) ((Expression) ai.val).evaluate(new Environment())
                            : ai.val instanceof String
                            ? (String) ai.val
                            : null;
                    if (ai.key.startsWith("throw")) {
                        final Severity severity = Severity.valueOf(ai.key.substring("throw".length()).toUpperCase());
                        check.errors.put(val, severity);
                    } else if ("fixAdd".equals(ai.key)) {
                        final PrimitiveToTag toTag = PrimitiveToTag.ofMapCSSObject(ai.val, false);
                        check.change.add(toTag);
                    } else if ("fixRemove".equals(ai.key)) {
                        CheckParameterUtil.ensureThat(!(ai.val instanceof String) || !val.contains("="), "Unexpected '='. Please only specify the key to remove!");
                        final PrimitiveToTag toTag = PrimitiveToTag.ofMapCSSObject(ai.val, true);
                        check.change.add(toTag);
                    } else if ("fixChangeKey".equals(ai.key) && val != null) {
                        CheckParameterUtil.ensureThat(val.contains("=>"), "Separate old from new key by '=>'!");
                        final String[] x = val.split("=>", 2);
                        check.keyChange.put(x[0].trim(), x[1].trim());
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
            if (check.errors.isEmpty()) {
                throw new RuntimeException("No throwError/throwWarning/throwOther given! You should specify a validation error message for " + rule.selectors);
            } else if (check.errors.size() > 1) {
                throw new RuntimeException("More than one throwError/throwWarning/throwOther given! You should specify a single validation error message for " + rule.selectors);
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
            return new ArrayList<TagCheck>(Utils.transform(source.rules, new Utils.Function<MapCSSRule, TagCheck>() {
                @Override
                public TagCheck apply(MapCSSRule x) {
                    return TagCheck.ofMapCSSRule(x);
                }
            }));
        }

        @Override
        public boolean evaluate(OsmPrimitive primitive) {
            return matchesPrimitive(primitive);
        }

        /**
         * Tests whether the {@link OsmPrimitive} contains a deprecated tag which is represented by this {@code MapCSSTagChecker}.
         *
         * @param primitive the primitive to test
         * @return true when the primitive contains a deprecated tag
         */
        boolean matchesPrimitive(OsmPrimitive primitive) {
            return whichSelectorMatchesPrimitive(primitive) != null;
        }

        Selector whichSelectorMatchesPrimitive(OsmPrimitive primitive) {
            final Environment env = new Environment().withPrimitive(primitive);
            for (Selector i : selector) {
                if (i.matches(env)) {
                    return i;
                }
            }
            return null;
        }

        /**
         * Determines the {@code index}-th key/value/tag (depending on {@code type}) of the {@link Selector.GeneralSelector}.
         */
        static String determineArgument(Selector.GeneralSelector matchingSelector, int index, String type) {
            try {
                final Condition c = matchingSelector.getConditions().get(index);
                final Tag tag = c instanceof Condition.KeyCondition
                        ? ((Condition.KeyCondition) c).asTag()
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
            }
            return null;
        }

        /**
         * Replaces occurrences of {@code {i.key}}, {@code {i.value}}, {@code {i.tag}} in {@code s} by the corresponding
         * key/value/tag of the {@code index}-th {@link Condition} of {@code matchingSelector}.
         */
        static String insertArguments(Selector matchingSelector, String s) {
            if (!(matchingSelector instanceof Selector.GeneralSelector) || s == null) {
                return s;
            }
            final Matcher m = Pattern.compile("\\{(\\d+)\\.(key|value|tag)\\}").matcher(s);
            final StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, determineArgument((Selector.GeneralSelector) matchingSelector, Integer.parseInt(m.group(1)), m.group(2)));
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
            Collection<Command> cmds = new LinkedList<Command>();
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
            return new SequenceCommand(tr("Fix of {0}", getDescriptionForMatchingSelector(matchingSelector)), cmds);
        }

        /**
         * Constructs a (localized) message for this deprecation check.
         *
         * @return a message
         */
        String getMessage() {
            return errors.keySet().iterator().next();
        }

        /**
         * Constructs a (localized) description for this deprecation check.
         *
         * @return a description (possibly with alternative suggestions)
         * @see {@link #getDescriptionForMatchingSelector(Selector)}
         */
        String getDescription() {
            if (alternatives.isEmpty()) {
                return getMessage();
            } else {
                /* I18N: {0} is the test error message and {1} is an alternative */
                return tr("{0}, use {1} instead", getMessage(), Utils.join(tr(" or "), alternatives));
            }
        }

        /**
         * Constructs a (localized) description for this deprecation check
         * where any placeholders are replaced by values of the matched selector.
         *
         * @return a description (possibly with alternative suggestions)
         */
        String getDescriptionForMatchingSelector(Selector matchingSelector) {
            return insertArguments(matchingSelector, getDescription());
        }

        Severity getSeverity() {
            return errors.values().iterator().next();
        }

        @Override
        public String toString() {
            return getDescription();
        }

        /**
         * Constructs a {@link TestError} for the given primitive, or returns null if the primitive does not give rise to an error.
         *
         * @param p the primitive to construct the error for
         * @return an instance of {@link TestError}, or returns null if the primitive does not give rise to an error.
         */
        TestError getErrorForPrimitive(OsmPrimitive p) {
            final Selector matchingSelector = whichSelectorMatchesPrimitive(p);
            if (matchingSelector != null) {
                final Command fix = fixPrimitive(p);
                final String description = getDescriptionForMatchingSelector(matchingSelector);
                if (fix != null) {
                    return new FixableTestError(null, getSeverity(), description, 3000, p, fix);
                } else {
                    return new TestError(null, getSeverity(), description, 3000, p);
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Visiting call for primitives.
     *
     * @param p The primitive to inspect.
     */
    public void visit(OsmPrimitive p) {
        for (TagCheck check : checks) {
            final TestError error = check.getErrorForPrimitive(p);
            if (error != null) {
                error.setTester(this);
                errors.add(error);
            }
        }
    }

    @Override
    public void visit(Node n) {
        visit((OsmPrimitive) n);
    }

    @Override
    public void visit(Way w) {
        visit((OsmPrimitive) w);
    }

    @Override
    public void visit(Relation r) {
        visit((OsmPrimitive) r);
    }

    /**
     * Adds a new MapCSS config file from the given {@code Reader}.
     * @param css The reader
     * @throws ParseException if the config file does not match MapCSS syntax
     */
    public void addMapCSS(Reader css) throws ParseException {
        checks.addAll(TagCheck.readMapCSS(css));
    }

    /**
     * Adds a new MapCSS config file from the given internal filename.
     * @param internalConfigFile the filename in data/validator
     * @throws ParseException if the config file does not match MapCSS syntax
     */
    private void addMapCSS(String internalConfigFile) throws ParseException {
        addMapCSS(new InputStreamReader(getClass().getResourceAsStream("/data/validator/" + internalConfigFile + ".mapcss"), Utils.UTF_8));
    }

    @Override
    public void initialize() throws Exception {
        addMapCSS("deprecated");
        addMapCSS("highway");
        addMapCSS("numeric");
        addMapCSS("religion");
        addMapCSS("relation");
        addMapCSS("combinations");
        addMapCSS("unnecessary");
        addMapCSS("wikipedia");
        addMapCSS("power");
        addMapCSS("geometry");
        for (final String i : sourcesProperty.get()) {
            final String file = new File(i).getAbsolutePath();
            try {
                Main.info(tr("Adding {0} to tag checker", file));
                addMapCSS(new BufferedReader(new InputStreamReader(new FileInputStream(i), Utils.UTF_8)));
            } catch (Exception ex) {
                Main.warn(new RuntimeException(tr("Failed to add {0} to tag checker", file), ex));
            }
        }
    }

    protected EditableList sourcesList;
    protected final CollectionProperty sourcesProperty = new CollectionProperty(
            "validator." + this.getClass().getName() + ".sources", Collections.<String>emptyList());

    @Override
    public void addGui(JPanel testPanel) {
        super.addGui(testPanel);
        sourcesList = new EditableList(tr("TagChecker source"));
        sourcesList.setItems(sourcesProperty.get());
        testPanel.add(new JLabel(tr("Data sources ({0})", "*.mapcss")), GBC.eol().insets(23, 0, 0, 0));
        testPanel.add(sourcesList, GBC.eol().fill(GBC.HORIZONTAL).insets(23, 0, 0, 0));
    }

    @Override
    public boolean ok() {
        sourcesProperty.put(sourcesList.getItems());
        return super.ok();
    }
}
