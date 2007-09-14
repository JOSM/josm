// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.testframework;

import java.util.Arrays;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Mercator;


/**
 * Test cases that need to manupulate a data set can use this helper.
 *  
 * @author Imi
 */
public class DataSetTestCaseHelper {

	/**
	 * Create a common dataset consisting of:
	 * - 5 random nodes
	 * - ls between node 0 and 1
	 * - ls between node 1 and 2
	 * - ls between node 3 and 4
	 * - a way with ls 0 and 1
	 */
	public static DataSet createCommon() {
		DataSet ds = new DataSet();
		Node n1 = createNode(ds);
		Node n2 = createNode(ds);
		Node n3 = createNode(ds);
		Node n4 = createNode(ds);
		Node n5 = createNode(ds);
		Segment ls1 = createSegment(ds, n1, n2);
		Segment ls2 = createSegment(ds, n2, n3);
		createSegment(ds, n4, n5);
		createWay(ds, ls1, ls2);
		return ds;
	}

	public static Way createWay(DataSet ds, Segment... segments) {
		Way w = new Way();
		w.segments.addAll(Arrays.asList(segments));
		if (ds != null)
			ds.ways.add(w);
		return w;
	}
	
	/**
	 * Create a segment with out of the given nodes.
	 */
	public static Segment createSegment(DataSet ds, Node n1, Node n2) {
		Segment ls = new Segment(n1, n2);
		if (ds != null)
			ds.segments.add(ls);
		return ls;
	}

	/**
	 * Add a random node.
	 */
	public static Node createNode(DataSet ds) {
		if (Main.proj == null)
			Main.proj = new Mercator();
		Node node = new Node(new LatLon(Math.random(), Math.random()));
		if (ds != null)
			ds.nodes.add(node);
		return node;
	}

}
