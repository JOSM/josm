// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

import static org.junit.Assert.*;

public class MergeSourceBuildingVisitorTest {

    protected OsmPrimitive lookupByName(Collection<? extends OsmPrimitive> primitives, String name) {
        if (primitives == null) return null;
        if (name == null) return null;
        for (OsmPrimitive primitive: primitives) {
            if (name.equals(primitive.get("name")))
                return primitive;
        }
        return null;
    }

    @Test
    public void test_Nodes() {
        DataSet source = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(new LatLon(10.0,10.0));
        n2.put("name","n2");
        Node n3 = new Node(3);
        Node n4 = new Node(new LatLon(20.0,20.0));
        n4.put("name","n4");
        source.nodes.add(n1);
        source.nodes.add(n2);
        source.nodes.add(n3);
        source.nodes.add(n4);
        source.setSelected(n1,n2);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(2, hull.nodes.size());

        OsmPrimitive p = hull.getPrimitiveById(1);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(3);
        assertNull(p);

        p = lookupByName(hull.nodes, "n2");
        assertNotNull(p);

        p = lookupByName(hull.nodes, "n4");
        assertNull(p);
    }


    @Test
    public void test_OneWay() {
        DataSet source = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Way w1 = new Way(3);
        w1.nodes.add(n1);
        w1.nodes.add(n2);
        source.nodes.add(n1);
        source.nodes.add(n2);
        source.ways.add(w1);
        source.setSelected(w1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.ways.size());
        assertEquals(2, hull.nodes.size());

        OsmPrimitive p = hull.getPrimitiveById(1);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(2);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(3);
        assertNotNull(p);
        assertEquals(p.getClass(), Way.class);
    }

    @Test
    public void test_OneWay_NodesSelectedToo() {
        DataSet source = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Way w1 = new Way(3);
        w1.nodes.add(n1);
        w1.nodes.add(n2);
        source.nodes.add(n1);
        source.nodes.add(n2);
        source.ways.add(w1);
        source.setSelected(w1,n1,n2);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.ways.size());
        assertEquals(2, hull.nodes.size());

        OsmPrimitive p = hull.getPrimitiveById(1);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(2);
        assertNotNull(p);
        assertEquals(p.getClass(), Node.class);

