// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of "Public Transport Route" validation test.
 */
public class PublicTransportRouteTestTest {

    final PublicTransportRouteTest test = new PublicTransportRouteTest();

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules();

    /**
     * Performs various tests.
     */
    @Test
    public void testVarious() {
        final List<Node> nodes = Arrays.asList(new Node(), new Node(), new Node(), new Node(), new Node(), new Node());
        final Way w1 = TestUtils.newWay("", nodes.get(0), nodes.get(1));
        final Way w2 = TestUtils.newWay("", nodes.get(1), nodes.get(2));
        final Way w3 = TestUtils.newWay("", nodes.get(3), nodes.get(2));
        final Way w4 = TestUtils.newWay("", nodes.get(3), nodes.get(4));

        test.startTest(null);
        test.visit(TestUtils.newRelation("type=route route=tram public_transport:version=2"));
        test.visit(TestUtils.newRelation("type=unknown"));
        assertEquals(0, test.getErrors().size());

        final Relation r2 = TestUtils.newRelation("type=route route=tram public_transport:version=2",
                new RelationMember("", w1), new RelationMember("", w2), new RelationMember("", w3), new RelationMember("", w4));
        test.startTest(null);
        test.visit(r2);
        assertEquals(0, test.getErrors().size());

        final Relation r3 = TestUtils.newRelation("type=route route=tram public_transport:version=2",
                new RelationMember("forward", w1));
        test.startTest(null);
        test.visit(r3);
        assertEquals(1, test.getErrors().size());
        assertEquals("Route relation contains a 'forward/backward/alternate' role", test.getErrors().get(0).getMessage());

        final Relation r4 = TestUtils.newRelation("type=route route=tram public_transport:version=2",
                new RelationMember("", w1), new RelationMember("", w3), new RelationMember("", w2));
        test.startTest(null);
        test.visit(r4);
        assertEquals(1, test.getErrors().size());
        assertEquals("Route relation contains a gap", test.getErrors().get(0).getMessage());

        final Relation r5 = TestUtils.newRelation("type=route route=tram public_transport:version=2",
                new RelationMember("", w1), new RelationMember("", w2), new RelationMember("", w3),
                new RelationMember("stop", w1.firstNode()), new RelationMember("stop", w4.lastNode()));
        test.startTest(null);
        test.visit(r5);
        assertEquals(1, test.getErrors().size());
        assertEquals("Stop position not part of route", test.getErrors().get(0).getMessage());
        assertEquals(w4.lastNode(), test.getErrors().get(0).getPrimitives().iterator().next());

    }
}
