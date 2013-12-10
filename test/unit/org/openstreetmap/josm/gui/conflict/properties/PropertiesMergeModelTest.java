// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Observable;
import java.util.Observer;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.conflict.pair.properties.PropertiesMergeModel;

public class PropertiesMergeModelTest {

    static public class ObserverTest implements Observer {
        public int numInvocations;

        public void update(Observable o, Object arg) {
            numInvocations++;
            test();
        }

        public void test() {
        }

        public void assertNumInvocations(int count) {
            assertEquals(count, numInvocations);
        }
    }

    PropertiesMergeModel model;

    @BeforeClass
    public static void init() {
        Main.setProjection(Projections.getProjectionByCode("EPSG:4326"));
        Main.initApplicationPreferences();
    }

    @Before
    public void setUp() {
        model = new PropertiesMergeModel();
    }

    private void populate(OsmPrimitive my, OsmPrimitive their) {
        model.populate(new Conflict<OsmPrimitive>(my, their));
    }

    @Test
    public void populate() {
        DataSet d1 = new DataSet();
        DataSet d2 = new DataSet();
        Node n1 = new Node(1);
        Node n2 = new Node(1);
        d1.addPrimitive(n1);
        d2.addPrimitive(n2);
        populate(n1, n2);

        Way w1 = new Way(1);
        Way w2 = new Way(1);
        d1.addPrimitive(w1);
        d2.addPrimitive(w2);
        populate(w2, w2);

        Relation r1 = new Relation(1);
        Relation r2 = new Relation(1);
        d1.addPrimitive(r1);
        d2.addPrimitive(r2);
        populate(r1, r2);
    }

    @Test
    public void decidingAboutCoords() {
        DataSet d1 = new DataSet();
        DataSet d2 = new DataSet();

        Node n1 = new Node(1);
        Node n2 = new Node(1);
        d1.addPrimitive(n1);
        d2.addPrimitive(n2);
        populate(n1, n2);
        assertFalse(model.hasCoordConflict());

        n1.setCoor(new LatLon(1,1));
        populate(n1, n2);
        assertTrue(model.hasCoordConflict());


        n1.cloneFrom(new Node(1));
        n2.setCoor(new LatLon(2,2));
        populate(n1, n2);
        assertTrue(model.hasCoordConflict());

        n1.setCoor(new LatLon(1,1));
        n2.setCoor(new LatLon(2,2));
        populate(n1, n2);
        assertTrue(model.hasCoordConflict());

        // decide KEEP_MINE  and ensure notification via Observable
        //
        ObserverTest observerTest;
        model.addObserver(
                observerTest = new ObserverTest() {
                    @Override
                    public void test() {
                        assertTrue(model.isCoordMergeDecision(MergeDecisionType.KEEP_MINE));
                    }
                }
        );
        model.decideCoordsConflict(MergeDecisionType.KEEP_MINE);
        assertTrue(model.isCoordMergeDecision(MergeDecisionType.KEEP_MINE));
        observerTest.assertNumInvocations(1);

        // decide KEEP_THEIR and  ensure notification via Observable
        //
        model.deleteObserver(observerTest);
        model.addObserver(
                observerTest = new ObserverTest() {
                    @Override
                    public void test() {
                        assertTrue(model.isCoordMergeDecision(MergeDecisionType.KEEP_THEIR));
                    }
                }
        );
        model.decideCoordsConflict(MergeDecisionType.KEEP_THEIR);
        assertTrue(model.isCoordMergeDecision(MergeDecisionType.KEEP_THEIR));
        observerTest.assertNumInvocations(1);
        model.deleteObserver(observerTest);
    }


}
