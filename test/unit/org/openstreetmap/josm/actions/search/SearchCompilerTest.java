// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.actions.search.SearchCompiler.ExactKeyValue;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
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
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.date.DateUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SearchCompiler}.
 */
public class SearchCompilerTest {

    /**
     * We need prefs for this. We access preferences when creating OSM primitives.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

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

        private SearchContext(String state) throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testAny() throws ParseError {
        final SearchCompiler.Match c = SearchCompiler.compile("foo");
        assertTrue(c.match(newPrimitive("foobar", "true")));
        assertTrue(c.match(newPrimitive("name", "hello-foo-xy")));
        assertFalse(c.match(newPrimitive("name", "X")));
        assertEquals("foo", c.toString());
    }

    /**
     * Search by equality key=value.
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testEquals() throws ParseError {
        final SearchCompiler.Match c = SearchCompiler.compile("foo=bar");
        assertFalse(c.match(newPrimitive("foobar", "true")));
        assertTrue(c.match(newPrimitive("foo", "bar")));
        assertFalse(c.match(newPrimitive("fooX", "bar")));
        assertFalse(c.match(newPrimitive("foo", "barX")));
        assertEquals("foo=bar", c.toString());
    }

    /**
     * Search by comparison.
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testCompare() throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testNth() throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testNthParseNegative() throws ParseError {
        assertEquals("Nth{nth=-1, modulo=false}", SearchCompiler.compile("nth:-1").toString());
    }

    /**
     * Search by modified status.
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testModified() throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testSelected() throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testIncomplete() throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testUntagged() throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testClosed() throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testNew() throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testTypeNode() throws ParseError {
        final SearchContext sc = new SearchContext("type:node");
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w1, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, OsmPrimitiveType.NODE.equals(p.getType()));
        }
    }

    /**
     * Search for way objects.
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testTypeWay() throws ParseError {
        final SearchContext sc = new SearchContext("type:way");
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w1, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, OsmPrimitiveType.WAY.equals(p.getType()));
        }
    }

    /**
     * Search for relation objects.
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testTypeRelation() throws ParseError {
        final SearchContext sc = new SearchContext("type:relation");
        for (OsmPrimitive p : new OsmPrimitive[]{sc.n1, sc.n2, sc.w1, sc.w2, sc.r1, sc.r2}) {
            sc.match(p, OsmPrimitiveType.RELATION.equals(p.getType()));
        }
    }

    /**
     * Search for users.
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testUser() throws ParseError {
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
     */
    @Test
    public void testFooTypeBar() {
        try {
            SearchCompiler.compile("foo type bar");
            fail();
        } catch (ParseError parseError) {
            assertEquals("<html>Expecting <code>:</code> after <i>type</i>", parseError.getMessage());
        }
    }

    /**
     * Search for primitive timestamps.
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testTimestamp() throws ParseError {
        final Match search = SearchCompiler.compile("timestamp:2010/2011");
        final Node n1 = new Node();
        n1.setTimestamp(DateUtils.fromString("2010-01-22"));
        assertTrue(search.match(n1));
        n1.setTimestamp(DateUtils.fromString("2016-01-22"));
        assertFalse(search.match(n1));
    }

    /**
     * Tests the implementation of the Boolean logic.
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testBooleanLogic() throws ParseError {
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
     * @throws ParseError if an error has been encountered while compiling
     */
    @Test
    public void testBuildSearchStringForTag() throws ParseError {
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
     * @throws ParseError always
     */
    @Test(expected = ParseError.class)
    public void testPattern13870() throws ParseError {
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
     * Unit test of {@link SearchCompiler.ExactKeyValue.Mode} enum.
     */
    @Test
    public void testEnumExactKeyValueMode() {
        TestUtils.superficialEnumCodeCoverage(ExactKeyValue.Mode.class);
    }
}
