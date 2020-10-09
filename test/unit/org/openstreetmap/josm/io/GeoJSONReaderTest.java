// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Unit tests of {@link GeoJSONReader}.
 */
public class GeoJSONReaderTest {

    /**
     * Setup test.
     */
    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    /**
     * Test reading a GeoJSON file.
     * @throws Exception in case of error
     */
    @Test
    public void testReadGeoJson() throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "geo.json"))) {
            final List<OsmPrimitive> primitives = new ArrayList<>(new GeoJSONReader()
                .doParseDataSet(in, null)
                .getPrimitives(it -> true));

            assertExpectedGeoPrimitives(primitives);
        }
    }

    /**
     * Tests reading a GeoJSON file that is line by line separated, per RFC 7464
     * @throws Exception in case of an error
     */
    @Test
    public void testReadLineByLineGeoJSON() throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "geoLineByLine.json"))) {
            final List<OsmPrimitive> primitives = new ArrayList<>(new GeoJSONReader()
                .doParseDataSet(in, null)
                .getPrimitives(it -> true));

            assertExpectedGeoPrimitives(primitives);
        }
    }

    private void assertExpectedGeoPrimitives(Collection<OsmPrimitive> primitives) {
        assertEquals(20, primitives.size());

        final Node node1 = new Node(new LatLon(0.5, 102.0));
        final Optional<OsmPrimitive> foundNode1 = primitives.stream()
            .filter(it -> areEqualNodes(it, node1))
            .findFirst();
        assertTrue(foundNode1.isPresent());
        assertEquals("valueA", foundNode1.get().get("propA"));

        final Way way1 = new Way();
        way1.addNode(new Node(new LatLon(0.5, 102.0)));
        way1.addNode(new Node(new LatLon(1, 103)));
        way1.addNode(new Node(new LatLon(0, 104)));
        way1.addNode(new Node(new LatLon(1, 105)));
        final Optional<OsmPrimitive> foundWay1 = primitives.stream()
            .filter(it -> areEqualWays(it, way1))
            .findFirst();
        assertTrue(foundWay1.isPresent());
        assertEquals("valueB", foundWay1.get().get("propB"));
        assertEquals("0.0", foundWay1.get().get("propB2"));
        assertEquals(foundNode1.get(), ((Way) foundWay1.get()).firstNode());
        assertEquals("valueA", ((Way) foundWay1.get()).firstNode().get("propA"));

        final Way way2 = new Way();
        way2.addNode(new Node(new LatLon(40, 180)));
        way2.addNode(new Node(new LatLon(50, 180)));
        way2.addNode(new Node(new LatLon(50, 170)));
        way2.addNode(new Node(new LatLon(40, 170)));
        way2.addNode(new Node(new LatLon(40, 180)));
        final Optional<OsmPrimitive> foundWay2 = primitives.stream()
            .filter(it -> areEqualWays(it, way2))
            .findFirst();
        assertTrue(foundWay2.isPresent());
        assertEquals(
            ((Way) foundWay2.get()).getNode(0),
            ((Way) foundWay2.get()).getNode(((Way) foundWay2.get()).getNodesCount() - 1)
        );

        final Way way3 = new Way();
        way3.addNode(new Node(new LatLon(40, -170)));
        way3.addNode(new Node(new LatLon(50, -170)));
        way3.addNode(new Node(new LatLon(50, -180)));
        way3.addNode(new Node(new LatLon(40, -180)));
        way3.addNode(new Node(new LatLon(40, -170)));
        final Optional<OsmPrimitive> foundWay3 = primitives.stream()
            .filter(it -> areEqualWays(it, way3))
            .findFirst();
        assertTrue(foundWay3.isPresent());
        assertEquals(
            ((Way) foundWay3.get()).getNode(0),
            ((Way) foundWay3.get()).getNode(((Way) foundWay3.get()).getNodesCount() - 1)
        );

        final Way way4 = new Way();
        way4.addNode(new Node(new LatLon(0, 100)));
        way4.addNode(new Node(new LatLon(0, 101)));
        way4.addNode(new Node(new LatLon(1, 101)));
        way4.addNode(new Node(new LatLon(1, 100)));
        way4.addNode(new Node(new LatLon(0, 100)));
        final Optional<OsmPrimitive> foundWay4 = primitives.stream()
            .filter(it -> areEqualWays(it, way4))
            .findFirst();
        assertTrue(foundWay4.isPresent());
        assertEquals(
            ((Way) foundWay4.get()).getNode(0),
            ((Way) foundWay4.get()).getNode(((Way) foundWay4.get()).getNodesCount() - 1)
        );
        assertEquals("valueD", foundWay4.get().get("propD"));
        assertFalse(foundWay4.get().hasTag("propD2"));
        assertEquals("true", foundWay4.get().get("propD3"));
        assertFalse(foundWay4.get().hasKey("propD4"));
        assertNull(foundWay4.get().get("propD4"));
    }

    /**
     * Test reading a GeoJSON file with a named CRS.
     * @throws Exception in case of error
     */
    @Test
    public void testReadGeoJsonNamedCrs() throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "geocrs.json"))) {
            final List<OsmPrimitive> primitives = new ArrayList<>(new GeoJSONReader()
                    .doParseDataSet(in, null)
                    .getPrimitives(it -> true));
                assertEquals(24, primitives.size());
                assertTrue(primitives.stream()
                        .anyMatch(it -> areEqualNodes(it, new Node(new LatLon(52.5840213, 13.1724145)))));
        }
    }

    /**
     * Test reading a JSON file which is not a proper GeoJSON (type missing).
     */
    @Test
    public void testReadGeoJsonWithoutType() {
        assertThrows(IllegalDataException.class, () ->
                new GeoJSONReader().doParseDataSet(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)), null));
    }

    private static boolean areEqualNodes(final OsmPrimitive p1, final OsmPrimitive p2) {
        return (p1 instanceof Node)
            && (p2 instanceof Node)
            && ((Node) p1).getCoor().equalsEpsilon(((Node) p2).getCoor());
    }

    private static boolean areEqualWays(final OsmPrimitive p1, final OsmPrimitive p2) {
        if (
            (!(p1 instanceof Way))
            || (!(p2 instanceof Way))
            || ((Way) p1).getNodes().size() != ((Way) p2).getNodes().size()
        ) {
            return false;
        }
        return IntStream.range(0, ((Way) p1).getNodes().size())
                .allMatch(i -> areEqualNodes(((Way) p1).getNode(i), ((Way) p2).getNode(i)));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/19822">Bug #19822</a>.
     * @throws Exception in case of error
     */
    @Test
    public void testTicket19822() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(19822, "data.geojson")) {
            final List<OsmPrimitive> primitives = new ArrayList<>(
                    new GeoJSONReader().doParseDataSet(in, null).getPrimitives(it -> true));
            assertTrue(primitives.stream().anyMatch(p -> p instanceof Relation && p.isMultipolygon()));
        }
    }

}
