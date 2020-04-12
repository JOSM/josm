// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.ExactKeyValue;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.date.DateUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link SearchCompiler}.
 */
public class SearchCompilerTest {

    /**
     * We need prefs for this. We access preferences when creating OSM primitives.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().timeout(30000);

    /**
     * Rule to assert exception message.
     */
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    private static final class SearchContext {
        final DataSet ds = new DataSet();
        final Node n1 = new Node(LatLon.ZERO);
        final Node n2 = new Node(new LatLon(5, 5));
        final Way w1 = new Way();
        final Way w2 = new Way();
        final Relation r1 = new Relation();
        final Relation r2 = new Relation();

        private final Match m;
        private final Match n;

        private SearchContext(String state) throws SearchParseError {
            m = SearchCompiler.compile(state);
            n = SearchCompiler.compile('-' + state);
            ds.addPrimitive(n1);
            ds.addPrimitive(n2);
            w1.addNode(n1);
            w1.addNode(n2);
            w2.addNode(n1);
            w2.addNode(n2);
            ds.addPrimitive(w1);
            ds.addPrimitive(w2);
            r1.addMember(new RelationMember("", w1));
            r1.addMember(new RelationMember("", w2));
            r2.addMember(new RelationMember("", w1));
            r2.addMember(new RelationMember("", w2));
            ds.addPrimitive(r1);
            ds.addPrimitive(r2);
        }

        private void match(OsmPrimitive p, boolean cond) {
            if (cond) {
                assertTrue(p.toString(), m.match(p));
                assertFalse(p.toString(), n.match(p));
            } else {
                assertFalse(p.toString(), m.match(p));
                assertTrue(p.toString(), n.match(p));
            }
        }
    }

    private static OsmPrimitive newPrimitive(String key, String value) {
        final Node p = new Node();
        p.put(key, value);
        return p;
    }

