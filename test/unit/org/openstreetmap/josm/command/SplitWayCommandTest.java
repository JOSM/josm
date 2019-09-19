// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.command.SplitWayCommand.Strategy;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link SplitWayCommand}.
 */
public final class SplitWayCommandTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection();

    /**
     * Unit test of {@link SplitWayCommand#findVias}.
     */
    @Test
    public void testFindVias() {
        // empty relation
        assertTrue(SplitWayCommand.findVias(new Relation(), null).isEmpty());
        // restriction relation without via member
        Relation r = new Relation();
        r.addMember(new RelationMember("", new Node()));
        assertTrue(SplitWayCommand.findVias(r, "restriction").isEmpty());
        // restriction relation with via member
        r = new Relation();
        OsmPrimitive via = new Node();
        r.addMember(new RelationMember("via", via));
        assertEquals(Collections.singletonList(via), SplitWayCommand.findVias(r, "restriction"));
        // destination_sign relation without sign nor intersection
        r = new Relation();
        r.addMember(new RelationMember("", new Node()));
        assertTrue(SplitWayCommand.findVias(r, "destination_sign").isEmpty());
        // destination_sign with sign
        r = new Relation();
        via = new Node();
        r.addMember(new RelationMember("sign", via));
        assertEquals(Collections.singletonList(via), SplitWayCommand.findVias(r, "destination_sign"));
        // destination_sign with intersection
        r = new Relation();
        via = new Node();
        r.addMember(new RelationMember("intersection", via));
        assertEquals(Collections.singletonList(via), SplitWayCommand.findVias(r, "destination_sign"));
    }

    /**
     * Unit tests of route relations.
     */
    @Test
    public void testRouteRelation() {
        doTestRouteRelation(false, 0);
        doTestRouteRelation(false, 1);
        doTestRouteRelation(false, 2);
        doTestRouteRelation(false, 3);
        doTestRouteRelation(true, 0);
        doTestRouteRelation(true, 1);
        doTestRouteRelation(true, 2);
        doTestRouteRelation(true, 3);
    }

    void doTestRouteRelation(final boolean wayIsReversed, final int indexOfWayToKeep) {
        final DataSet dataSet = new DataSet();
        final Node n1 = new Node(new LatLon(1, 0));
        final Node n2 = new Node(new LatLon(2, 0));
        final Node n3 = new Node(new LatLon(3, 0));
        final Node n4 = new Node(new LatLon(4, 0));
        final Node n5 = new Node(new LatLon(5, 0));
        final Node n6 = new Node(new LatLon(6, 0));
        final Node n7 = new Node(new LatLon(7, 0));
        final Way w1 = new Way();
        final Way w2 = new Way();
        final Way w3 = new Way();
        final Relation route = new Relation();
        for (OsmPrimitive p : Arrays.asList(n1, n2, n3, n4, n5, n6, n7, w1, w2, w3, route)) {
            dataSet.addPrimitive(p);
        }
        w1.setNodes(Arrays.asList(n1, n2));
        w2.setNodes(wayIsReversed
                ? Arrays.asList(n6, n5, n4, n3, n2)
                : Arrays.asList(n2, n3, n4, n5, n6)
        );
        w3.setNodes(Arrays.asList(n6, n7));
        route.put("type", "route");
        route.addMember(new RelationMember("", w1));
        route.addMember(new RelationMember("", w2));
        route.addMember(new RelationMember("", w3));
        dataSet.setSelected(Arrays.asList(w2, n3, n4, n5));

        final Strategy strategy = wayChunks -> {
                final Iterator<Way> it = wayChunks.iterator();
                for (int i = 0; i < indexOfWayToKeep; i++) {
                    it.next();
                }
                return it.next();
            };
        final SplitWayCommand result = SplitWayCommand.splitWay(
                w2, SplitWayCommand.buildSplitChunks(w2, Arrays.asList(n3, n4, n5)), new ArrayList<OsmPrimitive>(), strategy);
        UndoRedoHandler.getInstance().add(result);

        assertEquals(6, route.getMembersCount());
        assertEquals(w1, route.getMemberPrimitivesList().get(0));
        assertEquals(w3, route.getMemberPrimitivesList().get(5));
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(0)), n1);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(0)), n2);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(1)), n2);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(1)), n3);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(2)), n3);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(2)), n4);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(3)), n4);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(3)), n5);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(4)), n5);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(4)), n6);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(5)), n6);
        assertFirstLastNodeIs(((Way) route.getMemberPrimitivesList().get(5)), n7);
    }

    static void assertFirstLastNodeIs(Way way, Node node) {
        assertTrue("First/last node of " + way + " should be " + node, node.equals(way.firstNode()) || node.equals(way.lastNode()));
    }
}
