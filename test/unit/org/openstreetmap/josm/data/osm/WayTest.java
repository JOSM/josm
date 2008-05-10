// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.testframework.MotherObject;

public class WayTest extends MotherObject {

	private Way way;

	@Override protected void setUp() throws Exception {
		super.setUp();
		way = createWay();
	}

	public void testVisit() {
		OsmPrimitiveTest.TestCalledVisitor v = new OsmPrimitiveTest.TestCalledVisitor();
		way.visit(v);
		assertEquals("Way", v.called);
	}

	public void testCloneFromRealEqual() {
		Way w2 = createWay(42);
		way.cloneFrom(w2);
		assertTrue(way.realEqual(w2, false));
		assertEquals(w2.segments.size(), way.segments.size());
	}

	public void testWayWay() {
		Way w = new Way(way);
		assertEquals(way.id, w.id);
		assertTrue(way.realEqual(w, false));
	}

	public void testWay() {
		Way w = new Way();
		assertEquals(0, w.id);
		assertEquals(0, w.segments.size());
	}

	public void testCompareToWaySmallestOrCompareAfterId() {
		Way w = createWay(23);
		assertEquals(-1, w.compareTo(createNode()));
		assertEquals(-1, w.compareTo(createSegment()));
		
		assertEquals(1, w.compareTo(way));
		assertEquals(-1, way.compareTo(w));
	}

	public void testIsIncomplete() {
		way.segments.add(new Segment(23));
		assertTrue(way.isIncomplete());
	}

}
