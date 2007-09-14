// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import java.util.Date;

import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.testframework.MotherObject;

public class OsmPrimitiveTest extends MotherObject {

	public final static class TestCalledVisitor implements Visitor {
        public String called;
    	
    	public void visit(Node n) {
    		assertNull(called);
    		called = "Node";
        }
    
        public void visit(Segment s) {
        	assertNull(called);
        	called = "Segment";
        }
    
        public void visit(Way w) {
        	assertNull(called);
        	called = "Way";
        }
    }

	private OsmPrimitive osm;
	private boolean visited;

	@Override protected void setUp() throws Exception {
		super.setUp();
		osm = new OsmPrimitive() {
			@Override public void visit(Visitor visitor) {
				visited = true;
			}

			public int compareTo(OsmPrimitive o) {
				return 0;
			}
		};
		visited = false;
	}

	public void testVisit() {
		osm.visit(new Visitor(){
			public void visit(Node n) {}
			public void visit(Segment s) {}
			public void visit(Way w) {}});
		assertTrue(visited);
	}

	public void testEqualsIsEqualOnlyIfIdAndClassMatchesAndIdIsNotZero() {
		Node node = createNode(1,23,42);
		Node node2 = createNode(1);
		assertTrue(node.equals(node2));
		Segment segment = createSegment(1);
		assertFalse(node.equals(segment));
		node2.id = 2;
		assertFalse(node.equals(node2));
		node2.id = 0;
		node.id = 0;
		assertFalse(node.equals(node2));
	}

	public void testKeysPutRemoveGet() {
		assertTrue(osm.keySet().isEmpty());
		osm.put("foo", "bar");
		assertEquals(1, osm.keySet().size());
		assertEquals("bar", osm.get("foo"));
		assertEquals(1, osm.entrySet().size());
		assertEquals("foo", osm.entrySet().iterator().next().getKey());
		assertEquals("bar", osm.entrySet().iterator().next().getValue());
		
		osm.remove("asd");
		assertEquals(1, osm.keySet().size());
		osm.remove("foo");
		assertEquals(0, osm.keySet().size());
	}

	public void testGetTimeStr() {
		assertNull(osm.getTimeStr());
		osm.timestamp = new Date(1);
		assertEquals("1970-01-01 01:00:00", osm.getTimeStr());
	}
}
