package org.openstreetmap.josm.data.validation.tests;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.TextTagParser;

import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MapCSSTagCheckerTest {

    @Before
    public void setUp() throws Exception {
        Main.pref = new Preferences();
    }

    @Test
    public void testNaturalMarsh() throws Exception {

        final List<MapCSSTagChecker.TagCheck> checks = MapCSSTagChecker.TagCheck.readMapCSS(new StringReader("" +
                "*[natural=marsh] {\n" +
                "   throwWarning: tr(\"{0} is deprecated\", \"natural=marsh\");\n" +
                "   fixRemove: \"natural\";\n" +
                "   fixAdd: \"natural=wetland\";\n" +
                "   fixAdd: \"wetland=marsh\";\n" +
                "}"));
        assertThat(checks.size(), is(1));
        final MapCSSTagChecker.TagCheck check = checks.get(0);
        assertThat(check, notNullValue());
        assertThat(check.change.get(0), is(new Tag("natural")));
        assertThat(check.change.get(1), is(new Tag("natural", "wetland")));
        assertThat(check.change.get(2), is(new Tag("wetland", "marsh")));
        assertThat(check.errors.keySet().iterator().next(), is("natural=marsh is deprecated"));
        final Node n1 = new Node();
        n1.put("natural", "marsh");
        assertTrue(check.matchesPrimitive(n1));
        final Node n2 = new Node();
        n2.put("natural", "wood");
        assertFalse(check.matchesPrimitive(n2));
    }

    OsmPrimitive createPrimitiveForAssertion(String assertion) {
        final String[] x = assertion.split("\\s+", 2);
        final OsmPrimitive p = "n".equals(x[0]) || "node".equals(x[0])
                ? new Node()
                : "w".equals(x[0]) || "way".equals(x[0])
                ? new Way()
                : "r".equals(x[0]) || "relation".equals(x[0])
                ? new Relation()
                : null;
        if (p == null) {
            throw new IllegalArgumentException("Expecting n/node/w/way/r/relation, but got " + x[0]);
        }
        for (final Map.Entry<String, String> i : TextTagParser.getValidatedTagsFromText(x[1]).entrySet()) {
            p.put(i.getKey(), i.getValue());
        }
        return p;
    }

    @Test
    public void testCreatePrimitiveForAssertion() throws Exception {
        final OsmPrimitive p = createPrimitiveForAssertion("way name=Foo railway=rail");
        assertTrue(p instanceof Way);
        assertThat(p.keySet().size(), is(2));
        assertThat(p.get("name"), is("Foo"));
        assertThat(p.get("railway"), is("rail"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePrimitiveForAssertionFail() throws Exception {
        final OsmPrimitive p = createPrimitiveForAssertion("noway name=Foo");
    }

    @Test
    public void testInit() throws Exception {
        final MapCSSTagChecker c = new MapCSSTagChecker();
        c.initialize();

        LinkedHashSet<String> assertionErrors = new LinkedHashSet<String>();
        for (final MapCSSTagChecker.TagCheck check : c.checks) {
            for (final Map.Entry<String, Boolean> i : check.assertions.entrySet()) {
                final OsmPrimitive p = createPrimitiveForAssertion(i.getKey());
                if (check.matchesPrimitive(p) != i.getValue()) {
                    final String error = "Expecting test '" + check.getMessage() + "' to " + (i.getValue() ? "" : "not ") + "match " + i.getKey() + ", i.e., " + p.getKeys();
                    System.err.println(error);
                    assertionErrors.add(error);
                }
            }
        }
        assertTrue("not all assertions included in the tests are met", assertionErrors.isEmpty());

    }
}
