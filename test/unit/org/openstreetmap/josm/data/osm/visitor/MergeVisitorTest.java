// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Date;

import junit.framework.TestCase;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testframework.Bug;
import org.openstreetmap.josm.testframework.DataSetTestCaseHelper;

public class MergeVisitorTest extends TestCase {


	private DataSet ds;
	private Node dsNode;
	private Node n;
	private MergeVisitor v;

	@Override protected void setUp() throws Exception {
		ds = new DataSet();
		dsNode = DataSetTestCaseHelper.createNode(ds);
		v = new MergeVisitor(ds, null);
		n = DataSetTestCaseHelper.createNode(null);
	}


	private Segment createSegment(DataSet ds, boolean incomplete, boolean deleted, int id) {
    	Node n1 = DataSetTestCaseHelper.createNode(ds);
    	Node n2 = DataSetTestCaseHelper.createNode(ds);
    	Segment s = DataSetTestCaseHelper.createSegment(ds, n1, n2);
    	s.incomplete = incomplete;
    	s.id = id;
    	s.deleted = deleted;
    	s.timestamp = new Date();
    	return s;
    }

	/**
     * Create that amount of nodes and add them to the dataset. The id will be 1,2,3,4...
     * @param amount Number of nodes to create.
     * @return The created nodes.
     */
    private Node[] createNodes(DataSet ds, int amount) {
    	Node[] nodes = new Node[amount];
    	for (int i = 0; i < amount; ++i) {
    		nodes[i] = DataSetTestCaseHelper.createNode(ds);
    		nodes[i].id = i+1;
    	}
    	return nodes;
    }


	public void testNodesMergeUpdate() {
		dsNode.id = 1;
		n.id = 1;
		n.timestamp = new Date();
		v.visit(n);
		assertEquals(dsNode, n);
	}
	public void testNodesMergeModified() {
		dsNode.id = 1;
		n.id = 1;
		n.modified = true;
		v.visit(n);
		assertEquals(dsNode, n);
	}
	public void testNodesConflictBothModified() {
		n.modified = true;
		dsNode.modified = true;
		n.id = 1;
		dsNode.id = 1;
		v.visit(n);
		assertEquals(1, v.conflicts.size());
	}
	public void testNodesConflict() {
		dsNode.id = 1;
		dsNode.timestamp = new Date();
		n.id = 1;
		n.modified = true;
		n.timestamp = new Date(dsNode.timestamp.getTime()-1);
		v.visit(n);
		assertEquals(1, v.conflicts.size());
		assertSame(dsNode, v.conflicts.keySet().iterator().next());
		assertSame(n, v.conflicts.values().iterator().next());
	}
	public void testNodesConflict2() {
		dsNode.id = 1;
		dsNode.timestamp = new Date();
		dsNode.modified = true;
		n.id = 1;
		n.timestamp = new Date(dsNode.timestamp.getTime()+1);
		v.visit(n);
		assertEquals(1, v.conflicts.size());
	}
	public void testNodesConflictModifyDelete() {
		dsNode.id = 1;
		dsNode.modified = true;
		n.id = 1;
		n.delete(true);
		v.visit(n);
		assertEquals(1, v.conflicts.size());
	}
	public void testNodesMergeSamePosition() {
		n.id = 1; // new node comes from server
		dsNode.modified = true; // our node is modified
		dsNode.coor = new LatLon(n.coor.lat(), n.coor.lon());
		v.visit(n);
		v.fixReferences();
		assertEquals(0, v.conflicts.size());
		assertEquals(1, dsNode.id);
		assertFalse("updating a new node clear the modified state", dsNode.modified);
	}

	public void testNoConflictNewNodesMerged() {
		assertEquals(0, n.id);
		assertEquals(0, dsNode.id);
		v.visit(n);
		v.fixReferences();
		assertEquals(0,v.conflicts.size());
		assertTrue(ds.nodes.contains(n));
		assertEquals(2, ds.nodes.size());
	}

