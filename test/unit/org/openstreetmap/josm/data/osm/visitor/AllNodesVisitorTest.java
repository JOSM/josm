// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testframework.MotherObject;

public class AllNodesVisitorTest extends MotherObject {

	private AllNodesVisitor v;

	@Override protected void setUp() throws Exception {
		super.setUp();
		v = new AllNodesVisitor();
	}

	public void testVisitNode() {
		Node node = createNode();
		node.visit(v);
		assertEquals(1, v.nodes.size());
		assertSame(node, v.nodes.iterator().next());
	}

	public void testVisitSegment() {
		Segment s = createSegment();
		s.visit(v);
		assertEquals(2, v.nodes.size());
		assertTrue(v.nodes.contains(s.from));
		assertTrue(v.nodes.contains(s.to));
	}

	public void testVisitWay() {
		Way w = createWay(createSegment());
		w.visit(v);
		int numberOfNodes = 2*w.segments.size();
		assertEquals(numberOfNodes, v.nodes.size());
		for (Segment s : w.segments) {
			assertTrue(v.nodes.contains(s.from));
			assertTrue(v.nodes.contains(s.to));
		}
	}

	public void testGetAllNodes() {
		Collection<OsmPrimitive> all = new LinkedList<OsmPrimitive>();
		all.add(createNode());
		all.add(createSegment());
		Collection<Node> nodes = AllNodesVisitor.getAllNodes(all);
		
		assertEquals(3, nodes.size());
	}

}
