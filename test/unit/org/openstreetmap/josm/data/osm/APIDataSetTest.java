// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.upload.CyclicUploadDependencyException;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.Preferences;


public class APIDataSetTest {

    @BeforeClass
    public static void init() {
        Main.pref = new Preferences();
    }

    @Test
    public void oneNewRelationOnly() {
        Relation r = new Relation();
        r.put("name", "r1");
        DataSet ds = new DataSet();
        ds.addPrimitive(r);

        APIDataSet apiDataSet = new APIDataSet();
        apiDataSet.init(ds);
        try {
            apiDataSet.adjustRelationUploadOrder();
        } catch(CyclicUploadDependencyException e) {
            fail("unexpected exception:" + e);
        }
        List<OsmPrimitive> toAdd = apiDataSet.getPrimitivesToAdd();

        assertEquals(1, toAdd.size());
        assertEquals(r, toAdd.get(0));
    }

    @Test
    public void newParentChildPair() {
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
        try {
            apiDataSet.adjustRelationUploadOrder();
        } catch(CyclicUploadDependencyException e) {
            fail("unexpected exception:" + e);
        }
        List<OsmPrimitive> toAdd = apiDataSet.getPrimitivesToAdd();

        assertEquals(2, toAdd.size());
        assertEquals(r2, toAdd.get(0)); // child first
        assertEquals(r1, toAdd.get(1)); // ... then the parent
    }

    @Test
    public void oneExistingAndThreNewInAChain() {
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
        try {
            apiDataSet.adjustRelationUploadOrder();
        } catch(CyclicUploadDependencyException e) {
            fail("unexpected exception:" + e);
        }
        List<OsmPrimitive> toAdd = apiDataSet.getPrimitivesToAdd();

        assertEquals(3, toAdd.size());
        assertEquals(r3, toAdd.get(0));
        assertEquals(r2, toAdd.get(1));
        assertEquals(r1, toAdd.get(2));

        List<OsmPrimitive> toUpdate = apiDataSet.getPrimitivesToUpdate();
        assertEquals(1, toUpdate.size());
        assertEquals(r4, toUpdate.get(0));
    }

    @Test
    public void oneParentTwoNewChildren() {
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
        try {
            apiDataSet.adjustRelationUploadOrder();
        } catch(CyclicUploadDependencyException e) {
            fail("unexpected exception:" + e);
        }
        List<OsmPrimitive> toAdd = apiDataSet.getPrimitivesToAdd();

        assertEquals(3, toAdd.size());
        assertEquals(true, toAdd.indexOf(r2) < toAdd.indexOf(r1));
        assertEquals(true, toAdd.indexOf(r3) < toAdd.indexOf(r1));
    }

    @Test
    public void oneCycle() {
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
        } catch(CyclicUploadDependencyException e) {
            System.out.println(e);
        }
    }
}