	/**
	 * Test that two new segments that have different from/to are not merged
	 */
	@Bug(101)
	public void testNewSegmentNotMerged() {
		v.visit(n);
		Segment s1 = new Segment(n, dsNode);
		v.visit(s1);
		Segment s2 = new Segment(dsNode, n);
		v.visit(s2);
		assertEquals(2, ds.segments.size());
	}
	
	public void testFixReferencesConflicts() {
		// make two nodes mergable
		dsNode.id = 1;
		n.id = 1;
		n.timestamp = new Date();
		// have an old segment with the old node
		Segment sold = new Segment(dsNode, dsNode);
		sold.id = 23;
		sold.modified = true;
		ds.segments.add(sold);
		// have a conflicting segment point to the new node
		Segment s = new Segment(n,DataSetTestCaseHelper.createNode(null));
		s.id = 23;
		s.modified = true;

		v.visit(n); // merge
		assertEquals(n.timestamp, dsNode.timestamp);
		v.visit(s);
		assertEquals(1, v.conflicts.size());
		v.fixReferences();
		assertSame(s.from, dsNode);
	}

	public void testNoConflictForSame() {
		dsNode.id = 1;
		dsNode.modified = true;
		n.cloneFrom(dsNode);
		v.visit(n);
		assertEquals(0, v.conflicts.size());
	}

	/**
	 * Merge of an old segment with a new one. This should
	 * be mergable (if the nodes matches).
	 */
	public void testMergeOldSegmentsWithNew() {
		Node[] n = createNodes(ds, 2);
		Segment ls1 = DataSetTestCaseHelper.createSegment(ds, n[0], n[1]);
		ls1.id = 3;

		Node newnode = new Node(new LatLon(n[1].coor.lat(), n[1].coor.lon()));
		Segment newls = new Segment(n[0], newnode);

		v.visit(newls);
		assertEquals("segment should have been merged.", 1, ds.segments.size());
	}

	/**
	 * Incomplete segments should always loose.
	 */
	public void testImportIncomplete() throws Exception {
		Segment s1 = DataSetTestCaseHelper.createSegment(ds, dsNode, dsNode);
		s1.id = 1;
		Segment s2 = new Segment(s1);
		s1.incomplete = true;
		s2.timestamp = new Date();
		v.visit(s2);
		assertTrue(s1.realEqual(s2, false));
	}
	/**
	 * Incomplete segments should extend existing ways.
	 */
	public void testImportIncompleteExtendWays() throws Exception {
		Segment s1 = DataSetTestCaseHelper.createSegment(ds, dsNode, dsNode);
		Way w = DataSetTestCaseHelper.createWay(ds, new Segment[]{s1});
		s1.id = 1;
		Segment s2 = new Segment(s1);
		s1.incomplete = true;
		v.visit(s2);
		v.fixReferences();
		assertEquals(1, w.segments.size());
		assertEquals(s2, w.segments.get(0));
		assertFalse(s2.incomplete);
	}


	/**
	 * Nodes beeing merged are equal but should be the same.
	 */
	@Bug(54)
	public void testEqualNotSame() {
		ds = new DataSet();
		// create a dataset with segment a-b
		Node n[] = createNodes(ds, 2);
		Segment ls1 = DataSetTestCaseHelper.createSegment(ds, n[0], n[1]);
		ls1.id = 1;

		// create an other dataset with segment a'-c (a' is equal, but not same to a)
		DataSet ds2 = new DataSet();
		Node n2[] = createNodes(ds2, 2);
		n2[0].coor = new LatLon(n[0].coor.lat(), n[0].coor.lon());
		n2[0].id = 0;
		n2[1].id = 42;

		Segment ls2 = DataSetTestCaseHelper.createSegment(ds, n2[0], n2[1]);
		v = new MergeVisitor(ds, null);
		for (OsmPrimitive osm : ds2.allPrimitives())
			osm.visit(v);
		v.fixReferences();

		assertSame(ls1.from, ls2.from);
	}