    /**
     * Search anything.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testAny() throws SearchParseError {
        final SearchCompiler.Match c = SearchCompiler.compile("foo");
        assertTrue(c.match(newPrimitive("foobar", "true")));
        assertTrue(c.match(newPrimitive("name", "hello-foo-xy")));
        assertFalse(c.match(newPrimitive("name", "X")));
        assertEquals("foo", c.toString());
    }

    /**
     * Search by equality key=value.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testEquals() throws SearchParseError {
        final SearchCompiler.Match c = SearchCompiler.compile("foo=bar");
        assertFalse(c.match(newPrimitive("foobar", "true")));
        assertTrue(c.match(newPrimitive("foo", "bar")));
        assertFalse(c.match(newPrimitive("fooX", "bar")));
        assertFalse(c.match(newPrimitive("foo", "barX")));
        assertEquals("foo=bar", c.toString());
    }

    /**
     * Search by regular expression: key~value.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testRegexp() throws SearchParseError {
        final SearchCompiler.Match c = SearchCompiler.compile("foo~[Bb]a[rz]");
        assertFalse(c.match(newPrimitive("foobar", "true")));
        assertFalse(c.match(newPrimitive("foo", "foo")));
        assertTrue(c.match(newPrimitive("foo", "bar")));
        assertTrue(c.match(newPrimitive("foo", "baz")));
        assertTrue(c.match(newPrimitive("foo", "Baz")));
        assertEquals("foo=[Bb]a[rz]", c.toString());
    }

    /**
     * Search by comparison.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testCompare() throws SearchParseError {
        final SearchCompiler.Match c1 = SearchCompiler.compile("start_date>1950");
        assertTrue(c1.match(newPrimitive("start_date", "1950-01-01")));
        assertTrue(c1.match(newPrimitive("start_date", "1960")));
        assertFalse(c1.match(newPrimitive("start_date", "1950")));
        assertFalse(c1.match(newPrimitive("start_date", "1000")));
        assertTrue(c1.match(newPrimitive("start_date", "101010")));

        final SearchCompiler.Match c2 = SearchCompiler.compile("start_date<1960");
        assertTrue(c2.match(newPrimitive("start_date", "1950-01-01")));
        assertFalse(c2.match(newPrimitive("start_date", "1960")));
        assertTrue(c2.match(newPrimitive("start_date", "1950")));
        assertTrue(c2.match(newPrimitive("start_date", "1000")));
        assertTrue(c2.match(newPrimitive("start_date", "200")));

        final SearchCompiler.Match c3 = SearchCompiler.compile("name<I");
        assertTrue(c3.match(newPrimitive("name", "Alpha")));
        assertFalse(c3.match(newPrimitive("name", "Sigma")));

        final SearchCompiler.Match c4 = SearchCompiler.compile("\"start_date\"<1960");
        assertTrue(c4.match(newPrimitive("start_date", "1950-01-01")));
        assertFalse(c4.match(newPrimitive("start_date", "2000")));

        final SearchCompiler.Match c5 = SearchCompiler.compile("height>180");
        assertTrue(c5.match(newPrimitive("height", "200")));
        assertTrue(c5.match(newPrimitive("height", "99999")));
        assertFalse(c5.match(newPrimitive("height", "50")));
        assertFalse(c5.match(newPrimitive("height", "-9999")));
        assertFalse(c5.match(newPrimitive("height", "fixme")));

        final SearchCompiler.Match c6 = SearchCompiler.compile("name>C");
        assertTrue(c6.match(newPrimitive("name", "Delta")));
        assertFalse(c6.match(newPrimitive("name", "Alpha")));
    }

    /**
     * Search by nth.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testNth() throws SearchParseError {
        final DataSet dataSet = new DataSet();
        final Way way = new Way();
        final Node node0 = new Node(new LatLon(1, 1));
        final Node node1 = new Node(new LatLon(2, 2));
        final Node node2 = new Node(new LatLon(3, 3));
        dataSet.addPrimitive(way);
        dataSet.addPrimitive(node0);
        dataSet.addPrimitive(node1);
        dataSet.addPrimitive(node2);
        way.addNode(node0);
        way.addNode(node1);
        way.addNode(node2);
        assertFalse(SearchCompiler.compile("nth:2").match(node1));
        assertTrue(SearchCompiler.compile("nth:1").match(node1));
        assertFalse(SearchCompiler.compile("nth:0").match(node1));
        assertTrue(SearchCompiler.compile("nth:0").match(node0));
        assertTrue(SearchCompiler.compile("nth:2").match(node2));
        assertTrue(SearchCompiler.compile("nth:-1").match(node2));
        assertTrue(SearchCompiler.compile("nth:-2").match(node1));
        assertTrue(SearchCompiler.compile("nth:-3").match(node0));
    }

    /**
     * Search by negative nth.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testNthParseNegative() throws SearchParseError {
        assertEquals("Nth{nth=-1, modulo=false}", SearchCompiler.compile("nth:-1").toString());
    }

    /**
     * Search by modified status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testModified() throws SearchParseError {
        SearchContext sc = new SearchContext("modified");
        // Not modified but new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertFalse(p.toString(), p.isModified());
            assertTrue(p.toString(), p.isNewOrUndeleted());
            sc.match(p, true);
        }
        // Modified and new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            p.setModified(true);
            assertTrue(p.toString(), p.isModified());
            assertTrue(p.toString(), p.isNewOrUndeleted());
            sc.match(p, true);
        }
        // Modified but not new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            p.setOsmId(1, 1);
            assertTrue(p.toString(), p.isModified());
            assertFalse(p.toString(), p.isNewOrUndeleted());
            sc.match(p, true);
        }
        // Not modified nor new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            p.setOsmId(2, 2);
            assertFalse(p.toString(), p.isModified());
            assertFalse(p.toString(), p.isNewOrUndeleted());
            sc.match(p, false);
        }
    }

    /**
     * Search by selected status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testSelected() throws SearchParseError {
        SearchContext sc = new SearchContext("selected");
        // Not selected
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertFalse(p.toString(), p.isSelected());
            sc.match(p, false);
        }
        // Selected
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            sc.ds.addSelected(p);
            assertTrue(p.toString(), p.isSelected());
            sc.match(p, true);
        }
    }

    /**
     * Search by incomplete status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testIncomplete() throws SearchParseError {
        SearchContext sc = new SearchContext("incomplete");
        // Not incomplete
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertFalse(p.toString(), p.isIncomplete());
            sc.match(p, false);
        }
        // Incomplete
        sc.n2.setCoor(null);
        WayData wd = new WayData();
        wd.setIncomplete(true);
        sc.w2.load(wd);
        RelationData rd = new RelationData();
        rd.setIncomplete(true);
        sc.r2.load(rd);
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            assertTrue(p.toString(), p.isIncomplete());
            sc.match(p, true);
        }
    }

    /**
     * Search by untagged status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testUntagged() throws SearchParseError {
        SearchContext sc = new SearchContext("untagged");
        // Untagged
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertFalse(p.toString(), p.isTagged());
            sc.match(p, true);
        }
        // Tagged
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            p.put("foo", "bar");
            assertTrue(p.toString(), p.isTagged());
            sc.match(p, false);
        }
    }

    /**
     * Search by closed status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testClosed() throws SearchParseError {
        SearchContext sc = new SearchContext("closed");
        // Closed
        sc.w1.addNode(sc.n1);
        for (Way w : new Way[]{sc.w1}) {
            assertTrue(w.toString(), w.isClosed());
            sc.match(w, true);
        }
        // Unclosed
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, false);
        }
    }

    /**
     * Search by new status.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testNew() throws SearchParseError {
        SearchContext sc = new SearchContext("new");
        // New
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.w1, sc.r1}) {
            assertTrue(p.toString(), p.isNew());
            sc.match(p, true);
        }
        // Not new
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n2, sc.w2, sc.r2}) {
            p.setOsmId(2, 2);
            assertFalse(p.toString(), p.isNew());
            sc.match(p, false);
        }
    }

    /**
     * Search for node objects.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testTypeNode() throws SearchParseError {
        final SearchContext sc = new SearchContext("type:node");
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w1, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, OsmPrimitiveType.NODE.equals(p.getType()));
        }
    }

    /**
     * Search for way objects.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testTypeWay() throws SearchParseError {
        final SearchContext sc = new SearchContext("type:way");
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w1, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, OsmPrimitiveType.WAY.equals(p.getType()));
        }
    }

    /**
     * Search for relation objects.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testTypeRelation() throws SearchParseError {
        final SearchContext sc = new SearchContext("type:relation");
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w1, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, OsmPrimitiveType.RELATION.equals(p.getType()));
        }
    }

    /**
     * Search for users.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testUser() throws SearchParseError {
        final SearchContext foobar = new SearchContext("user:foobar");
        foobar.n1.setUser(User.createLocalUser("foobar"));
        foobar.match(foobar.n1, true);
        foobar.match(foobar.n2, false);
        final SearchContext anonymous = new SearchContext("user:anonymous");
        anonymous.n1.setUser(User.createLocalUser("foobar"));
        anonymous.match(anonymous.n1, false);
        anonymous.match(anonymous.n2, true);
    }

    /**
     * Compiles "foo type bar" and tests the parse error message
     * @throws SearchParseError always
     */
    @Test
    public void testFooTypeBar() throws SearchParseError {
        expectedEx.expect(SearchParseError.class);
        expectedEx.expectMessage("<html>Expecting <code>:</code> after <i>type</i></html>");
        SearchCompiler.compile("foo type bar");
    }

