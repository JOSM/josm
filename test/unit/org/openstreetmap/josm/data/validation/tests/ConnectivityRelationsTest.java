// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test the ConnectivityRelations validation test
 *
 * @author Taylor Smock
 */
class ConnectivityRelationsTest {
    private ConnectivityRelations check;
    private static final String CONNECTIVITY = "connectivity";

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rule = new JOSMTestRules();

    @BeforeEach
    public void setUpCheck() throws Exception {
        check = new ConnectivityRelations();
    }

    private Relation createDefaultTestRelation() {
        Node connection = new Node(new LatLon(0, 0));
        return TestUtils.newRelation("type=connectivity connectivity=1:1",
                new RelationMember("from", TestUtils.newWay("lanes=4", new Node(new LatLon(-0.1, -0.1)), connection)),
                new RelationMember("via", connection),
                new RelationMember("to", TestUtils.newWay("lanes=4", connection, new Node(new LatLon(0.1, 0.1)))));
    }

    /**
     * Test for connectivity relations without a connectivity tag
     */
    @Test
    void testNoConnectivityTag() {
        Relation relation = createDefaultTestRelation();
        check.visit(relation);

        Assert.assertEquals(0, check.getErrors().size());

        relation.remove(CONNECTIVITY);
        check.visit(relation);
        Assert.assertEquals(1, check.getErrors().size());
    }

    /**
     * Check for lanes that don't make sense
     */
    @Test
    void testMisMatchedLanes() {
        Relation relation = createDefaultTestRelation();
        check.visit(relation);
        int expectedFailures = 0;

        Assert.assertEquals(expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "45000:1");
        check.visit(relation);
        Assert.assertEquals(++expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "1:45000");
        check.visit(relation);
        Assert.assertEquals(++expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "1:1,2");
        check.visit(relation);
        Assert.assertEquals(expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "1:1,(2)");
        check.visit(relation);
        Assert.assertEquals(expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "1:1,(20000)");
        check.visit(relation);
        Assert.assertEquals(++expectedFailures, check.getErrors().size());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/201821">Bug #20182</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20182() throws Exception {
        Relation relation = createDefaultTestRelation();
        check.visit(relation);
        int expectedFailures = 0;

        Assert.assertEquals(expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "left_turn");
        check.visit(relation);
        Assert.assertEquals(++expectedFailures, check.getErrors().size());

        relation.put(CONNECTIVITY, "1");
        check.visit(relation);
        Assert.assertEquals(++expectedFailures, check.getErrors().size());
    }
}
