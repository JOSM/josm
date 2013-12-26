// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.FixableTestError;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Expression;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSRule;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

/**
 * MapCSS-based tag checker/fixer.
 * @since 6506
 */
public class MapCSSTagChecker extends Test {

    /**
     * Constructs a new {@code MapCSSTagChecker}.
     */
    public MapCSSTagChecker() {
        super(tr("Tag checker (new)"), tr("This test checks for errors in tag keys and values."));
    }

    final List<TagCheck> checks = new ArrayList<TagCheck>();

    static class TagCheck implements Predicate<OsmPrimitive> {
        protected final List<Selector> selector;
        protected final List<PrimitiveToTag> change = new ArrayList<PrimitiveToTag>();
        protected final Map<String, String> keyChange = new LinkedHashMap<String, String>();
        protected final List<Tag> alternatives = new ArrayList<Tag>();
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
                        check.alternatives.add(val.contains("=") ? Tag.ofString(val) : new Tag(val));
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
            final Environment env = new Environment().withPrimitive(primitive);
            for (Selector i : selector) {
                if (i.matches(env)) {
                    return true;
                }
            }
            return false;
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
            Collection<Command> cmds = new LinkedList<Command>();
            for (PrimitiveToTag toTag : change) {
                final Tag tag = toTag.apply(p);
                cmds.add(new ChangePropertyCommand(p, tag.getKey(), tag.getValue()));
            }
            for (Map.Entry<String, String> i : keyChange.entrySet()) {
                cmds.add(new ChangePropertyKeyCommand(p, i.getKey(), i.getValue()));
            }
            return new SequenceCommand(tr("Fix of {0}", getDescription()), cmds);
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
         */
        String getDescription() {
            if (alternatives.isEmpty()) {
                return getMessage();
            } else {
                /* I18N: {0} is the test error message and {1} is an alternative */
                return tr("{0}, use {1} instead", getMessage(), Utils.join(tr(" or "), alternatives));
            }
        }

        Severity getSeverity() {
            return errors.values().iterator().next();
        }

    }

    /**
     * Visiting call for primitives.
     *
     * @param p The primitive to inspect.
     */
    public void visit(OsmPrimitive p) {
        for (TagCheck check : checks) {
            if (check.matchesPrimitive(p)) {
                final Command fix = check.fixPrimitive(p);
                if (fix != null) {
                    errors.add(new FixableTestError(this, check.getSeverity(), check.getDescription(), 3000, p, fix));
                } else {
                    errors.add(new TestError(this, check.getSeverity(), check.getDescription(), 3000, p));
                }
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
     * @throws UnsupportedEncodingException if UTF-8 charset is not supported on the platform
     */
    private void addMapCSS(String internalConfigFile) throws ParseException, UnsupportedEncodingException {
        addMapCSS(new InputStreamReader(getClass().getResourceAsStream("/data/validator/"+internalConfigFile), "UTF-8"));
    }

    @Override
    public void initialize() throws Exception {
        addMapCSS("deprecated.mapcss");
        addMapCSS("highway.mapcss");
        addMapCSS("numeric.mapcss");
        addMapCSS("religion.mapcss");
        addMapCSS("relation.mapcss");
    }
}
