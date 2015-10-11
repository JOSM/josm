// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.TagCheck;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;

/**
 * JUnit Test of {@link MapCSSTagChecker}.
 */
public class MapCSSTagCheckerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    static MapCSSTagChecker buildTagChecker(String css) throws ParseException {
        final MapCSSTagChecker test = new MapCSSTagChecker();
        test.checks.putAll("test", TagCheck.readMapCSS(new StringReader(css)));
        return test;
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
        assertEquals(1, checks.size());
        final MapCSSTagChecker.TagCheck check = checks.get(0);
        assertNotNull(check);
        assertEquals("{0.key}=null is deprecated", check.getDescription(null));
        assertEquals("fixRemove: {0.key}", check.fixCommands.get(0).toString());
        assertEquals("fixAdd: natural=wetland", check.fixCommands.get(1).toString());
        assertEquals("fixAdd: wetland=marsh", check.fixCommands.get(2).toString());
        final Node n1 = new Node();
        n1.put("natural", "marsh");
        assertTrue(check.evaluate(n1));
        assertEquals("natural=marsh is deprecated", check.getErrorForPrimitive(n1).getMessage());
        assertEquals(Severity.WARNING, check.getErrorForPrimitive(n1).getSeverity());
        assertEquals("Sequence: Fix of natural=marsh is deprecated", check.fixPrimitive(n1).getDescriptionText());
        assertEquals("{natural=}", ((ChangePropertyCommand) check.fixPrimitive(n1).getChildren().iterator().next()).getTags().toString());
        final Node n2 = new Node();
        n2.put("natural", "wood");
        assertFalse(check.evaluate(n2));
        assertEquals("The key is natural and the value is marsh",
                MapCSSTagChecker.TagCheck.insertArguments(check.rule.selectors.get(0), "The key is {0.key} and the value is {0.value}", null));
    }

    @Test
    public void test10913() throws Exception {
        final OsmPrimitive p = OsmUtils.createPrimitive("way highway=tertiary construction=yes");
        final TagCheck check = TagCheck.readMapCSS(new StringReader("way {" +
                "throwError: \"error\";" +
                "fixChangeKey: \"highway => construction\";\n" +
                "fixAdd: \"highway=construction\";\n" +
                "}")).get(0);
        final Command command = check.fixPrimitive(p);
        assertTrue(command instanceof SequenceCommand);
        final Iterator<PseudoCommand> it = command.getChildren().iterator();
        assertTrue(it.next() instanceof ChangePropertyKeyCommand);
        assertTrue(it.next() instanceof ChangePropertyCommand);
    }

    @Test
    public void test9782() throws Exception {
        final MapCSSTagChecker test = buildTagChecker("*[/.+_name/][!name] {" +
                "throwWarning: tr(\"has {0} but not {1}\", \"{0.key}\", \"{1.key}\");}");
        final OsmPrimitive p = OsmUtils.createPrimitive("way alt_name=Foo");
        final Collection<TestError> errors = test.getErrorsForPrimitive(p, false);
        assertEquals(1, errors.size());
        assertEquals("has alt_name but not name", errors.iterator().next().getMessage());
        assertEquals("3000_*[.+_name][!name]", errors.iterator().next().getIgnoreSubGroup());
    }

    @Test
    public void test10859() throws Exception {
        final MapCSSTagChecker test = buildTagChecker("way[highway=footway][foot?!] {\n" +
                "  throwWarning: tr(\"{0} used with {1}\", \"{0.value}\", \"{1.tag}\");}");
        final OsmPrimitive p = OsmUtils.createPrimitive("way highway=footway foot=no");
        final Collection<TestError> errors = test.getErrorsForPrimitive(p, false);
        assertEquals(1, errors.size());
        assertEquals("footway used with foot=no", errors.iterator().next().getMessage());
        assertEquals("3000_way[highway=footway][foot]", errors.iterator().next().getIgnoreSubGroup());
    }

    @Test
    public void testPreprocessing() throws Exception {
        final MapCSSTagChecker test = buildTagChecker("" +
                "@media (min-josm-version: 1) { *[foo] { throwWarning: \"!\"; } }\n" +
                "@media (min-josm-version: 2147483647) { *[bar] { throwWarning: \"!\"; } }\n");
        assertEquals(1, test.getErrorsForPrimitive(OsmUtils.createPrimitive("way foo=1"), false).size());
        assertEquals(0, test.getErrorsForPrimitive(OsmUtils.createPrimitive("way bar=1"), false).size());
    }

    @Test
    public void testInit() throws Exception {
        MapCSSTagChecker c = new MapCSSTagChecker();
        c.initialize();

        Set<String> assertionErrors = new LinkedHashSet<>();
        for (Set<TagCheck> schecks : c.checks.values()) {
            assertionErrors.addAll(c.checkAsserts(schecks));
        }
        for (String msg : assertionErrors) {
            Main.error(msg);
        }
        assertTrue("not all assertions included in the tests are met", assertionErrors.isEmpty());
    }
}