        p = hull.getPrimitiveById(3);
        assertNotNull(p);
        assertEquals(p.getClass(), Way.class);
    }

    @Test
    public void test_OneWay_Incomplete() {
        DataSet source = new DataSet();
        Way w1 = new Way(3);
        w1.incomplete = true;
        source.ways.add(w1);
        source.setSelected(w1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.ways.size());

        OsmPrimitive p = hull.getPrimitiveById(3);
        assertNotNull(p);
        assertEquals(p.getClass(), Way.class);
        assertTrue(p.incomplete);
    }

    @Test
    public void test_OneRelation_ExistingMembersSelected() {
        DataSet source = new DataSet();
        Relation r1 = new Relation(1);
        Node n20 = new Node(20);
        r1.members.add(new RelationMember("node-20",n20));
        Way w30 = new Way(30);
        Node n21;
        w30.nodes.add(n21 = new Node(21));
        Node n22;
        w30.nodes.add(n22 = new Node(22));
        r1.members.add(new RelationMember("way-30",w30));
        Relation r40 = new Relation(40);
        r1.members.add(new RelationMember("relation-40", r40));
        source.nodes.add(n20);
        source.nodes.add(n21);
        source.nodes.add(n22);
        source.ways.add(w30);
        source.relations.add(r1);
        source.relations.add(r40);
        source.setSelected(r1,n20,w30,r40);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.ways.size());
        assertEquals(3, hull.nodes.size());
        assertEquals(2, hull.relations.size());

        OsmPrimitive p = hull.getPrimitiveById(1);
        assertNotNull(p);
        assertEquals(p.getClass(), Relation.class);

        Way w = (Way)hull.getPrimitiveById(30);
        assertNotNull(w);
        assertEquals(2, w.nodes.size());
        Node n = (Node)hull.getPrimitiveById(21);
        assertNotNull(n);
        assertTrue(w.nodes.contains(n));

        n = (Node)hull.getPrimitiveById(22);
        assertNotNull(n);
        assertTrue(w.nodes.contains(n));

        Relation r = (Relation)hull.getPrimitiveById(40);
        assertNotNull(r);

        r = (Relation)hull.getPrimitiveById(1);
        assertNotNull(r);
        assertEquals(3, r.members.size());
        RelationMember m = new RelationMember("node-20", hull.getPrimitiveById(20));
        assertTrue(r.members.contains(m));
        m = new RelationMember("way-30", hull.getPrimitiveById(30));
        assertTrue(r.members.contains(m));
        m = new RelationMember("relation-40", hull.getPrimitiveById(40));
        assertTrue(r.members.contains(m));
    }

    @Test
    public void test_OneRelation_ExistingMembersNotSelected() {
        DataSet source = new DataSet();
        Relation r1 = new Relation(1);
        Node n20 = new Node(20);
        r1.members.add(new RelationMember("node-20",n20));
        Way w30 = new Way(30);
        Node n21;
        w30.nodes.add(n21 = new Node(21));
        Node n22;
        w30.nodes.add(n22 = new Node(22));
        r1.members.add(new RelationMember("way-30",w30));
        Relation r40 = new Relation(40);
        r1.members.add(new RelationMember("relation-40", r40));
        source.nodes.add(n20);
        source.nodes.add(n21);
        source.nodes.add(n22);
        source.ways.add(w30);
        source.relations.add(r1);
        source.relations.add(r40);
        source.setSelected(r1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.ways.size());
        assertEquals(1, hull.nodes.size());
        assertEquals(2, hull.relations.size());

        OsmPrimitive p = hull.getPrimitiveById(1);
        assertNotNull(p);
        assertEquals(p.getClass(), Relation.class);

        Way w = (Way)hull.getPrimitiveById(30);
        assertNotNull(w);
        assertTrue(w.incomplete);


        Node n = (Node)hull.getPrimitiveById(21);
        assertNull(n);

        n = (Node)hull.getPrimitiveById(22);
        assertNull(n);

        Relation r = (Relation)hull.getPrimitiveById(40);
        assertNotNull(r);
        assertTrue(r.incomplete);

        r = (Relation)hull.getPrimitiveById(1);
        assertNotNull(r);
        assertEquals(3, r.members.size());
        RelationMember m = new RelationMember("node-20", hull.getPrimitiveById(20));
        assertTrue(r.members.contains(m));
        m = new RelationMember("way-30", hull.getPrimitiveById(30));
        assertTrue(r.members.contains(m));
        m = new RelationMember("relation-40", hull.getPrimitiveById(40));
        assertTrue(r.members.contains(m));
    }

    @Test
    public void test_OneRelation_NewMembersNotSelected() {
        DataSet source = new DataSet();
        Relation r1 = new Relation();
        r1.put("name", "r1");
        Node n20 = new Node(new LatLon(20.0,20.0));
        n20.put("name", "n20");
        r1.members.add(new RelationMember("node-20",n20));
        Way w30 = new Way();
        w30.put("name", "w30");
        Node n21;
        w30.nodes.add(n21 = new Node(new LatLon(21.0,21.0)));
        n21.put("name","n21");
        Node n22;
        w30.nodes.add(n22 = new Node(new LatLon(22.0,22.0)));
        n22.put("name","n22");
        r1.members.add(new RelationMember("way-30",w30));
        Relation r40 = new Relation();
        r40.put("name", "r40");
        r1.members.add(new RelationMember("relation-40", r40));
        source.nodes.add(n20);
        source.nodes.add(n21);
        source.nodes.add(n22);
        source.ways.add(w30);
        source.relations.add(r1);
        source.relations.add(r40);
        source.setSelected(r1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.ways.size());
        assertEquals(3, hull.nodes.size());
        assertEquals(2, hull.relations.size());

        OsmPrimitive p = lookupByName(hull.relations, "r1");
        assertNotNull(p);
        assertEquals(p.getClass(), Relation.class);

        Way w = (Way)lookupByName(hull.ways, "w30");
        assertNotNull(w);
        assertEquals(2, w.nodes.size());

        Node n = (Node)lookupByName(hull.nodes, "n21");
        assertNotNull(n);
        assertTrue(w.nodes.contains(n));

        n = (Node)lookupByName(hull.nodes, "n22");
        assertNotNull(n);
        assertTrue(w.nodes.contains(n));

        Relation r = (Relation)lookupByName(hull.relations, "r40");
        assertNotNull(r);

        r = (Relation)lookupByName(hull.relations, "r1");
        assertNotNull(r);
        assertEquals(3, r.members.size());
        RelationMember m = new RelationMember("node-20", lookupByName(hull.nodes, "n20"));
        assertTrue(r.members.contains(m));
        m = new RelationMember("way-30", lookupByName(hull.ways, "w30"));
        assertTrue(r.members.contains(m));
        m = new RelationMember("relation-40", lookupByName(hull.relations, "r40"));
        assertTrue(r.members.contains(m));
    }

    @Test
    public void test_OneRelation_Existing_Recursive() {
        DataSet source = new DataSet();
        Relation r1 = new Relation(1);
        r1.members.add(new RelationMember("relation-1",r1));
        source.relations.add(r1);
        source.setSelected(r1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.relations.size());

        Relation r = (Relation)hull.getPrimitiveById(1);
        assertNotNull(r);
        assertEquals(1, r.members.size());
        assertTrue(r.members.contains(new RelationMember("relation-1",r)));
    }

    @Test
    public void test_OneRelation_New_Recursive() {
        DataSet source = new DataSet();
        Relation r1 = new Relation();
        r1.put("name", "r1");
        r1.members.add(new RelationMember("relation-1",r1));
        source.relations.add(r1);
        source.setSelected(r1);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(1, hull.relations.size());

        Relation r = (Relation)lookupByName(hull.relations, "r1");
        assertNotNull(r);
        assertEquals(1, r.members.size());
        assertTrue(r.members.contains(new RelationMember("relation-1",r)));
    }

    @Test
    public void test_TwoRelation_Existing_Circular() {
        DataSet source = new DataSet();
        Relation r1 = new Relation(1);
        Relation r2 = new Relation(2);
        r1.members.add(new RelationMember("relation-2",r2));
        r2.members.add(new RelationMember("relation-1",r1));
        source.relations.add(r1);
        source.relations.add(r2);
        source.setSelected(r1,r2);

        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(source);
        DataSet hull = builder.build();
        assertNotNull(hull);
        assertEquals(2, hull.relations.size());

        r1 = (Relation)hull.getPrimitiveById(1);
        assertNotNull(r1);
        r2 = (Relation)hull.getPrimitiveById(2);
        assertNotNull(r2);
        assertEquals(1, r1.members.size());
        assertTrue(r1.members.contains(new RelationMember("relation-2",r2)));
        assertEquals(1, r2.members.size());
        assertTrue(r2.members.contains(new RelationMember("relation-1",r1)));
    }
}
