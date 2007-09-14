// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.testframework;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import junit.framework.TestCase;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Epsg4326;

abstract public class MotherObject extends TestCase {

	public static DataSet dataSet;
	
	@Override protected void setUp() throws Exception {
	    super.setUp();
	    Main.proj = new Epsg4326();
	    Main.ds = new DataSet();
    }

	public static Node createNode(int id) {
		return createNode(id, 0, 0);
	}
	
	public static Node createNode(int id, double lat, double lon) {
		Node n = createNode(lat, lon);
		n.id = id;
		return n;
	}

	public static Node createNode() {
		return createNode(Math.random()*360-180, Math.random()*180-90);
	}

	public static Node createNode(double lat, double lon) {
	    Node node = new Node(new LatLon(lat,lon));
	    if (dataSet != null)
	    	dataSet.nodes.add(node);
		return node;
    }
	
	
	public static Segment createSegment(long id) {
		Segment s = createSegment();
		s.id = id;
		return s;
	}
	public static Segment createSegment(long id, Node from, Node to) {
		Segment s = new Segment(from, to);
		s.id = id;
		return s;
	}
	public static Segment createSegment() {
		Segment segment = new Segment(createNode(), createNode());
		if (dataSet != null)
			dataSet.segments.add(segment);
		return segment;
	}
	
	
	public static Way createWay() {
		return createWay(0);
	}
	public static Way createWay(Segment... segments) {
		return createWay(0, segments);
	}
	public static Way createWay(long id, Segment... segments) {
		Way way = new Way();
		way.segments.addAll(Arrays.asList(segments));
		way.id = id;
		if (dataSet != null)
			dataSet.ways.add(way);
		return way;
	}
	
	public static DataSet createDataSet() {
	    DataSet ds = new DataSet();
		Node node1 = createNode();
		Node node2 = createNode();
		Node node3 = createNode();
		Segment segment = createSegment(23, node1, node2);
		Way way = createWay(42, segment);
		ds.nodes.add(node1);
		ds.nodes.add(node2);
		ds.nodes.add(node3);
		ds.segments.add(segment);
		ds.ways.add(way);
		return ds;
    }

	public static void assertContainsSame(Collection<OsmPrimitive> data, OsmPrimitive... all) {
		Collection<OsmPrimitive> copy = new LinkedList<OsmPrimitive>(data);
		copy.removeAll(Arrays.asList(all));
		assertEquals(0, copy.size());
    }
}
