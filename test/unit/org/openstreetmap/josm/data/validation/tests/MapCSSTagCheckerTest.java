package org.openstreetmap.josm.data.validation.tests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.TestUtils;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

public class MapCSSTagCheckerTest {

    @Before
    public void setUp() throws Exception {
        Main.initApplicationPreferences();
    }

    @Test
    public void testNaturalMarsh() throws Exception {

        final List<MapCSSTagChecker.TagCheck> checks = MapCSSTagChecker.TagCheck.readMapCSS(new StringReader("" +
                "*[natural=marsh] {\n" +
                "   throwWarning: tr(\"{0}={1} is deprecated\", \"{0.key}\", tag(\"natural\"));\n" +
                "   fixRemove: \"{0.key}\";\n" +
                "   fixAdd: \"natural=wetland\";\n" +
                "   fixAdd: \"wetland=marsh\";\n" +
                "}"));
        assertThat(checks.size(), is(1));
        final MapCSSTagChecker.TagCheck check = checks.get(0);
        assertThat(check, notNullValue());
        assertThat(check.getDescription(null), is("{0.key}=null is deprecated"));
        assertThat(check.change.get(0).apply(null), is(new Tag("{0.key}")));
        assertThat(check.change.get(1).apply(null), is(new Tag("natural", "wetland")));
        assertThat(check.change.get(2).apply(null), is(new Tag("wetland", "marsh")));
        final Node n1 = new Node();
        n1.put("natural", "marsh");
        assertTrue(check.matchesPrimitive(n1));
        assertThat(check.getErrorForPrimitive(n1).getMessage(), is("natural=marsh is deprecated"));
        assertThat(check.getErrorForPrimitive(n1).getSeverity(), is(Severity.WARNING));
        assertThat(check.fixPrimitive(n1).getDescriptionText(), is("Sequence: Fix of natural=marsh is deprecated"));
        assertThat(((ChangePropertyCommand) check.fixPrimitive(n1).getChildren().iterator().next()).getTags().toString(),
                is("{natural=}"));
        final Node n2 = new Node();
        n2.put("natural", "wood");
        assertFalse(check.matchesPrimitive(n2));
        assertThat(MapCSSTagChecker.TagCheck.insertArguments(check.rule.selectors.get(0), "The key is {0.key} and the value is {0.value}"),
                is("The key is natural and the value is marsh"));
    }

    @Test
    public void testInit() throws Exception {
        final MapCSSTagChecker c = new MapCSSTagChecker();
        c.initialize();

        LinkedHashSet<String> assertionErrors = new LinkedHashSet<String>();
        for (final MapCSSTagChecker.TagCheck check : c.checks) {
            for (final Map.Entry<String, Boolean> i : check.assertions.entrySet()) {
                final OsmPrimitive p = TestUtils.createPrimitive(i.getKey());
                final boolean isError = Utils.exists(c.getErrorsForPrimitive(p, true), new Predicate<TestError>() {
                    @Override
                    public boolean evaluate(TestError e) {
                        //noinspection EqualsBetweenInconvertibleTypes
                        return e.getTester().equals(check.rule);
                    }
                });
                if (isError != i.getValue()) {
                    final String error = MessageFormat.format("Expecting test ''{0}'' (i.e., {1}) to {2} {3} (i.e., {4})",
                            check.getMessage(p), check.rule.selectors, i.getValue() ? "match" : "not match", i.getKey(), p.getKeys());
                    System.err.println(error);
                    assertionErrors.add(error);
                }
            }
        }
        assertTrue("not all assertions included in the tests are met", assertionErrors.isEmpty());

    }
}