	public void testCloneWayNotIncomplete() {
		DataSet ds = new DataSet();
		Node[] n = createNodes(ds, 2);
		Segment s = DataSetTestCaseHelper.createSegment(ds, n[0], n[1]);
		Way w = DataSetTestCaseHelper.createWay(ds, s);
		MergeVisitor v = new MergeVisitor(ds, null);
		v.visit(n[0]);
		v.visit(n[1]);
		v.visit(s);
		v.visit(w);
		Way w2 = new Way(w);
		w2.timestamp = new Date();
		Segment s2 = new Segment(s);
		s2.incomplete = true;
		w2.segments.clear();
		w2.segments.add(s2);
		v.visit(w2);
		assertSame("Do not import incomplete segments when merging ways.", s, w.segments.iterator().next());
	}

	/**
	 * When merging an incomplete way over a dataset that contain already all
	 * necessary segments, the way must be completed.
	 */
	@Bug(117)
	public void testMergeIncompleteOnExistingDoesNotComplete() {
		// create a dataset with an segment (as base for the later incomplete way)
		DataSet ds = new DataSet();
		Node[] n = createNodes(ds, 2);
		Segment s = DataSetTestCaseHelper.createSegment(ds, n[0], n[1]);
		s.id = 23;
		// create an incomplete way which references the former segment
		Way w = new Way();
		Segment incompleteSegment = new Segment(s.id);
		w.segments.add(incompleteSegment);
		w.id = 42;
		// merge both
		MergeVisitor v = new MergeVisitor(ds, null);
		v.visit(w);
		v.fixReferences();
		
		assertTrue(ds.ways.contains(w));
		assertEquals(1, w.segments.size());
		assertFalse(w.segments.get(0).incomplete);
	}
	
	/**
	 * Deleted segments should raise an conflict when merged over changed segments. 
	 */
	public void testMergeDeletedOverChangedConflict() {
		DataSet ds = new DataSet();
		createSegment(ds, false, false, 23).modified = true;
		Segment s = createSegment(null, false, true, 23);
		s.timestamp = new Date(new Date().getTime()+1);
		
		MergeVisitor v = new MergeVisitor(ds, null);
		v.visit(s);
		v.fixReferences();
		
		assertEquals(1, v.conflicts.size());
	}
	
	public void testMergeIncompleteSegmentsAddToDataSet() throws Exception {
		DataSet ds = new DataSet();
		MergeVisitor v = new MergeVisitor(ds, null);
		v.visit(createSegment(null, true, false, 1));
		assertEquals(1, ds.segments.size());
    }

	/**
	 * The merger should auto-resolve items that have not changed but are marked as
	 * changed. In the case where an unmodified newer item is merged over an modified
	 * older, the modified-flag should be removed and the newer timestamp is used.
	 */
	public void testMergeModifiedWithOlderTimestampOverUnmodifiedNewerDoesNotConflict() throws Exception {
		DataSet ds = new DataSet();

		Node oldNode = createNodes(ds, 1)[0];
		oldNode.modified = true;
		oldNode.timestamp = new Date();
		
		Node newNode = new Node(oldNode);
		Date date = new Date(oldNode.timestamp.getTime()+10000);
		newNode.modified = false;
		newNode.timestamp = new Date(date.getTime());
		
		MergeVisitor v = new MergeVisitor(ds, null);
		v.visit(newNode);

		assertEquals(0, v.conflicts.size());
		assertEquals(date, ds.nodes.iterator().next().timestamp);
		assertFalse(ds.nodes.iterator().next().modified);
	}
	
	public void testMergeTwoIncompleteWaysMergesSecondAsWell() throws Exception {
		DataSet ds = new DataSet();
		Segment s1 = new Segment(23);
		Segment s2 = new Segment(42);
		
		MergeVisitor v = new MergeVisitor(ds, null);
		v.visit(s1);
		v.visit(s2);
		
		assertEquals(2, ds.segments.size());
		assertEquals(23, ds.segments.iterator().next().id);
	}
}
