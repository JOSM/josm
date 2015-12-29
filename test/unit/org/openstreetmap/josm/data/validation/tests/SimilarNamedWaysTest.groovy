// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests

import static org.junit.Assert.assertEquals

import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.data.validation.TestError

class SimilarNamedWaysTest extends GroovyTestCase {

    SimilarNamedWays test = new SimilarNamedWays()

    @Override
    void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    public static List<TestError> testWays(String namea, String nameb) {
        def ds = new DataSet()

        def n00 = new Node(LatLon.ZERO)
        def n10 = new Node(new LatLon(1, 0))
        def n20 = new Node(new LatLon(2, 0))
        def n30 = new Node(new LatLon(3, 0))
        def n40 = new Node(new LatLon(4, 0))

        ds.addPrimitive(n00)
        ds.addPrimitive(n10)
        ds.addPrimitive(n20)
        ds.addPrimitive(n30)
        ds.addPrimitive(n40)

        def waya = new Way()
        waya.addNode(n00)
        waya.addNode(n10)
        waya.addNode(n20)
        waya.put("name", namea)
        def wayb = new Way()
        wayb.addNode(n20)
        wayb.addNode(n30)
        wayb.addNode(n40)
        wayb.put("name", nameb)

        ds.addPrimitive(waya)
        ds.addPrimitive(wayb)

        assert waya.isUsable()
        assert wayb.isUsable()

        def t = new SimilarNamedWays()
        t.startTest(null)
        t.visit(waya)
        t.visit(wayb)
        return t.errors
    }

    void testCombinations() {
        assert testWays("Church Street", "Water Street").isEmpty()
        assert !testWays("Main Street", "Maim Street").isEmpty()
        assert !testWays("First Street", "Frist Street").isEmpty()

        assert testWays("1st Street", "2nd Street").isEmpty()
        assert testWays("First Avenue", "Second Avenue").isEmpty()
        assert testWays("West Main Street", "East Main Street").isEmpty()
        assert testWays("A Street", "B Street").isEmpty()
    }

    void checkSimilarity(String message, String name1, String name2, boolean expected) {
        boolean actual = test.similaryName(name1, name2);
        assertEquals(message, expected, actual);
    }

    void testSimilarNames() {
        checkSimilarity("same string", "Testname", "Testname", false);
        checkSimilarity("different case", "Testname", "TestName", true);
        checkSimilarity("typo", "Testname", "Testxame", true);
        checkSimilarity("missing char", "Testname", "Testame", true);
        checkSimilarity("additional char", "Testname", "Testxname", true);
        checkSimilarity("2 changes", "Testname", "Tostxname", true);
        checkSimilarity("3 changes", "Testname", "Tostxnam", false);

        // regular expression rule
        checkSimilarity("same number", "track 1", "track 1", false);
        checkSimilarity("different number", "track 1", "track 2", false);
        checkSimilarity("different number length", "track 9", "track 10", false);
        checkSimilarity("multiple numbers", "track 8 - 9", "track 10 - 11", false);

        checkSimilarity("1st and 2nd", "1st Street", "2nd Street", false);
        checkSimilarity("1st case", "1St Street", "1st Street", true);
        checkSimilarity("1st and 2nd case", "1St Street", "2nd Street", true);
        checkSimilarity("3rd and 4th", "2rd Street", "4th Street", false);

        // synonyms
        checkSimilarity("east and west", "East Foothill Drive", "West Foothill Drive", false);
        checkSimilarity("east and west case", "east Foothill Drive", "West Foothill Drive", true);
        checkSimilarity("first and second", "First Street", "Second Street", false);
        checkSimilarity("first and second case", "First Street", "second Street", true);
        checkSimilarity("first and second typo", "Forst Street", "Second Street", true);
        checkSimilarity("first and second typo2", "First Street", "Socond Street", true);
        checkSimilarity("first and second 2 changes", "First Street", "Soconds Street", true);
        checkSimilarity("first and second 3 changes", "First Street", "Soconds Stret", false);
    }
}
