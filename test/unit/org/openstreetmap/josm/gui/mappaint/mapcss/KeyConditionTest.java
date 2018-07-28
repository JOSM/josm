// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyMatchType;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link KeyCondition}.
 */
public class KeyConditionTest {

    private DataSet ds;

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    /**
     * Setup test
     */
    @Before
    public void setUp() {
        ds = new DataSet();
    }

    Relation relation(int id) {
        Relation r = new Relation(id, 1);
        ds.addPrimitive(r);
        return r;
    }

    Node node(int id) {
        Node n = new Node(id, 1);
        n.setCoor(LatLon.ZERO);
        ds.addPrimitive(n);
        return n;
    }

    private static void shouldFail(Runnable r) {
        try {
            r.run();
            fail("should throw exception");
        } catch (MapCSSException e) {
            Logging.trace(e);
        }
    }

    /**
     * Test {@link ConditionFactory#createKeyCondition}.
     */
    @Test
    public void create() {

        // ["a label"]
        ConditionFactory.createKeyCondition("a key", false, KeyMatchType.FALSE, Context.PRIMITIVE);
        // ["a label"?]
        ConditionFactory.createKeyCondition("a key", false, KeyMatchType.TRUE, Context.PRIMITIVE);
        // [!"a label"]
        ConditionFactory.createKeyCondition("a key", true, KeyMatchType.FALSE, Context.PRIMITIVE);
        // [!"a label"?]
        ConditionFactory.createKeyCondition("a key", true, KeyMatchType.TRUE, Context.PRIMITIVE);

        // ["a label"]
        ConditionFactory.createKeyCondition("a key", false, null, Context.LINK);
        // [!"a label"]
        ConditionFactory.createKeyCondition("a key", true, null, Context.LINK);

        // ["a label"?]
        shouldFail(() ->
           ConditionFactory.createKeyCondition("a key", false, KeyMatchType.TRUE, Context.LINK)
        );

        // [!"a label"?]
        shouldFail(() ->
            ConditionFactory.createKeyCondition("a key", true, KeyMatchType.TRUE, Context.LINK)
        );
    }

    @Test
    public void applies_1() {
        Relation r = relation(1);
        Node n = node(1);
        r.addMember(new RelationMember("my_role", n));

        Environment e = new Environment(n).withParent(r).withIndex(0, r.getMembersCount()).withLinkContext();

        Condition cond = ConditionFactory.createKeyCondition("my_role", false, null, Context.LINK);
        assertTrue(cond.applies(e));

        cond = ConditionFactory.createKeyCondition("my_role", true, null, Context.LINK);
        assertFalse(cond.applies(e));
    }

    @Test
    public void applies_2() {
        Relation r = relation(1);
        Node n = node(1);
        r.addMember(new RelationMember("my_role", n));

        Environment e = new Environment(n).withParent(r).withIndex(0, r.getMembersCount()).withLinkContext();

        Condition cond = ConditionFactory.createKeyCondition("another_role", false, null, Context.LINK);
        assertFalse(cond.applies(e));

        cond = ConditionFactory.createKeyCondition("another_role", true, null, Context.LINK);
        assertTrue(cond.applies(e));
    }
}
