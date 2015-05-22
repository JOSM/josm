// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

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

class KeyConditionTest {

    def shouldFail = new GroovyTestCase().&shouldFail

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
        n.setCoor(new LatLon(0,0))
        ds.addPrimitive(n)
        return n
    }

    @Test
    public void create() {

        // ["a label"]
        Condition c = Condition.createKeyCondition("a key", false, Condition.KeyMatchType.FALSE, Context.PRIMITIVE)
        // ["a label"?]
        c = Condition.createKeyCondition("a key", false, Condition.KeyMatchType.TRUE, Context.PRIMITIVE)
        // [!"a label"]
        c = Condition.createKeyCondition("a key", true, Condition.KeyMatchType.FALSE, Context.PRIMITIVE)
        // [!"a label"?]
        c = Condition.createKeyCondition("a key", true, Condition.KeyMatchType.TRUE, Context.PRIMITIVE)

        // ["a label"]
        c = Condition.createKeyCondition("a key", false, null, Context.LINK)
        // [!"a label"]
        c = Condition.createKeyCondition("a key", true, null, Context.LINK)

        shouldFail(MapCSSException) {
            // ["a label"?]
           c = Condition.createKeyCondition("a key", false, Condition.KeyMatchType.TRUE, Context.LINK)
        }

        shouldFail(MapCSSException) {
            // [!"a label"?]
            c = Condition.createKeyCondition("a key", true, Condition.KeyMatchType.TRUE, Context.LINK)
        }
    }

    @Test
    public void applies_1() {
        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))

        Environment e = new Environment(n).withParent(r).withIndex(0, r.membersCount).withLinkContext()

        Condition cond = Condition.createKeyCondition("my_role", false, null, Context.LINK)
        assert cond.applies(e)

        cond = Condition.createKeyCondition("my_role", true, null, Context.LINK)
        assert !cond.applies(e)
    }

    @Test
    public void applies_2() {
        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))

        Environment e = new Environment(n).withParent(r).withIndex(0, r.membersCount).withLinkContext()

        Condition cond = Condition.createKeyCondition("another_role", false, null, Context.LINK)
        assert !cond.applies(e)

        cond = Condition.createKeyCondition("another_role", true, null, Context.LINK)
        assert cond.applies(e)
    }
}
