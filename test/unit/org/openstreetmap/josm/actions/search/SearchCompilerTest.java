// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.search;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

public class SearchCompilerTest {

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    protected OsmPrimitive newPrimitive(String key, String value) {
        final Node p = new Node();
        p.put(key, value);
        return p;
    }

    @Test
    public void testAny() throws Exception {
        final SearchCompiler.Match c = SearchCompiler.compile("foo");
        Assert.assertTrue(c.match(newPrimitive("foobar", "true")));
        Assert.assertTrue(c.match(newPrimitive("name", "hello-foo-xy")));
        Assert.assertFalse(c.match(newPrimitive("name", "X")));
    }

    @Test
    public void testEquals() throws Exception {
        final SearchCompiler.Match c = SearchCompiler.compile("foo=bar");
        Assert.assertFalse(c.match(newPrimitive("foobar", "true")));
        Assert.assertTrue(c.match(newPrimitive("foo", "bar")));
        Assert.assertFalse(c.match(newPrimitive("fooX", "bar")));
        Assert.assertFalse(c.match(newPrimitive("foo", "barX")));
    }

    @Test
    public void testCompare() throws Exception {
        final SearchCompiler.Match c1 = SearchCompiler.compile("start_date>1950");
        Assert.assertTrue(c1.match(newPrimitive("start_date", "1950-01-01")));
        Assert.assertTrue(c1.match(newPrimitive("start_date", "1960")));
        Assert.assertFalse(c1.match(newPrimitive("start_date", "1950")));
        Assert.assertFalse(c1.match(newPrimitive("start_date", "1000")));
        Assert.assertTrue(c1.match(newPrimitive("start_date", "101010")));

        final SearchCompiler.Match c2 = SearchCompiler.compile("start_date<1960");
        Assert.assertTrue(c2.match(newPrimitive("start_date", "1950-01-01")));
        Assert.assertFalse(c2.match(newPrimitive("start_date", "1960")));
        Assert.assertTrue(c2.match(newPrimitive("start_date", "1950")));
        Assert.assertTrue(c2.match(newPrimitive("start_date", "1000")));
        Assert.assertTrue(c2.match(newPrimitive("start_date", "200")));

        final SearchCompiler.Match c3 = SearchCompiler.compile("name<I");
        Assert.assertTrue(c3.match(newPrimitive("name", "Alpha")));
        Assert.assertFalse(c3.match(newPrimitive("name", "Sigma")));

        final SearchCompiler.Match c4 = SearchCompiler.compile("\"start_date\"<1960");
        Assert.assertTrue(c4.match(newPrimitive("start_date", "1950-01-01")));
        Assert.assertFalse(c4.match(newPrimitive("start_date", "2000")));

        final SearchCompiler.Match c5 = SearchCompiler.compile("height>180");
        Assert.assertTrue(c5.match(newPrimitive("height", "200")));
        Assert.assertTrue(c5.match(newPrimitive("height", "99999")));
        Assert.assertFalse(c5.match(newPrimitive("height", "50")));
        Assert.assertFalse(c5.match(newPrimitive("height", "-9999")));
        Assert.assertFalse(c5.match(newPrimitive("height", "fixme")));

        final SearchCompiler.Match c6 = SearchCompiler.compile("name>C");
        Assert.assertTrue(c6.match(newPrimitive("name", "Delta")));
        Assert.assertFalse(c6.match(newPrimitive("name", "Alpha")));
    }

    @Test
    public void testNth() throws Exception {
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
        Assert.assertFalse(SearchCompiler.compile("nth:2").match(node1));
        Assert.assertTrue(SearchCompiler.compile("nth:1").match(node1));
        Assert.assertFalse(SearchCompiler.compile("nth:0").match(node1));
        Assert.assertTrue(SearchCompiler.compile("nth:0").match(node0));
        Assert.assertTrue(SearchCompiler.compile("nth:2").match(node2));
        Assert.assertTrue(SearchCompiler.compile("nth:-1").match(node2));
        Assert.assertTrue(SearchCompiler.compile("nth:-2").match(node1));
        Assert.assertTrue(SearchCompiler.compile("nth:-3").match(node0));
    }

    @Test
    public void testNthParseNegative() throws Exception {
        Assert.assertThat(SearchCompiler.compile("nth:-1").toString(), CoreMatchers.is("Nth{nth=-1, modulo=false}"));

    }
}
