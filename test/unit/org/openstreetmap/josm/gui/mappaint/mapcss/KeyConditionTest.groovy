// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.*

import org.junit.*
import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.RelationMember
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.KeyMatchType

class KeyConditionTest {

    def DataSet ds;

    @BeforeClass
    public static void createJOSMFixture(){
        JOSMFixture.createUnitTestFixture().init()
    }

    @Before
    public void setUp() {
        ds = new DataSet()
    }

    def relation(id) {
        def r = new Relation(id,1)
        ds.addPrimitive(r)
        return r
    }

    def node(id) {
        def n = new Node(id,1)
        n.setCoor(LatLon.ZERO)
        ds.addPrimitive(n)
        return n
    }

    @Test
    public void create() {

        // ["a label"]
        Condition c = ConditionFactory.createKeyCondition("a key", false, KeyMatchType.FALSE, Context.PRIMITIVE)
        // ["a label"?]
        c = ConditionFactory.createKeyCondition("a key", false, KeyMatchType.TRUE, Context.PRIMITIVE)
        // [!"a label"]
        c = ConditionFactory.createKeyCondition("a key", true, KeyMatchType.FALSE, Context.PRIMITIVE)
        // [!"a label"?]
        c = ConditionFactory.createKeyCondition("a key", true, KeyMatchType.TRUE, Context.PRIMITIVE)

        // ["a label"]
        c = ConditionFactory.createKeyCondition("a key", false, null, Context.LINK)
        // [!"a label"]
        c = ConditionFactory.createKeyCondition("a key", true, null, Context.LINK)

        shouldFail(MapCSSException) {
            // ["a label"?]
           c = ConditionFactory.createKeyCondition("a key", false, KeyMatchType.TRUE, Context.LINK)
        }

        shouldFail(MapCSSException) {
            // [!"a label"?]
            c = ConditionFactory.createKeyCondition("a key", true, KeyMatchType.TRUE, Context.LINK)
        }
    }

    @Test
    public void applies_1() {
        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))

        Environment e = new Environment(n).withParent(r).withIndex(0, r.membersCount).withLinkContext()

        Condition cond = ConditionFactory.createKeyCondition("my_role", false, null, Context.LINK)
        assert cond.applies(e)

        cond = ConditionFactory.createKeyCondition("my_role", true, null, Context.LINK)
        assert !cond.applies(e)
    }

    @Test
    public void applies_2() {
        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))

        Environment e = new Environment(n).withParent(r).withIndex(0, r.membersCount).withLinkContext()

        Condition cond = ConditionFactory.createKeyCondition("another_role", false, null, Context.LINK)
        assert !cond.applies(e)

        cond = ConditionFactory.createKeyCondition("another_role", true, null, Context.LINK)
        assert cond.applies(e)
    }
}
