// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static groovy.test.GroovyAssert.shouldFail

import org.junit.*
import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmUtils
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.RelationMember
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op
import org.openstreetmap.josm.gui.mappaint.mapcss.parsergen.MapCSSParser

class KeyValueConditionTest {

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
        Condition c = ConditionFactory.createKeyValueCondition("a key", "a value", Op.EQ, Context.PRIMITIVE, false)

        c = ConditionFactory.createKeyValueCondition("role", "a role", Op.EQ, Context.LINK, false)
        c = ConditionFactory.createKeyValueCondition("RoLe", "a role", Op.EQ, Context.LINK, false)

        shouldFail(MapCSSException) {
            c = ConditionFactory.createKeyValueCondition("an arbitry tag", "a role", Op.EQ, Context.LINK, false)
        }
    }

    @Test
    public void applies_1() {
        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))

        Environment e = new Environment(n).withParent(r).withLinkContext().withIndex(0, r.membersCount)

        Condition cond = new ConditionFactory.RoleCondition("my_role", Op.EQ)
        assert cond.applies(e)

        cond = new ConditionFactory.RoleCondition("another_role", Op.EQ)
        assert !cond.applies(e)
    }

    @Test
    public void applies_2() {
        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))

        Environment e = new Environment(n).withParent(r).withIndex(0, r.membersCount).withLinkContext()

        Condition cond = ConditionFactory.createKeyValueCondition("role", "my_role", Op.NEQ, Context.LINK, false)
        assert !cond.applies(e)

        cond = ConditionFactory.createKeyValueCondition("role", "another_role", Op.NEQ, Context.LINK, false)
        assert cond.applies(e)
    }

    @Test
    public void testKeyRegexValueRegex() throws Exception {
        def selPos = new MapCSSParser(new StringReader("*[/^source/ =~ /.*,.*/]")).selector()
        def selNeg = new MapCSSParser(new StringReader("*[/^source/ !~ /.*,.*/]")).selector()
        assert !selPos.matches(new Environment(OsmUtils.createPrimitive("way foo=bar")))
        assert selPos.matches(new Environment(OsmUtils.createPrimitive("way source=1,2")))
        assert selPos.matches(new Environment(OsmUtils.createPrimitive("way source_foo_bar=1,2")))
        assert !selPos.matches(new Environment(OsmUtils.createPrimitive("way source=1")))
        assert !selPos.matches(new Environment(OsmUtils.createPrimitive("way source=1")))
        assert !selNeg.matches(new Environment(OsmUtils.createPrimitive("way source=1,2")))
        assert !selNeg.matches(new Environment(OsmUtils.createPrimitive("way foo=bar source=1,2")))
        assert selNeg.matches(new Environment(OsmUtils.createPrimitive("way foo=bar source=baz")))
        assert selNeg.matches(new Environment(OsmUtils.createPrimitive("way foo=bar src=1,2")))
    }

    @Test
    public void testValueFive() throws Exception {
        // ticket #5985
        def sel = new MapCSSParser(new StringReader("*[width=5]")).selector()
        assert sel.matches(new Environment(OsmUtils.createPrimitive("way highway=track width=5")))
        assert !sel.matches(new Environment(OsmUtils.createPrimitive("way highway=track width=2")))
    }

    @Test
    public void testValueZero() throws Exception {
        // ticket #12267
        def sel = new MapCSSParser(new StringReader("*[frequency=0]")).selector()
        assert sel.matches(new Environment(OsmUtils.createPrimitive("way railway=rail frequency=0")))
        assert !sel.matches(new Environment(OsmUtils.createPrimitive("way railway=rail frequency=50")))
    }
}
