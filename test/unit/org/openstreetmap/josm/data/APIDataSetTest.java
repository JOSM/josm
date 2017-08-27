// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.CyclicUploadDependencyException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link APIDataSet}.
 */
public class APIDataSetTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    @Test
    public void testOneNewRelationOnly() throws CyclicUploadDependencyException {
        Relation r = new Relation();
        r.put("name", "r1");
        DataSet ds = new DataSet();
        ds.addPrimitive(r);

        APIDataSet apiDataSet = new APIDataSet();
        apiDataSet.init(ds);
        apiDataSet.adjustRelationUploadOrder();
        List<OsmPrimitive> toAdd = apiDataSet.getPrimitivesToAdd();

        assertEquals(1, toAdd.size());
        assertEquals(r, toAdd.get(0));
    }

    @Test
    public void testNewParentChildPair() throws CyclicUploadDependencyException {
        DataSet ds = new DataSet();
        Relation r1 = new Relation();
        ds.addPrimitive(r1);
        r1.put("name", "r1");

        Relation r2 = new Relation();
        ds.addPrimitive(r2);
        r2.put("name", "r2");

        r1.addMember(new RelationMember("", r2));

        APIDataSet apiDataSet = new APIDataSet();
        apiDataSet.init(ds);
        apiDataSet.adjustRelationUploadOrder();
        List<OsmPrimitive> toAdd = apiDataSet.getPrimitivesToAdd();

        assertEquals(2, toAdd.size());
        assertEquals(r2, toAdd.get(0)); // child first
        assertEquals(r1, toAdd.get(1)); // ... then the parent
    }

    @Test
    public void testOneExistingAndThreNewInAChain() throws CyclicUploadDependencyException {
        DataSet ds = new DataSet();

        Relation r1 = new Relation();
        ds.addPrimitive(r1);
        r1.put("name", "r1");

        Relation r2 = new Relation();
        ds.addPrimitive(r2);
        r2.put("name", "r2");

        Relation r3 = new Relation();
        ds.addPrimitive(r3);
        r3.put("name", "r3");

        Relation r4 = new Relation(1, 1);
        ds.addPrimitive(r4);
        r4.put("name", "r4");
        r4.setModified(true);

        r1.addMember(new RelationMember("", r2));
        r2.addMember(new RelationMember("", r3));

        APIDataSet apiDataSet = new APIDataSet();
        apiDataSet.init(ds);
        apiDataSet.adjustRelationUploadOrder();
        List<OsmPrimitive> toAdd = apiDataSet.getPrimitivesToAdd();

        assertEquals(3, toAdd.size());
        assertEquals(r3, toAdd.get(0));
        assertEquals(r2, toAdd.get(1));
        assertEquals(r1, toAdd.get(2));

        List<OsmPrimitive> toUpdate = apiDataSet.getPrimitivesToUpdate();
        assertEquals(1, toUpdate.size());
        assertEquals(r4, toUpdate.get(0));

        final ConflictCollection cc4 = new ConflictCollection();
        cc4.add(r4, r4);
        assertTrue(apiDataSet.participatesInConflict(cc4));
        final ConflictCollection cc1 = new ConflictCollection();
        cc1.add(r1, r1);
        assertFalse(apiDataSet.participatesInConflict(cc1));
    }

    @Test
    public void testOneParentTwoNewChildren() throws CyclicUploadDependencyException {
        DataSet ds = new DataSet();
        Relation r1 = new Relation();
        ds.addPrimitive(r1);
        r1.put("name", "r1");

        Relation r2 = new Relation();
        ds.addPrimitive(r2);
        r2.put("name", "r2");

        Relation r3 = new Relation();
        ds.addPrimitive(r3);
        r3.put("name", "r3");

        r1.addMember(new RelationMember("", r2));
        r1.addMember(new RelationMember("", r3));


        APIDataSet apiDataSet = new APIDataSet();
        apiDataSet.init(ds);
        apiDataSet.adjustRelationUploadOrder();
        List<OsmPrimitive> toAdd = apiDataSet.getPrimitivesToAdd();

        assertEquals(3, toAdd.size());
        assertTrue(toAdd.indexOf(r2) < toAdd.indexOf(r1));
        assertTrue(toAdd.indexOf(r3) < toAdd.indexOf(r1));
    }

    @Test // for ticket #9624
    public void testDeleteOneParentTwoNewChildren() throws CyclicUploadDependencyException {
        DataSet ds = new DataSet();
        Relation r1 = new Relation(1);
        ds.addPrimitive(r1);
        r1.put("name", "r1");

        Relation r2 = new Relation(2);
        ds.addPrimitive(r2);
        r2.put("name", "r2");

        Relation r3 = new Relation(3);
        ds.addPrimitive(r3);
        r3.put("name", "r3");

        Relation r4 = new Relation(4);
        ds.addPrimitive(r4);
        r4.put("name", "unrelated");


        r1.addMember(new RelationMember("", r2));
        r1.addMember(new RelationMember("", r3));

        r1.setDeleted(true);
        r2.setDeleted(true);
        r3.setDeleted(true);
        r4.setDeleted(true);


        APIDataSet apiDataSet = new APIDataSet();
        // add r1 first to test functionality of APIDataSet#adjustRelationUploadOrder()
        apiDataSet.getPrimitivesToDelete().add(r1);
        apiDataSet.getPrimitivesToDelete().add(r2);
        apiDataSet.getPrimitivesToDelete().add(r3);
        apiDataSet.getPrimitivesToDelete().add(r4);
        apiDataSet.adjustRelationUploadOrder();
        List<OsmPrimitive> toDelete = apiDataSet.getPrimitivesToDelete();

        assertEquals(4, toDelete.size());
        assertTrue(toDelete.indexOf(r2) < toDelete.indexOf(r1));
        assertTrue(toDelete.indexOf(r3) < toDelete.indexOf(r1));
    }

    @Test // for ticket #9656
    public void testDeleteWay() throws CyclicUploadDependencyException {
        DataSet ds = new DataSet();
        final Way way = new Way(1, 2);
        way.put("highway", "unclassified");
        ds.addPrimitive(way);

        final Node n1 = new Node(2);
        ds.addPrimitive(n1);
        way.addNode(n1);

        final Node n2 = new Node(3);
        ds.addPrimitive(n2);
        way.addNode(n2);

        Relation r1 = new Relation(4, 2);
        ds.addPrimitive(r1);
        r1.put("name", "r1");
        r1.addMember(new RelationMember("foo", way));


        r1.setDeleted(true);
        way.setDeleted(true);
        n1.setDeleted(true);
        n2.setDeleted(true);

        APIDataSet apiDataSet = new APIDataSet();
        apiDataSet.init(ds);
        apiDataSet.adjustRelationUploadOrder();
        List<OsmPrimitive> toDelete = apiDataSet.getPrimitivesToDelete();

        assertEquals(4, toDelete.size());
        assertTrue(toDelete.indexOf(way) < toDelete.indexOf(n1));
        assertTrue(toDelete.indexOf(way) < toDelete.indexOf(n2));
        assertTrue(toDelete.indexOf(r1) < toDelete.indexOf(way));
    }

    @Test
    public void testOneCycle() {
        DataSet ds = new DataSet();
        Relation r1 = new Relation();
        ds.addPrimitive(r1);
        r1.put("name", "r1");

        Relation r2 = new Relation();
        ds.addPrimitive(r2);
        r2.put("name", "r2");

        Relation r3 = new Relation();
        ds.addPrimitive(r3);
        r3.put("name", "r3");

        r1.addMember(new RelationMember("", r2));
        r2.addMember(new RelationMember("", r3));
        r3.addMember(new RelationMember("", r1));

        APIDataSet apiDataSet = new APIDataSet();
        apiDataSet.init(ds);
        try {
            apiDataSet.adjustRelationUploadOrder();
            fail("expected cyclic upload dependency exception not thrown");
        } catch (CyclicUploadDependencyException e) {
            System.out.println(e);
        }
    }
}
