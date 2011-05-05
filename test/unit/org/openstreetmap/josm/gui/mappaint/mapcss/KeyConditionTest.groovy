// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.junit.Assert.*

import org.junit.*
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.DataSet
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.RelationMember
import org.openstreetmap.josm.fixtures.JOSMFixture
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
        Condition c = Condition.create("a key", false, false, Context.PRIMITIVE)
        // ["a label"?]
        c = Condition.create("a key", false, true, Context.PRIMITIVE)
        // [!"a label"]
        c = Condition.create("a key", true, false, Context.PRIMITIVE)
        // [!"a label"?]
        c = Condition.create("a key", true, true, Context.PRIMITIVE)
       
        // ["a label"]
        c = Condition.create("a key", false, false, Context.LINK)
        // [!"a label"]
        c = Condition.create("a key", true, false, Context.LINK)
        
        shouldFail(MapCSSException) {
            // ["a label"?]
           c = Condition.create("a key", false, true, Context.LINK)
        }
        
        shouldFail(MapCSSException) {
            // [!"a label"?]
            c = Condition.create("a key", true, true, Context.LINK)
        }
    }
    
    @Test
    public void applies_1() {
        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))
        
        Environment e = new Environment().withPrimitive(n).withParent(r).withIndex(0).withLinkContext()
        
        Condition cond = Condition.create("my_role", false, false, Context.LINK)
        assert cond.applies(e)        
        
        cond = Condition.create("my_role", true, false, Context.LINK)
        assert !cond.applies(e)
    }
    
    @Test
    public void applies_2() {
        Relation r = relation(1)
        Node n = node(1)
        r.addMember(new RelationMember("my_role", n))
        
        Environment e = new Environment().withPrimitive(n).withParent(r).withIndex(0).withLinkContext()
        
        Condition cond = Condition.create("another_role", false, false, Context.LINK)
        assert !cond.applies(e)
        
        cond = Condition.create("another_role", true, false, Context.LINK)
        assert cond.applies(e)
    }    
}

