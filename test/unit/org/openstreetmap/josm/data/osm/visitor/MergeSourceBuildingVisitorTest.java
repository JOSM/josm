// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link MergeSourceBuildingVisitor}.
 */
public class MergeSourceBuildingVisitorTest {

    protected OsmPrimitive lookupByName(Collection<? extends OsmPrimitive> primitives, String name) {
        if (primitives == null) return null;
        if (name == null) return null;
        return primitives.stream()
                .filter(primitive -> name.equals(primitive.get("name")))
                .findFirst().orElse(null);
    }

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @Test
    public void testNodes() {
        DataSet source = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(new LatLon(10.0, 10.0));
        n2.put("name", "n2");
        Node n3 = new Node(3);
        Node n4 = new Node(new LatLon(20.0, 20.0));
        n4.put("name", "n4");
        source.addPrimitive(n1);
        source.addPrimitive(n2);
        source.addPrimitive(n3);
        source.addPrimitive(n4);
        source.setSelected(n1, n2);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(2, hull.getNodes().size());

        OsmPrimitive p = hull.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(3, OsmPrimitiveType.NODE);
        assertNull(p);

        p = lookupByName(hull.getNodes(), "n2");
        assertNotNull(p);

        p = lookupByName(hull.getNodes(), "n4");
        assertNull(p);
    }

    @Test
    public void testOneWay() {
        DataSet source = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Way w1 = new Way(3, 1);
        w1.addNode(n1);
        w1.addNode(n2);
        source.addPrimitive(n1);
        source.addPrimitive(n2);
        source.addPrimitive(w1);
        source.setSelected(w1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.getWays().size());
        assertEquals(2, hull.getNodes().size());

        OsmPrimitive p = hull.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(p);
        assertEquals(p.getClass(), Way.class);
    }

    @Test
    public void testOneWayNodesSelectedToo() {
        DataSet source = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Way w1 = new Way(3, 1);
        w1.addNode(n1);
        w1.addNode(n2);
        source.addPrimitive(n1);
        source.addPrimitive(n2);
        source.addPrimitive(w1);
        source.setSelected(w1, n1, n2);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.getWays().size());
        assertEquals(2, hull.getNodes().size());

        OsmPrimitive p = hull.getPrimitiveById(1, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(2, OsmPrimitiveType.NODE);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(p);
        assertEquals(p.getClass(), Way.class);
    }

    @Test
    public void testOneWayIncomplete() {
        DataSet source = new DataSet();
        Way w1 = new Way(3);
        source.addPrimitive(w1);
        source.setSelected(w1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.getWays().size());

        OsmPrimitive p = hull.getPrimitiveById(3, OsmPrimitiveType.WAY);
        assertNotNull(p);
        assertEquals(p.getClass(), Way.class);
        assertTrue(p.isIncomplete());
    }

    @Test
    public void testOneRelationExistingMembersSelected() {
        DataSet source = new DataSet();
        Relation r1 = new Relation(1, 1);
        Node n20 = new Node(20, 1);
        n20.setCoor(LatLon.ZERO);
        r1.addMember(new RelationMember("node-20", n20));
        Way w30 = new Way(30, 1);
        Node n21 = new Node(21);
        w30.addNode(n21);
        Node n22 = new Node(22);
        w30.addNode(n22);
        r1.addMember(new RelationMember("way-30", w30));
        Relation r40 = new Relation(40);
        r1.addMember(new RelationMember("relation-40", r40));
        source.addPrimitive(n20);
        source.addPrimitive(n21);
        source.addPrimitive(n22);
        source.addPrimitive(w30);
        source.addPrimitive(r40);
        source.addPrimitive(r1);
        source.setSelected(r1, n20, w30, r40);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.getWays().size());
        assertEquals(3, hull.getNodes().size());
        assertEquals(2, hull.getRelations().size());

        OsmPrimitive p = hull.getPrimitiveById(1, OsmPrimitiveType.RELATION);
        assertNotNull(p);
        assertEquals(p.getClass(), Relation.class);

