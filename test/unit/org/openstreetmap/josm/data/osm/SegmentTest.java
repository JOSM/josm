// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.testframework.MotherObject;

public class SegmentTest extends MotherObject {

	private Segment segment;

	@Override protected void setUp() throws Exception {
		super.setUp();
		segment = createSegment();
	}

	public void testVisit() {
		OsmPrimitiveTest.TestCalledVisitor v = new OsmPrimitiveTest.TestCalledVisitor();
		segment.visit(v);
		assertEquals("Segment", v.called);
	}

	public void testCloneFromRealEqual() {
		Segment s2 = createSegment(23, createNode(1,2,3), createNode(2,3,4));
		segment.cloneFrom(s2);
		assertTrue(segment.realEqual(s2, false));
		assertTrue(s2.realEqual(segment, false));
		assertSame(segment.from, s2.from);
		assertSame(segment.to, s2.to);
	}

	public void testSegmentSegment() {
		Segment s = new Segment(segment);
		assertTrue(s.realEqual(segment, false));
	}

	public void testSegmentNodeNode() {
		Segment s = new Segment(createNode(1,2,3), createNode(4,5,6));
		assertEquals(2.0, s.from.coor.lat());
		assertEquals(6.0, s.to.coor.lon());
	}

	public void testSegmentLong() {
		Segment s = new Segment(23);
		assertEquals(23, s.id);
	}

	public void testEqualPlace() {
		Segment s = createSegment();
		assertFalse(s.equalPlace(segment));
		assertFalse(segment.equalPlace(s));
		s.from.coor = segment.to.coor;
		s.to.coor = segment.from.coor;
		assertTrue(s.equalPlace(segment));
		assertTrue(segment.equalPlace(s));
	}

	public void testCompareToSegmentSmallerThanNodeBiggerThanWayOrCompareAfterId() {
		Segment s = createSegment(23);
		assertEquals(-1, s.compareTo(createNode()));
		assertEquals(1, s.compareTo(createWay()));
		
		assertEquals(1, s.compareTo(segment));
		assertEquals(-1, segment.compareTo(s));
		segment.id = s.id;
		assertEquals(0, segment.compareTo(s));
		assertEquals(0, s.compareTo(segment));
	}
}
