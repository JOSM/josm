// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.*

import java.util.logging.Logger

import org.junit.*
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.RelationMember
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.fixtures.JOSMFixture
import org.openstreetmap.josm.gui.mappaint.Environment
import org.openstreetmap.josm.gui.mappaint.mapcss.Selector.ChildOrParentSelector

class ChildOrParentSelectorTest {
    static private Logger logger = Logger.getLogger(ChildOrParentSelectorTest.class.getName());
    
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
    
    def way(id){
        def w = new Way(id,1)
        ds.addPrimitive(w)
        return w
    }
    
    def ChildOrParentSelector parse(css){
         MapCSSStyleSource source = new MapCSSStyleSource(css)
         source.loadStyleSource()
         assert source.rules.size() == 1
         assert source.rules[0].selectors.size() == 1
         return source.rules[0].selectors[0]
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
        assert selector.parentSelector
        
    }
    @Test
    public void matches_5() {
        def css = """
           way <[role != "my_role"] relation {}
        """
        ChildOrParentSelector selector = parse(css)   
        assert selector.parentSelector     
        
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
        
        Environment e = new Environment().withPrimitive(r)
        assert selector.matches(e)
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
        
        Environment e = new Environment().withPrimitive(w1)
        assert !selector.matches(e)
        
        e = new Environment().withPrimitive(w2)
        assert !selector.matches(e)
        
        e = new Environment().withPrimitive(w3)
        assert selector.matches(e)
    }
}
