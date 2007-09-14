// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.testframework.MotherObject;

public class BoundingXYVisitorTest extends MotherObject {

	private BoundingXYVisitor v;

	private void assertVisitorFilled() {
        assertNotNull(v.max);
    	assertNotNull(v.min);
    }

	@Override protected void setUp() throws Exception {
		super.setUp();
		v = new BoundingXYVisitor();
	}

	public void testVisitNode() {
		Node node = createNode();
		node.visit(v);
		assertVisitorFilled();
		assertEquals(node.eastNorth.east(), v.min.east());
		assertEquals(node.eastNorth.north(), v.min.north());
		assertEquals(v.max, v.min);
	}

	public void testVisitSegment() {
		createSegment().visit(v);
		assertVisitorFilled();
		assertFalse(v.max.equals(v.min));
	}

	public void testVisitWay() {
		createWay(createSegment()).visit(v);
		assertVisitorFilled();
		assertFalse(v.max.equals(v.min));
	}

	public void testVisitEastNorth() {
		v.visit(new EastNorth(123,321));
		v.visit(new EastNorth(124,322));
		assertEquals(123.0, v.min.east());
		assertEquals(124.0, v.max.east());
		assertEquals(321.0, v.min.north());
		assertEquals(322.0, v.max.north());
	}

	public void testGetBounds() {
		Node node = createNode();
		v.visit(node);
		Bounds b = v.getBounds();
		assertNotNull(b);
		assertEquals(node.coor.lat(), b.min.lat());
		assertEquals(node.coor.lon(), b.min.lon());
	}

}
