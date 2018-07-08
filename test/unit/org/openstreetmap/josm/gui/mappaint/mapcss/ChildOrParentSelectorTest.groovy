// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss

import java.util.logging.Logger

import org.junit.*
import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.RelationMember
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.MultiCascade
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.ChildOrParentSelector
import org.openstreetmap.josm.io.OsmReader

class ChildOrParentSelectorTest {
    static private Logger logger = Logger.getLogger(ChildOrParentSelectorTest.class.getName());

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

    def way(id){
        def w = new Way(id,1)
        ds.addPrimitive(w)
        return w
    }

    def ChildOrParentSelector parse(css){
         MapCSSStyleSource source = new MapCSSStyleSource(css)
         source.loadStyleSource()
         assert source.rules.size() == 1
         return source.rules[0].selector
    }

    @Test
    @Ignore
    public void matches_1() {
        def css = """
           relation >[role="my_role"] node {}
        """
        ChildOrParentSelector selector = parse(css)

        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))
        Environment e = new Environment().withChild(n)

        assert selector.matches(e)
    }

    @Test
    @Ignore
    public void matches_2() {
        def css = """
           relation >["my_role"] node {}
        """
        ChildOrParentSelector selector = parse(css)

        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))
        Environment e = new Environment().withChild(n)

        assert selector.matches(e)
    }

    @Test
    @Ignore
    public void matches_3() {
        def css = """
           relation >[!"my_role"] node {}
        """
        ChildOrParentSelector selector = parse(css)

        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))
        Environment e = new Environment().withChild(n)

        assert !selector.matches(e)
    }

    @Test
    @Ignore
    public void matches_4() {
        def css = """
           way < relation {}
        """
        ChildOrParentSelector selector = parse(css)
        assert selector.type == Selector.ChildOrParentSelectorType.PARENT

    }
    @Test
    public void matches_5() {
        def css = """
           way <[role != "my_role"] relation {text: index();}
        """
        ChildOrParentSelector selector = parse(css)
        assert selector.type == Selector.ChildOrParentSelectorType.PARENT

        Relation r = relation(1)
        Way w1 = way(1)
        w1.setNodes([node(11), node(12)])

        Way w2 = way(2)
        w2.setNodes([node(21), node(22)])

        Way w3 = way(3)
        w3.setNodes([node(31), node(32)])

        r.addMember(new RelationMember("my_role", w1))
        r.addMember(new RelationMember("my_role", w2))
        r.addMember(new RelationMember("another role", w3))
        r.addMember(new RelationMember("yet another role", w3))

        Environment e = new Environment(r, new MultiCascade(), Environment.DEFAULT_LAYER, null)
        assert selector.matches(e)

        MapCSSStyleSource source = new MapCSSStyleSource(css)
        source.loadStyleSource()
        source.rules[0].declaration.execute(e)
        assert Float.valueOf(3f).equals(e.getCascade(Environment.DEFAULT_LAYER).get("text", null, Float.class))
    }

    @Test
    public void matches_6() {
        def css = """
           relation >[role != "my_role"] way {}
        """
        ChildOrParentSelector selector = parse(css)

        Relation r = relation(1)
        Way w1 = way(1)
        w1.setNodes([node(11), node(12)])

        Way w2 = way(2)
        w2.setNodes([node(21), node(22)])

        Way w3 = way(3)
        w3.setNodes([node(31), node(32)])

        r.addMember(new RelationMember("my_role", w1))
        r.addMember(new RelationMember("my_role", w2))
        r.addMember(new RelationMember("another role", w3))

        Environment e = new Environment(w1)
        assert !selector.matches(e)

        e = new Environment(w2)
        assert !selector.matches(e)

        e = new Environment(w3)
        assert selector.matches(e)
    }

    @Test
    public void testContains() throws Exception {
        def ds = OsmReader.parseDataSet(new FileInputStream("data_nodist/amenity-in-amenity.osm"), null)
        def css = parse("node[tag(\"amenity\") = parent_tag(\"amenity\")] ∈ *[amenity] {}")
        assert css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.WAY)))
        assert css.matches(new Environment(ds.getPrimitiveById(123, OsmPrimitiveType.RELATION)))
    }
}
