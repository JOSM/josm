// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyValueCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op;
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link KeyValueCondition}.
 */
public class KeyValueConditionTest {

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

    @Test
    public void create() {
        ConditionFactory.createKeyValueCondition("a key", "a value", Op.EQ, Context.PRIMITIVE, false);

        ConditionFactory.createKeyValueCondition("role", "a role", Op.EQ, Context.LINK, false);
        ConditionFactory.createKeyValueCondition("RoLe", "a role", Op.EQ, Context.LINK, false);

        shouldFail(() ->
            ConditionFactory.createKeyValueCondition("an arbitry tag", "a role", Op.EQ, Context.LINK, false)
        );
    }

    @Test
    public void applies_1() {
        Relation r = relation(1);
        Node n = node(1);
        r.addMember(new RelationMember("my_role", n));

        Environment e = new Environment(n).withParent(r).withLinkContext().withIndex(0, r.getMembersCount());

        Condition cond = new ConditionFactory.RoleCondition("my_role", Op.EQ);
        assertTrue(cond.applies(e));

        cond = new ConditionFactory.RoleCondition("another_role", Op.EQ);
        assertFalse(cond.applies(e));
    }

    @Test
    public void applies_2() {
        Relation r = relation(1);
        Node n = node(1);
        r.addMember(new RelationMember("my_role", n));

        Environment e = new Environment(n).withParent(r).withIndex(0, r.getMembersCount()).withLinkContext();

        Condition cond = ConditionFactory.createKeyValueCondition("role", "my_role", Op.NEQ, Context.LINK, false);
        assertFalse(cond.applies(e));

        cond = ConditionFactory.createKeyValueCondition("role", "another_role", Op.NEQ, Context.LINK, false);
        assertTrue(cond.applies(e));
    }

    @Test
    public void testKeyRegexValueRegex() throws Exception {
        Selector selPos = new MapCSSParser(new StringReader("*[/^source/ =~ /.*,.*/]")).selector();
        Selector selNeg = new MapCSSParser(new StringReader("*[/^source/ !~ /.*,.*/]")).selector();
        assertFalse(selPos.matches(new Environment(OsmUtils.createPrimitive("way foo=bar"))));
        assertTrue(selPos.matches(new Environment(OsmUtils.createPrimitive("way source=1,2"))));
        assertTrue(selPos.matches(new Environment(OsmUtils.createPrimitive("way source_foo_bar=1,2"))));
        assertFalse(selPos.matches(new Environment(OsmUtils.createPrimitive("way source=1"))));
        assertFalse(selPos.matches(new Environment(OsmUtils.createPrimitive("way source=1"))));
        assertFalse(selNeg.matches(new Environment(OsmUtils.createPrimitive("way source=1,2"))));
        assertFalse(selNeg.matches(new Environment(OsmUtils.createPrimitive("way foo=bar source=1,2"))));
        assertTrue(selNeg.matches(new Environment(OsmUtils.createPrimitive("way foo=bar source=baz"))));
        assertTrue(selNeg.matches(new Environment(OsmUtils.createPrimitive("way foo=bar src=1,2"))));
    }

    @Test
    public void testValueFive() throws Exception {
        // ticket #5985
        Selector sel = new MapCSSParser(new StringReader("*[width=5]")).selector();
        assertTrue(sel.matches(new Environment(OsmUtils.createPrimitive("way highway=track width=5"))));
        assertFalse(sel.matches(new Environment(OsmUtils.createPrimitive("way highway=track width=2"))));
    }

    @Test
    public void testValueZero() throws Exception {
        // ticket #12267
        Selector sel = new MapCSSParser(new StringReader("*[frequency=0]")).selector();
        assertTrue(sel.matches(new Environment(OsmUtils.createPrimitive("way railway=rail frequency=0"))));
        assertFalse(sel.matches(new Environment(OsmUtils.createPrimitive("way railway=rail frequency=50"))));
    }
}
