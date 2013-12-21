package org.openstreetmap.josm.data.validation.tests;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Tag;

import java.io.StringReader;
import java.util.List;

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

    @Test
    public void testInit() throws Exception {
        final MapCSSTagChecker c = new MapCSSTagChecker();
        c.initialize();
    }
}
