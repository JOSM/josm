// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.ChangePropertyKeyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test.TagTest;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.ParseResult;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Environment.LinkEnvironment;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.ParseException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of {@link MapCSSTagChecker}.
 */
class MapCSSTagCheckerTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().territories().preferences();

    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        MapCSSTagCheckerAsserts.clear();
    }

    static MapCSSTagChecker buildTagChecker(String css) throws ParseException {
        final MapCSSTagChecker test = new MapCSSTagChecker();
        Set<String> errors = new HashSet<>();
        test.checks.putAll("test", MapCSSTagCheckerRule.readMapCSS(new StringReader(css), errors::add).parseChecks);
        assertTrue(errors.isEmpty(), errors::toString);
        return test;
    }

    /**
     * Test {@code natural=marsh}.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testNaturalMarsh() throws ParseException {
        ParseResult result = MapCSSTagCheckerRule.readMapCSS(new StringReader(
                "*[natural=marsh] {\n" +
                "   group: tr(\"deprecated\");\n" +
                "   throwWarning: tr(\"{0}={1} is deprecated\", \"{0.key}\", tag(\"natural\"));\n" +
                "   fixRemove: \"{0.key}\";\n" +
                "   fixAdd: \"natural=wetland\";\n" +
                "   fixAdd: \"wetland=marsh\";\n" +
                "}"));
        final List<MapCSSTagCheckerRule> checks = result.parseChecks;
        assertEquals(1, checks.size());
        assertTrue(result.parseErrors.isEmpty());
        final MapCSSTagCheckerRule check = checks.get(0);
        assertNotNull(check);
        assertEquals("{0.key}=null is deprecated", check.getDescription(null));
        assertEquals("fixRemove: {0.key}", check.fixCommands.get(0).toString());
        assertEquals("fixAdd: natural=wetland", check.fixCommands.get(1).toString());
        assertEquals("fixAdd: wetland=marsh", check.fixCommands.get(2).toString());
        final OsmPrimitive n1 = OsmUtils.createPrimitive("node natural=marsh");
        final OsmPrimitive n2 = OsmUtils.createPrimitive("node natural=wood");
        new DataSet(n1, n2);
        assertTrue(check.test(n1));

        final Collection<TestError> errors = check.getErrorsForPrimitive(n1, check.whichSelectorMatchesPrimitive(n1), new LinkEnvironment(), null);
        assertEquals(1, errors.size());
        TestError err = errors.iterator().next();
        assertEquals("deprecated", err.getMessage());
        assertEquals("natural=marsh is deprecated", err.getDescription());
        assertEquals(Severity.WARNING, err.getSeverity());
        assertEquals("Sequence: Fix of natural=marsh is deprecated", check.fixPrimitive(n1).getDescriptionText());
        assertEquals("{natural=}", ((ChangePropertyCommand) check.fixPrimitive(n1).getChildren().iterator().next()).getTags().toString());
        assertFalse(check.test(n2));
        assertEquals("The key is natural and the value is marsh",
                MapCSSTagCheckerRule.insertArguments(check.rule.selectors.get(0), "The key is {0.key} and the value is {0.value}", null));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/10913">Bug #10913</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testTicket10913() throws ParseException {
        final OsmPrimitive p = TestUtils.addFakeDataSet(TestUtils.newWay("highway=tertiary construction=yes"));
        final MapCSSTagCheckerRule check = MapCSSTagCheckerRule.readMapCSS(new StringReader("way {" +
                "throwError: \"error\";" +
                "fixChangeKey: \"highway => construction\";\n" +
                "fixAdd: \"highway=construction\";\n" +
                "}")).parseChecks.get(0);
        final Command command = check.fixPrimitive(p);
        assertTrue(command instanceof SequenceCommand);
        final Iterator<PseudoCommand> it = command.getChildren().iterator();
        assertTrue(it.next() instanceof ChangePropertyKeyCommand);
        assertTrue(it.next() instanceof ChangePropertyCommand);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/9782">Bug #9782</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testTicket9782() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker("*[/.+_name/][!name] {" +
                "throwWarning: tr(\"has {0} but not {1}\", \"{0.key}\", \"{1.key}\");}");
        final OsmPrimitive p = OsmUtils.createPrimitive("way alt_name=Foo");
        final Collection<TestError> errors = test.getErrorsForPrimitive(p, false);
        assertEquals(1, errors.size());
        assertEquals("has alt_name but not name", errors.iterator().next().getMessage());
        assertEquals("3000_has alt_name but not name", errors.iterator().next().getIgnoreSubGroup());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/10859">Bug #10859</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testTicket10859() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker("way[highway=footway][foot?!] {\n" +
                "  throwWarning: tr(\"{0} used with {1}\", \"{0.value}\", \"{1.tag}\");}");
        final OsmPrimitive p = OsmUtils.createPrimitive("way highway=footway foot=no");
        final Collection<TestError> errors = test.getErrorsForPrimitive(p, false);
        assertEquals(1, errors.size());
        assertEquals("footway used with foot=no", errors.iterator().next().getMessage());
        assertEquals("3000_footway used with foot=no", errors.iterator().next().getIgnoreSubGroup());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13630">Bug #13630</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testTicket13630() throws ParseException {
        ParseResult result = MapCSSTagCheckerRule.readMapCSS(new StringReader(
                "node[crossing=zebra] {fixRemove: \"crossing=zebra\";}"));
        assertTrue(result.parseChecks.isEmpty());
        assertEquals(1, result.parseErrors.size());
    }

    /**
     * Unit test of {@code min-josm-version} processing.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testPreprocessing() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker(
                "@supports (min-josm-version: 0) { *[foo] { throwWarning: \"!\"; } }\n" +
                "@supports (min-josm-version: 2147483647) { *[bar] { throwWarning: \"!\"; } }");
        assertEquals(1, test.getErrorsForPrimitive(OsmUtils.createPrimitive("way foo=1"), false).size());
        assertEquals(0, test.getErrorsForPrimitive(OsmUtils.createPrimitive("way bar=1"), false).size());
    }

    /**
     * Unit test of {@link MapCSSTagChecker#initialize}.
     * @throws Exception if an error occurs
     */
    @Test
    void testInit() throws Exception {
        Logging.clearLastErrorAndWarnings();
        MapCSSTagChecker c = new MapCSSTagChecker();
        c.initialize();

        assertTrue(Logging.getLastErrorAndWarnings().isEmpty(), "no warnings/errors are logged");

        // to trigger MapCSSStyleIndex code
        Node node = new Node(new LatLon(12, 34));
        node.put("amenity", "drinking_water");
        assertTrue(c.getErrorsForPrimitive(node, false).isEmpty());
    }

    /**
     * Unit test for all {@link TagTest} assertions.
     * @throws Exception if an error occurs
     */
    @Test
    void testAssertions() throws Exception {
        MapCSSTagChecker c = new MapCSSTagChecker();
        Set<String> assertionErrors = new LinkedHashSet<>();

        // initialize
        for (ExtendedSourceEntry entry : ValidatorPrefHelper.INSTANCE.getDefault()) {
            c.addMapCSS(entry.url, assertionErrors::add);
        }

        for (String msg : assertionErrors) {
            Logging.error(msg);
        }
        assertTrue(assertionErrors.isEmpty(), "not all assertions included in the tests are met");
    }

    /**
     * Checks that assertions work for country-specific checks.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testAssertInsideCountry() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker(
                "node[amenity=parking][inside(\"BR\")] {\n" +
                "  throwWarning: \"foo\";\n" +
                "  assertMatch: \"node amenity=parking\";\n" +
                "  assertNoMatch: \"node amenity=restaurant\";\n" +
                "}");
        assertNotNull(test);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17058">Bug #17058</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testTicket17058() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker(
                "*[name =~ /(?i).*Straße.*/][inside(\"LI,CH\")] {\n" +
                "  throwError: tr(\"street name contains ß\");\n" +
                "  assertMatch: \"way name=Hauptstraße\";\n" +
                "  assertNoMatch: \"way name=Hauptstrasse\";\n" +
                "}");
        assertNotNull(test);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13762">Bug #13762</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testTicket13762() throws ParseException {
        final ParseResult parseResult = MapCSSTagCheckerRule.readMapCSS(new StringReader("" +
                "meta[lang=de] {\n" +
                "    title: \"Deutschlandspezifische Regeln\";" +
                "}"));
        assertTrue(parseResult.parseErrors.isEmpty());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14287">Bug #14287</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket14287() throws Exception {
        final MapCSSTagChecker test = buildTagChecker(
                "node[amenity=parking] ∈ *[amenity=parking] {" +
                "  throwWarning: tr(\"{0} inside {1}\", \"amenity=parking\", \"amenity=parking\");" +
                "}");
        try (InputStream is = TestUtils.getRegressionDataStream(14287, "example.osm")) {
            test.visit(OsmReader.parseDataSet(is, null).allPrimitives());
            assertEquals(6, test.getErrors().size());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17053">Bug #17053</a>.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testTicket17053() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker(
                "way[highway=cycleway][cycleway=track] {\n" +
                "   throwWarning: tr(\"{0} with {1}\", \"{0.tag}\", \"{1.tag}\");\n" +
                "   -osmoseItemClassLevel: \"3032/30328/2\";\n" +
                "   -osmoseTags: list(\"tag\", \"highway\", \"cycleway\");\n" +
                "   fixRemove: \"cycleway\";\n" +
                "}");
        assertEquals(1, test.checks.size());
        MapCSSTagCheckerRule check = test.checks.get("test").iterator().next();
        assertEquals(1, check.fixCommands.size());
        assertEquals(2, check.rule.declaration.instructions.size());
    }

    private void doTestNaturalWood(int ticket, String filename, int errorsCount, int setsCount) throws Exception {
        final MapCSSTagChecker test = buildTagChecker(
                "area:closed:areaStyle[tag(\"natural\") = parent_tag(\"natural\")] ⧉ area:closed:areaStyle[natural] {" +
                "  throwWarning: tr(\"Overlapping Identical Natural Areas\");" +
                "}");
        final MapCSSStyleSource style = new MapCSSStyleSource(
                "area[natural=wood] {" +
                "    fill-color: woodarea#008000;" +
                "}");
        MapPaintStyles.addStyle(style);
        try (InputStream is = TestUtils.getRegressionDataStream(ticket, filename)) {
            test.visit(OsmReader.parseDataSet(is, null).allPrimitives());
            List<TestError> errors = test.getErrors();
            assertEquals(errorsCount, errors.size());
            Set<Set<IPrimitive>> primitives = new HashSet<>();
            for (TestError e : errors) {
                primitives.add(new HashSet<>(e.getPrimitives()));
            }
            assertEquals(setsCount, primitives.size());
        } finally {
            MapPaintStyles.removeStyle(style);
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/12627">Bug #12627</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket12627() throws Exception {
        doTestNaturalWood(12627, "overlapping.osm", 1, 1);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14289">Bug #14289</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket14289() throws Exception {
        doTestNaturalWood(14289, "example2.osm", 3, 3);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/15641">Bug #15641</a>.
     * @throws ParseException if an error occurs
     */
    @Test
    void testTicket15641() throws ParseException {
        assertNotNull(buildTagChecker(
                "relation[type=public_transport][public_transport=stop_area_group] > way {" +
                "  throwWarning: eval(count(parent_tags(public_transport)));" +
                "}"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17358">Bug #17358</a>.
     * @throws ParseException if an error occurs
     */
    @Test
    void testTicket17358() throws ParseException {
        final Collection<TestError> errors = buildTagChecker(
                "*[/^name/=~/Test/]{" +
                "  throwWarning: \"Key value match\";" +
                "}").getErrorsForPrimitive(OsmUtils.createPrimitive("way name=Test St"), false);
        assertEquals(1, errors.size());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17695">Bug #17695</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket17695() throws Exception {
        final MapCSSTagChecker test = buildTagChecker(
                "*[building] ∈  *[building] {" +
                "throwWarning: tr(\"Building inside building\");" +
                "}");
        try (InputStream is = TestUtils.getRegressionDataStream(17695, "bib2.osm")) {
            test.visit(OsmReader.parseDataSet(is, null).allPrimitives());
            List<TestError> errors = test.getErrors();
            assertEquals(6, errors.size());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13165">Bug #13165</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket13165() throws Exception {
        final MapCSSTagChecker test = buildTagChecker(
                "area:closed[tag(\"landuse\") = parent_tag(\"landuse\")] ⧉ area:closed[landuse] {"
                        + "throwWarning: tr(\"Overlapping Identical Landuses\");"
                        + "}");
        try (InputStream is = TestUtils.getRegressionDataStream(13165, "13165.osm")) {
            test.visit(OsmReader.parseDataSet(is, null).allPrimitives());
            List<TestError> errors = test.getErrors();
            assertEquals(3, errors.size());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13165">Bug #13165</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket13165IncompleteMP() throws Exception {
        final MapCSSTagChecker test = buildTagChecker(
                "area:closed[tag(\"landuse\") = parent_tag(\"landuse\")] ⧉ area:closed[landuse] {"
                        + "throwWarning: tr(\"Overlapping Identical Landuses\");"
                        + "}");
        try (InputStream is = TestUtils.getRegressionDataStream(13165, "13165-incomplete.osm.bz2")) {
            test.visit(OsmReader.parseDataSet(is, null).allPrimitives());
            List<TestError> errors = test.getErrors();
            assertEquals(3, errors.size());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/19053">Bug #19053</a>.
     * Mapcss rule with group.
     * @throws ParseException if a parsing error occurs
     */
    @Test
    void testTicket19053() throws ParseException {
        final MapCSSTagChecker test = buildTagChecker(
                "*[ele][ele =~ /^-?[0-9]+\\.[0-9][0-9][0-9]+$/] {"
                        + "throwWarning: tr(\"{0}\",\"{0.tag}\");"
                        + "group: tr(\"Unnecessary amount of decimal places\");" + "}");
        final OsmPrimitive p = OsmUtils.createPrimitive("node ele=12.123456");
        final Collection<TestError> errors = test.getErrorsForPrimitive(p, false);
        assertEquals(1, errors.size());
        assertEquals("Unnecessary amount of decimal places", errors.iterator().next().getMessage());
        assertEquals("3000_ele=12.123456", errors.iterator().next().getIgnoreSubGroup());
        assertEquals("3000_Unnecessary amount of decimal places", errors.iterator().next().getIgnoreGroup());
    }

}
