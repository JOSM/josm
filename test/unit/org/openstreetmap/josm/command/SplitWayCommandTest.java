// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.SplitWayCommand.Strategy;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
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
    public JOSMTestRules test = new JOSMTestRules().main().projection().preferences();

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
                w2, SplitWayCommand.buildSplitChunks(w2, Arrays.asList(n3, n4, n5)), new ArrayList<>(), strategy);
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

    @Test
    public void testOneMemberOrderedRelationShowsWarningTest() {
        final DataSet dataSet = new DataSet();

        // Positive IDs to mark that these ways are incomplete (i.e., no nodes loaded).
        final Way w1 = new Way(1);
        final Way w3 = new Way(3);

        // The way we are going to split is complete of course.
        final Node n1 = new Node(new LatLon(1, 0));
        final Node n2 = new Node(new LatLon(2, 0));
        final Node n3 = new Node(new LatLon(3, 0));
        final Way w2 = new Way();

        final Relation route = new Relation();
        for (OsmPrimitive p : Arrays.asList(n1, n2, n3, w1, w2, w3, route)) {
            dataSet.addPrimitive(p);
        }
        w2.setNodes(Arrays.asList(n1, n2, n3));

        route.put("type", "route");
        route.addMember(new RelationMember("", w1));
        route.addMember(new RelationMember("", w2));
        route.addMember(new RelationMember("", w3));
        dataSet.setSelected(Arrays.asList(w2, n2));

        // This split cannot be safely performed without downloading extra relation members.
        // Here we ask the split method to abort if it needs more information.
        final Optional<SplitWayCommand> result = SplitWayCommand.splitWay(
                w2,
                SplitWayCommand.buildSplitChunks(w2, Collections.singletonList(n2)),
                new ArrayList<>(),
                Strategy.keepLongestChunk(),
                SplitWayCommand.WhenRelationOrderUncertain.ABORT
        );

        assertFalse(result.isPresent());
    }

    @Test
    public void testDoIncompleteMembersOrderedRelationCorrectOrderTest() {
        for (int i = 0; i < 2; i++) {
            // All these permutations should result in a split that keeps the new parts in order.
            doIncompleteMembersOrderedRelationCorrectOrderTest(false, false, i);
            doIncompleteMembersOrderedRelationCorrectOrderTest(true, false, i);
            doIncompleteMembersOrderedRelationCorrectOrderTest(true, true, i);
            doIncompleteMembersOrderedRelationCorrectOrderTest(false, true, i);
        }
    }

    private void doIncompleteMembersOrderedRelationCorrectOrderTest(final boolean reverseWayOne,
                                                                    final boolean reverseWayTwo,
                                                                    final int indexOfWayToKeep) {
        final DataSet dataSet = new DataSet();

        // Positive IDs to mark that these ways are incomplete (i.e., no nodes loaded).
        final Way w1 = new Way(1);
        final Way w4 = new Way(3);

        // The ways we are going to split are complete of course.
        final Node n1 = new Node(new LatLon(1, 0));
        final Node n2 = new Node(new LatLon(2, 0));
        final Node n3 = new Node(new LatLon(3, 0));
        final Node n4 = new Node(new LatLon(4, 0));
        final Node n5 = new Node(new LatLon(5, 0));
        final Way w2 = new Way();
        final Way w3 = new Way();

        final Relation route = new Relation();
        for (OsmPrimitive p : Arrays.asList(n1, n2, n3, n4, n5, w1, w2, w3, w4, route)) {
            dataSet.addPrimitive(p);
        }
        w2.setNodes(reverseWayOne ? Arrays.asList(n3, n2, n1) : Arrays.asList(n1, n2, n3));
        w3.setNodes(reverseWayTwo ? Arrays.asList(n5, n4, n3) : Arrays.asList(n3, n4, n5));

        route.put("type", "route");
        route.addMember(new RelationMember("", w1));
        route.addMember(new RelationMember("", w2));
        route.addMember(new RelationMember("", w3));
        route.addMember(new RelationMember("", w4));

        Way splitWay = indexOfWayToKeep == 0 ? w2 : w3;
        Node splitNode = indexOfWayToKeep == 0 ? n2 : n4;

        dataSet.setSelected(Arrays.asList(splitWay, splitNode));

        final SplitWayCommand result = SplitWayCommand.splitWay(
                splitWay, SplitWayCommand.buildSplitChunks(splitWay, Collections.singletonList(splitNode)), new ArrayList<>());
        UndoRedoHandler.getInstance().add(result);

        assertEquals(5, route.getMembersCount());
        assertConnectedAtEnds(route.getMember(1).getWay(), route.getMember(2).getWay());
        assertConnectedAtEnds(route.getMember(2).getWay(), route.getMember(3).getWay());
    }

    static void assertFirstLastNodeIs(Way way, Node node) {
        assertTrue("First/last node of " + way + " should be " + node, node.equals(way.firstNode()) || node.equals(way.lastNode()));
    }

    static void assertConnectedAtEnds(Way one, Way two) {
        Node first1 = one.firstNode();
        Node last1 = one.lastNode();
        Node first2 = two.firstNode();
        Node last2 = two.lastNode();

        assertTrue("Ways expected to be connected at their ends.",
                first1 == first2 || first1 == last2 || last1 == first2 || last1 == last2);
    }

    /**
     * Non-regression test for patch #18596 (Fix relation ordering after split-way)
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket18596() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(18596, "data.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);

            Way splitWay = (Way) ds.getPrimitiveById(5, OsmPrimitiveType.WAY);
            Node splitNode = (Node) ds.getPrimitiveById(100002, OsmPrimitiveType.NODE);

            final SplitWayCommand result = SplitWayCommand.splitWay(
                    splitWay,
                    SplitWayCommand.buildSplitChunks(splitWay, Collections.singletonList(splitNode)),
                    new ArrayList<>()
            );

            UndoRedoHandler.getInstance().add(result);

            Relation relation = (Relation) ds.getPrimitiveById(8888, OsmPrimitiveType.RELATION);

            assertEquals(relation.getMembersCount(), 8);

            // Before the patch introduced in #18596, these asserts would fail. The two parts of
            // way '5' would be in the wrong order, breaking the boundary relation in this test.
            assertConnectedAtEnds(relation.getMember(4).getWay(), relation.getMember(5).getWay());
            assertConnectedAtEnds(relation.getMember(5).getWay(), relation.getMember(6).getWay());
        }
    }

    /**
     * Non-regression test for issue #17400 (Warn when splitting way in not fully downloaded region)
     *
     * Bus route 190 gets broken when the split occurs, because the two new way parts are inserted in the relation in
     * the wrong order.
     *
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket17400() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(17400, "data.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);

            Way splitWay = (Way) ds.getPrimitiveById(253731928, OsmPrimitiveType.WAY);
            Node splitNode = (Node) ds.getPrimitiveById(29830834, OsmPrimitiveType.NODE);

            final Optional<SplitWayCommand> result = SplitWayCommand.splitWay(
                    splitWay,
                    SplitWayCommand.buildSplitChunks(splitWay, Collections.singletonList(splitNode)),
                    new ArrayList<>(),
                    Strategy.keepLongestChunk(),
                    // This split requires no additional downloads.
                    SplitWayCommand.WhenRelationOrderUncertain.ABORT
            );

            assertTrue(result.isPresent());

            UndoRedoHandler.getInstance().add(result.get());

            // 190 Hormersdorf-Thalheim-Stollberg.
            Relation relation = (Relation) ds.getPrimitiveById(2873422, OsmPrimitiveType.RELATION);

            // One more than the original 161.
            assertEquals(relation.getMembersCount(), 162);

            // Before the patch introduced in #18596, these asserts would fail. The new parts of
            // the Hauptstraße would be in the wrong order, breaking the bus route relation.
            // These parts should be connected, in their relation sequence: 74---75---76.
            // Before #18596 this would have been a broken connection: 74---75-x-76.
            assertConnectedAtEnds(relation.getMember(74).getWay(), relation.getMember(75).getWay());
            assertConnectedAtEnds(relation.getMember(75).getWay(), relation.getMember(76).getWay());
        }
    }

    /**
     * Non-regression test for issue #18863 (Asking for download of missing members when not needed)
     *
     * A split on node 4518025255 caused the 'download missing members?' dialog to pop up for relation 68745 (CB 2),
     * even though the way members next to the split way were already downloaded. This happened because this relation
     * does not have its members connected at all.
     *
     * This split should not trigger any download action at all.
     *
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket18863() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(18863, "data.osm.bz2")) {
            DataSet ds = OsmReader.parseDataSet(is, null);

            Way splitWay = (Way) ds.getPrimitiveById(290581177L, OsmPrimitiveType.WAY);
            Node splitNode = (Node) ds.getPrimitiveById(4518025255L, OsmPrimitiveType.NODE);

            final Optional<SplitWayCommand> result = SplitWayCommand.splitWay(
                    splitWay,
                    SplitWayCommand.buildSplitChunks(splitWay, Collections.singletonList(splitNode)),
                    new ArrayList<>(),
                    Strategy.keepLongestChunk(),
                    // This split requires no additional downloads. If any are needed, this command will fail.
                    SplitWayCommand.WhenRelationOrderUncertain.ABORT
            );

            // Should not result in aborting the split.
            assertTrue(result.isPresent());
        }
    }

    /**
     * Non-regression test for issue #19432 (AIOOB: Problem with member check with duplicate members)
     *
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket19432() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(19432, "josm_split_way_exception_example.osm.bz2")) {
            DataSet ds = OsmReader.parseDataSet(is, null);

            Way splitWay = (Way) ds.getPrimitiveById(632576744L, OsmPrimitiveType.WAY);
            Node splitNode = (Node) ds.getPrimitiveById(1523436358L, OsmPrimitiveType.NODE);

            final Optional<SplitWayCommand> result = SplitWayCommand.splitWay(
                    splitWay,
                    SplitWayCommand.buildSplitChunks(splitWay, Collections.singletonList(splitNode)),
                    new ArrayList<>(),
                    Strategy.keepLongestChunk(),
                    // This split requires additional downloads but problem occured before the download
                    SplitWayCommand.WhenRelationOrderUncertain.SPLIT_ANYWAY
            );

            // Should not result in aborting the split.
            assertTrue(result.isPresent());
        }
    }
}