        Way w = (Way) hull.getPrimitiveById(30, OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertEquals(2, w.getNodesCount());
        Node n = (Node) hull.getPrimitiveById(21, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertTrue(w.containsNode(n));

        n = (Node) hull.getPrimitiveById(22, OsmPrimitiveType.NODE);
        assertNotNull(n);
        assertTrue(w.containsNode(n));

        Relation r = (Relation) hull.getPrimitiveById(40, OsmPrimitiveType.RELATION);
        assertNotNull(r);

        r = (Relation) hull.getPrimitiveById(1, OsmPrimitiveType.RELATION);
        assertNotNull(r);
        assertEquals(3, r.getMembersCount());
        RelationMember m = new RelationMember("node-20", hull.getPrimitiveById(20, OsmPrimitiveType.NODE));
        assertTrue(r.getMembers().contains(m));
        m = new RelationMember("way-30", hull.getPrimitiveById(30, OsmPrimitiveType.WAY));
        assertTrue(r.getMembers().contains(m));
        m = new RelationMember("relation-40", hull.getPrimitiveById(40, OsmPrimitiveType.RELATION));
        assertTrue(r.getMembers().contains(m));
    }

    @Test
    public void testOneRelationExistingMembersNotSelected() {
        DataSet source = new DataSet();
        Relation r1 = new Relation(1, 1);
        Node n20 = new Node(20);
        r1.addMember(new RelationMember("node-20", n20));
        Way w30 = new Way(30, 1);
        Node n21;
        w30.addNode(n21 = new Node(21));
        Node n22;
        w30.addNode(n22 = new Node(22));
        r1.addMember(new RelationMember("way-30", w30));
        Relation r40 = new Relation(40);
        r1.addMember(new RelationMember("relation-40", r40));
        source.addPrimitive(n20);
        source.addPrimitive(n21);
        source.addPrimitive(n22);
        source.addPrimitive(w30);
        source.addPrimitive(r40);
        source.addPrimitive(r1);
        source.setSelected(r1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.getWays().size());
        assertEquals(1, hull.getNodes().size());
        assertEquals(2, hull.getRelations().size());

        OsmPrimitive p = hull.getPrimitiveById(1, OsmPrimitiveType.RELATION);
        assertNotNull(p);
        assertEquals(p.getClass(), Relation.class);

        Way w = (Way) hull.getPrimitiveById(30, OsmPrimitiveType.WAY);
        assertNotNull(w);
        assertTrue(w.isIncomplete());


        Node n = (Node) hull.getPrimitiveById(21, OsmPrimitiveType.NODE);
        assertNull(n);

        n = (Node) hull.getPrimitiveById(22, OsmPrimitiveType.NODE);
        assertNull(n);

        Relation r = (Relation) hull.getPrimitiveById(40, OsmPrimitiveType.RELATION);
        assertNotNull(r);
        assertTrue(r.isIncomplete());

        r = (Relation) hull.getPrimitiveById(1, OsmPrimitiveType.RELATION);
        assertNotNull(r);
        assertEquals(3, r.getMembersCount());
        RelationMember m = new RelationMember("node-20", hull.getPrimitiveById(20, OsmPrimitiveType.NODE));
        assertTrue(r.getMembers().contains(m));
        m = new RelationMember("way-30", hull.getPrimitiveById(30, OsmPrimitiveType.WAY));
        assertTrue(r.getMembers().contains(m));
        m = new RelationMember("relation-40", hull.getPrimitiveById(40, OsmPrimitiveType.RELATION));
        assertTrue(r.getMembers().contains(m));
    }

    @Test
    public void testOneRelationNewMembersNotSelected() {
        DataSet source = new DataSet();
        Relation r1 = new Relation();
        r1.put("name", "r1");
        Node n20 = new Node(new LatLon(20.0, 20.0));
        n20.put("name", "n20");
        r1.addMember(new RelationMember("node-20", n20));

        Way w30 = new Way();
        w30.put("name", "w30");
        Node n21;
        w30.addNode(n21 = new Node(new LatLon(21.0, 21.0)));
        n21.put("name", "n21");
        Node n22;
        w30.addNode(n22 = new Node(new LatLon(22.0, 22.0)));
        n22.put("name", "n22");
        r1.addMember(new RelationMember("way-30", w30));
        Relation r40 = new Relation();
        r40.put("name", "r40");
        r1.addMember(new RelationMember("relation-40", r40));

        source.addPrimitive(n20);
        source.addPrimitive(n21);
        source.addPrimitive(n22);
        source.addPrimitive(w30);
        source.addPrimitive(r40);
        source.addPrimitive(r1);
        source.setSelected(r1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.getWays().size());
        assertEquals(3, hull.getNodes().size());
        assertEquals(2, hull.getRelations().size());

        OsmPrimitive p = lookupByName(hull.getRelations(), "r1");
        assertNotNull(p);
        assertEquals(p.getClass(), Relation.class);

        Way w = (Way) lookupByName(hull.getWays(), "w30");
        assertNotNull(w);
        assertEquals(2, w.getNodesCount());

        Node n = (Node) lookupByName(hull.getNodes(), "n21");
        assertNotNull(n);
        assertTrue(w.containsNode(n));

        n = (Node) lookupByName(hull.getNodes(), "n22");
        assertNotNull(n);
        assertTrue(w.containsNode(n));

        Relation r = (Relation) lookupByName(hull.getRelations(), "r40");
        assertNotNull(r);

        r = (Relation) lookupByName(hull.getRelations(), "r1");
        assertNotNull(r);
        assertEquals(3, r.getMembersCount());
        RelationMember m = new RelationMember("node-20", lookupByName(hull.getNodes(), "n20"));
        assertTrue(r.getMembers().contains(m));
        m = new RelationMember("way-30", lookupByName(hull.getWays(), "w30"));
        assertTrue(r.getMembers().contains(m));
        m = new RelationMember("relation-40", lookupByName(hull.getRelations(), "r40"));
        assertTrue(r.getMembers().contains(m));
    }

    @Test
    public void testOneRelationExistingRecursive() {
        DataSet source = new DataSet();
        Relation r1 = new Relation(1, 1);
        r1.addMember(new RelationMember("relation-1", r1));
        source.addPrimitive(r1);
        source.setSelected(r1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.getRelations().size());

        Relation r = (Relation) hull.getPrimitiveById(1, OsmPrimitiveType.RELATION);
        assertNotNull(r);
        assertEquals(1, r.getMembersCount());
        assertTrue(r.getMembers().contains(new RelationMember("relation-1", r)));
    }

    @Test
    public void testOneRelationNewRecursive() {
        DataSet source = new DataSet();
        Relation r1 = new Relation();
        r1.put("name", "r1");
        r1.addMember(new RelationMember("relation-1", r1));
        source.addPrimitive(r1);
        source.setSelected(r1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.getRelations().size());

        Relation r = (Relation) lookupByName(hull.getRelations(), "r1");
        assertNotNull(r);
        assertEquals(1, r.getMembersCount());
        assertTrue(r.getMembers().contains(new RelationMember("relation-1", r)));
    }

    @Test
    public void testTwoRelationExistingCircular() {
        DataSet source = new DataSet();
        Relation r1 = new Relation(1, 1);
        source.addPrimitive(r1);
        Relation r2 = new Relation(2, 3);
        source.addPrimitive(r2);
        r1.addMember(new RelationMember("relation-2", r2));
        r2.addMember(new RelationMember("relation-1", r1));
        source.setSelected(r1, r2);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(2, hull.getRelations().size());

        r1 = (Relation) hull.getPrimitiveById(1, OsmPrimitiveType.RELATION);
        assertNotNull(r1);
        r2 = (Relation) hull.getPrimitiveById(2, OsmPrimitiveType.RELATION);
        assertNotNull(r2);
        assertEquals(1, r1.getMembersCount());
        assertTrue(r1.getMembers().contains(new RelationMember("relation-2", r2)));
        assertEquals(1, r2.getMembersCount());
        assertTrue(r2.getMembers().contains(new RelationMember("relation-1", r1)));
    }
}
