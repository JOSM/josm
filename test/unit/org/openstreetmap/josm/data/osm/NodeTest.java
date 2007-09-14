// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testframework.MotherObject;

public class NodeTest extends MotherObject {

	private Node node;

	@Override protected void setUp() throws Exception {
	    super.setUp();
	    node = createNode();
    }

	public void testVisit() {
		OsmPrimitiveTest.TestCalledVisitor v = new OsmPrimitiveTest.TestCalledVisitor();
		node.visit(v);
		assertEquals("Node", v.called);
	}

	public void testCloneFromRealEqual() {
		Node node2 = createNode(23,3,4);
		assertFalse(node2.realEqual(node, false));
		assertFalse(node.realEqual(node2, false));
		node.cloneFrom(node2);
		assertTrue(node2.realEqual(node, false));
		assertTrue(node.realEqual(node2, false));
	}

	public void testNodeNode() {
		Node node2 = new Node(node);
		assertTrue(node2.realEqual(node, false));
	}

	public void testNodeLatLon() {
		LatLon latLon = new LatLon(1,2);
		node = new Node(latLon);
		assertEquals(node.coor, latLon);
	}

	public void testCompareToNodeTypeBiggestOrComparedAfterId() {
		assertEquals(1, node.compareTo(createSegment()));
		assertEquals(1, node.compareTo(createWay()));
		Node node2 = createNode(23,1,2);
		assertEquals(-1, node.compareTo(node2));
		assertEquals(1, node2.compareTo(node));
	}
}