    /**
     * Search for primitive timestamps.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testTimestamp() throws SearchParseError {
        final Match search = SearchCompiler.compile("timestamp:2010/2011");
        final Node n1 = new Node();
        n1.setTimestamp(DateUtils.fromString("2010-01-22"));
        assertTrue(search.match(n1));
        n1.setTimestamp(DateUtils.fromString("2016-01-22"));
        assertFalse(search.match(n1));
    }

    /**
     * Tests the implementation of the Boolean logic.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testBooleanLogic() throws SearchParseError {
        final SearchCompiler.Match c1 = SearchCompiler.compile("foo AND bar AND baz");
        assertTrue(c1.match(newPrimitive("foobar", "baz")));
        assertEquals("foo && bar && baz", c1.toString());
        final SearchCompiler.Match c2 = SearchCompiler.compile("foo AND (bar OR baz)");
        assertTrue(c2.match(newPrimitive("foobar", "yes")));
        assertTrue(c2.match(newPrimitive("foobaz", "yes")));
        assertEquals("foo && (bar || baz)", c2.toString());
        final SearchCompiler.Match c3 = SearchCompiler.compile("foo OR (bar baz)");
        assertEquals("foo || (bar && baz)", c3.toString());
        final SearchCompiler.Match c4 = SearchCompiler.compile("foo1 OR (bar1 bar2 baz1 XOR baz2) OR foo2");
        assertEquals("foo1 || (bar1 && bar2 && (baz1 ^ baz2)) || foo2", c4.toString());
        final SearchCompiler.Match c5 = SearchCompiler.compile("foo1 XOR (baz1 XOR (bar baz))");
        assertEquals("foo1 ^ baz1 ^ (bar && baz)", c5.toString());
        final SearchCompiler.Match c6 = SearchCompiler.compile("foo1 XOR ((baz1 baz2) XOR (bar OR baz))");
        assertEquals("foo1 ^ (baz1 && baz2) ^ (bar || baz)", c6.toString());
    }

    /**
     * Tests {@code buildSearchStringForTag}.
     * @throws SearchParseError if an error has been encountered while compiling
     */
    @Test
    public void testBuildSearchStringForTag() throws SearchParseError {
        final Tag tag1 = new Tag("foo=", "bar\"");
        final Tag tag2 = new Tag("foo=", "=bar");
        final String search1 = SearchCompiler.buildSearchStringForTag(tag1.getKey(), tag1.getValue());
        assertEquals("\"foo=\"=\"bar\\\"\"", search1);
        assertTrue(SearchCompiler.compile(search1).match(tag1));
        assertFalse(SearchCompiler.compile(search1).match(tag2));
        final String search2 = SearchCompiler.buildSearchStringForTag(tag1.getKey(), "");
        assertEquals("\"foo=\"=*", search2);
        assertTrue(SearchCompiler.compile(search2).match(tag1));
        assertTrue(SearchCompiler.compile(search2).match(tag2));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13870">Bug #13870</a>.
     * @throws SearchParseError always
     */
    @Test(expected = SearchParseError.class)
    public void testPattern13870() throws SearchParseError {
        // https://bugs.openjdk.java.net/browse/JI-9044959
        SearchSetting setting = new SearchSetting();
        setting.regexSearch = true;
        setting.text = "[";
        SearchCompiler.compile(setting);
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14217">Bug #14217</a>.
     * @throws Exception never
     */
    @Test
    public void testTicket14217() throws Exception {
        assertNotNull(SearchCompiler.compile(new String(Files.readAllBytes(
                Paths.get(TestUtils.getRegressionDataFile(14217, "filter.txt"))), StandardCharsets.UTF_8)));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17112">Bug #17112</a>.
     * @throws SearchParseError always
     */
    @Test(expected = SearchParseError.class)
    public void testTicket17112() throws SearchParseError {
        SearchSetting setting = new SearchSetting();
        setting.mapCSSSearch = true;
        setting.text = "w"; // partial input
        SearchCompiler.compile(setting);
    }

    /**
     * Test empty values.
     * @throws SearchParseError never
     */
    @Test
    public void testEmptyValues15943() throws SearchParseError {
        Match matcher = SearchCompiler.compile("access=");
        assertTrue(matcher.match(new Tag("access", null)));
        assertTrue(matcher.match(new Tag("access", "")));
        assertFalse(matcher.match(new Tag("access", "private")));
    }

    /**
     * Test whether a key exists.
     * @throws SearchParseError never
     */
    @Test
    public void testKeyExists15943() throws SearchParseError {
        Match matcher = SearchCompiler.compile("surface:");
        assertTrue(matcher.match(new Tag("surface", "")));
        assertTrue(matcher.match(new Tag("surface", "wood")));
        assertFalse(matcher.match(new Tag("surface:source", "xxx")));
        assertFalse(matcher.match(new Tag("foo", "bar")));
        assertFalse(matcher.match(new Tag("name", "foo:surface:bar")));
    }

    /**
     * Unit test of {@link SearchCompiler.ExactKeyValue.Mode} enum.
     */
    @Test
    public void testEnumExactKeyValueMode() {
        TestUtils.superficialEnumCodeCoverage(ExactKeyValue.Mode.class);
    }

    /**
     * Robustness test for preset searching. Ensures that the query 'preset:' is not accepted.
     * @throws SearchParseError always
     * @since 12464
     */
    @Test(expected = SearchParseError.class)
    public void testPresetSearchMissingValue() throws SearchParseError {
        SearchSetting settings = new SearchSetting();
        settings.text = "preset:";
        settings.mapCSSSearch = false;

        TaggingPresets.readFromPreferences();

        SearchCompiler.compile(settings);
    }

    /**
     * Robustness test for preset searching. Validates that it is not possible to search for
     * non existing presets.
     * @throws SearchParseError always
     * @since 12464
     */
    @Test(expected = SearchParseError.class)
    public void testPresetNotExist() throws SearchParseError {
        String testPresetName = "groupnamethatshouldnotexist/namethatshouldnotexist";
        SearchSetting settings = new SearchSetting();
        settings.text = "preset:" + testPresetName;
        settings.mapCSSSearch = false;

        // load presets
        TaggingPresets.readFromPreferences();

        SearchCompiler.compile(settings);
    }

    /**
     * Robustness tests for preset searching. Ensures that combined preset names (having more than
     * 1 word) must be enclosed with " .
     * @throws SearchParseError always
     * @since 12464
     */
    @Test(expected = SearchParseError.class)
    public void testPresetMultipleWords() throws SearchParseError {
        TaggingPreset testPreset = new TaggingPreset();
        testPreset.name = "Test Combined Preset Name";
        testPreset.group = new TaggingPresetMenu();
        testPreset.group.name = "TestGroupName";

        String combinedPresetname = testPreset.getRawName();
        SearchSetting settings = new SearchSetting();
        settings.text = "preset:" + combinedPresetname;
        settings.mapCSSSearch = false;

        // load presets
        TaggingPresets.readFromPreferences();

        SearchCompiler.compile(settings);
    }

    /**
     * Ensures that correct presets are stored in the {@link org.openstreetmap.josm.data.osm.search.SearchCompiler.Preset}
     * class against which the osm primitives are tested.
     * @throws SearchParseError if an error has been encountered while compiling
     * @throws NoSuchFieldException if there is no field called 'presets'
     * @throws IllegalAccessException if cannot access the field where all matching presets are stored
     * @since 12464
     */
    @Test
    public void testPresetLookup() throws SearchParseError, NoSuchFieldException, IllegalAccessException {
        TaggingPreset testPreset = new TaggingPreset();
        testPreset.name = "Test Preset Name";
        testPreset.group = new TaggingPresetMenu();
        testPreset.group.name = "Test Preset Group Name";

        String query = "preset:" +
                "\"" + testPreset.getRawName() + "\"";
        SearchSetting settings = new SearchSetting();
        settings.text = query;
        settings.mapCSSSearch = false;

        // load presets and add the test preset
        TaggingPresets.readFromPreferences();
        TaggingPresets.addTaggingPresets(Collections.singletonList(testPreset));

        Match match = SearchCompiler.compile(settings);

        // access the private field where all matching presets are stored
        // and ensure that indeed the correct ones are there
        Field field = match.getClass().getDeclaredField("presets");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Collection<TaggingPreset> foundPresets = (Collection<TaggingPreset>) field.get(match);

        assertEquals(1, foundPresets.size());
        assertTrue(foundPresets.contains(testPreset));
    }

    /**
     * Ensures that the wildcard search works and that correct presets are stored in
     * the {@link org.openstreetmap.josm.data.osm.search.SearchCompiler.Preset} class against which
     * the osm primitives are tested.
     * @throws SearchParseError if an error has been encountered while compiling
     * @throws NoSuchFieldException if there is no field called 'presets'
     * @throws IllegalAccessException if cannot access the field where all matching presets are stored
     * @since 12464
     */
    @Test
    public void testPresetLookupWildcard() throws SearchParseError, NoSuchFieldException, IllegalAccessException {
        TaggingPresetMenu group = new TaggingPresetMenu();
        group.name = "TestPresetGroup";

        TaggingPreset testPreset1 = new TaggingPreset();
        testPreset1.name = "TestPreset1";
        testPreset1.group = group;

        TaggingPreset testPreset2 = new TaggingPreset();
        testPreset2.name = "TestPreset2";
        testPreset2.group = group;

        TaggingPreset testPreset3 = new TaggingPreset();
        testPreset3.name = "TestPreset3";
        testPreset3.group = group;

        String query = "preset:" + "\"" + group.getRawName() + "/*\"";
        SearchSetting settings = new SearchSetting();
        settings.text = query;
        settings.mapCSSSearch = false;

        TaggingPresets.readFromPreferences();
        TaggingPresets.addTaggingPresets(Arrays.asList(testPreset1, testPreset2, testPreset3));

        Match match = SearchCompiler.compile(settings);

        // access the private field where all matching presets are stored
        // and ensure that indeed the correct ones are there
        Field field = match.getClass().getDeclaredField("presets");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Collection<TaggingPreset> foundPresets = (Collection<TaggingPreset>) field.get(match);

        assertEquals(3, foundPresets.size());
        assertTrue(foundPresets.contains(testPreset1));
        assertTrue(foundPresets.contains(testPreset2));
        assertTrue(foundPresets.contains(testPreset3));
    }

    /**
     * Ensures that correct primitives are matched against the specified preset.
     * @throws SearchParseError if an error has been encountered while compiling
     * @since 12464
     */
    @Test
    public void testPreset() throws SearchParseError {
        final String presetName = "Test Preset Name";
        final String presetGroupName = "Test Preset Group";
        final String key = "test_key1";
        final String val = "test_val1";

        Key key1 = new Key();
        key1.key = key;
        key1.value = val;

        TaggingPreset testPreset = new TaggingPreset();
        testPreset.name = presetName;
        testPreset.types = Collections.singleton(TaggingPresetType.NODE);
        testPreset.data.add(key1);
        testPreset.group = new TaggingPresetMenu();
        testPreset.group.name = presetGroupName;

        TaggingPresets.readFromPreferences();
        TaggingPresets.addTaggingPresets(Collections.singleton(testPreset));

        String query = "preset:" + "\"" + testPreset.getRawName() + "\"";

        SearchContext ctx = new SearchContext(query);
        ctx.n1.put(key, val);
        ctx.n2.put(key, val);

        for (OsmPrimitive osm : new OsmPrimitive[] {ctx.n1, ctx.n2}) {
            ctx.match(osm, true);
        }

        for (OsmPrimitive osm : new OsmPrimitive[] {ctx.r1, ctx.r2, ctx.w1, ctx.w2}) {
            ctx.match(osm, false);
        }
    }

    /**
     * Unit test of methods {@link Match#equals} and {@link Match#hashCode}, including all subclasses.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        Set<Class<? extends Match>> matchers = TestUtils.getJosmSubtypes(Match.class);
        Assert.assertTrue(matchers.size() >= 10); // if it finds less than 10 classes, something is broken
        for (Class<?> c : matchers) {
            Logging.debug(c.toString());
            EqualsVerifier.forClass(c).usingGetClass()
                .suppress(Warning.NONFINAL_FIELDS, Warning.INHERITED_DIRECTLY_FROM_OBJECT)
                .withPrefabValues(TaggingPreset.class, newTaggingPreset("foo"), newTaggingPreset("bar"))
                .verify();
        }
    }

    private static TaggingPreset newTaggingPreset(String name) {
        TaggingPreset result = new TaggingPreset();
        result.name = name;
        return result;
    }
}
