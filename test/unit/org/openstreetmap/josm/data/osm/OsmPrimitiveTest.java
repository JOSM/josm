// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;

/**
 * Unit tests of the {@code OsmPrimitive} class.
 */
class OsmPrimitiveTest {
    private void compareReferrers(OsmPrimitive actual, OsmPrimitive... expected) {
        assertEquals(new HashSet<>(Arrays.asList(expected)), new HashSet<>(actual.getReferrers()));
    }

    private final DataSet dataSet = new DataSet();

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUp() {
        ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
    }

    @Test
    void testSimpleReferrersTest() {
        Node n1 = new Node(LatLon.ZERO);
        Way w1 = new Way();
        w1.addNode(n1);
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(w1);
        compareReferrers(n1, w1);
    }

    @Test
    void testAddAndRemoveReferrer() {
        Node n1 = new Node(LatLon.ZERO);
        Node n2 = new Node(LatLon.ZERO);
        Way w1 = new Way();
        w1.addNode(n1);
        w1.addNode(n2);
        w1.addNode(n1);
        w1.removeNode(n1);
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(w1);
        compareReferrers(n1);
        compareReferrers(n2, w1);
    }

    @Test
    void testMultipleReferrers() {
        Node n1 = new Node(LatLon.ZERO);
        Way w1 = new Way();
        Way w2 = new Way();
        Relation r1 = new Relation();
        w1.addNode(n1);
        w2.addNode(n1);
        r1.addMember(new RelationMember("", n1));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(w1);
        dataSet.addPrimitive(w2);
        dataSet.addPrimitive(r1);
        compareReferrers(n1, w1, w2, r1);
    }

    @Test
    void testRemoveMemberFromRelationReferrerTest() {
        Node n1 = new Node(LatLon.ZERO);
        Relation r1 = new Relation();
        r1.addMember(new RelationMember("", n1));
        r1.addMember(new RelationMember("", n1));
        r1.removeMember(0);
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(r1);
        compareReferrers(n1, r1);
    }

    @Test
    void testSetRelationMemberReferrerTest() {
        Node n1 = new Node(LatLon.ZERO);
        Node n2 = new Node(LatLon.ZERO);
        Relation r1 = new Relation();
        Relation r2 = new Relation();
        r1.addMember(new RelationMember("", n1));
        r2.addMember(new RelationMember("", n2));
        r2.setMember(0, r1.getMember(0));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(r1);
        dataSet.addPrimitive(r2);
        compareReferrers(n1, r1, r2);
        compareReferrers(n2);
    }

    @Test
    void testRemovePrimitiveReferrerTest() {
        Node n1 = new Node(LatLon.ZERO);
        Way w1 = new Way();
        w1.addNode(n1);
        w1.setDeleted(true);
        dataSet.addPrimitive(n1);
        compareReferrers(n1);
        w1.setDeleted(false);
        dataSet.addPrimitive(w1);

        compareReferrers(n1, w1);

        Relation r1 = new Relation();
        r1.addMember(new RelationMember("", w1));
        r1.setDeleted(true);
        dataSet.addPrimitive(r1);
        compareReferrers(w1);
        r1.setDeleted(false);
        compareReferrers(w1, r1);
    }

    @Test
    void testNodeFromMultipleDatasets() {
        // n has two referrers - w1 and w2. But only w1 is returned because it is in the same dataset as n
        Node n = new Node(LatLon.ZERO);

        Way w1 = new Way();
        w1.addNode(n);
        dataSet.addPrimitive(n);
        dataSet.addPrimitive(w1);
        new Way(w1);

        assertEquals(n.getReferrers().size(), 1);
        assertEquals(n.getReferrers().get(0), w1);
    }

    @Test
    void testCheckMustBeInDatasate() {
        Node n = new Node();
        assertThrows(DataIntegrityProblemException.class, n::getReferrers);
    }
}
