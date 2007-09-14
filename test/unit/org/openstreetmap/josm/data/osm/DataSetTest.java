// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.testframework.MotherObject;

public class DataSetTest extends MotherObject {

	private final class TestSelectionChangeListener implements SelectionChangedListener {
	    public Collection<? extends OsmPrimitive> called;
		public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
	    	called = newSelection;
	    }
    }

	private DataSet ds;
	private Node node1;
	private Node node2;
	private Node node3;
	private Segment segment;
	private Way way;

	@Override protected void setUp() throws Exception {
		super.setUp();
		ds = createDataSet();
		Iterator<Node> it = ds.nodes.iterator();
		node1 = it.next();
		node2 = it.next();
		node3 = it.next();
		segment = ds.segments.iterator().next();
		way = ds.ways.iterator().next();
	}

	public void testAllPrimitives() {
		Collection<OsmPrimitive> all = ds.allPrimitives();
		assertContainsSame(all, node1, node2, node3, segment, way);
	}

	public void testAllNonDeletedPrimitives() {
		assertEquals(5, ds.allNonDeletedPrimitives().size());
		node1.deleted = true;
		assertEquals(4, ds.allNonDeletedPrimitives().size());
	}

	public void testGetSelected() {
		node1.selected = true;
		segment.selected = true;
		Collection<OsmPrimitive> sel = ds.getSelected();
		assertContainsSame(sel, node1, segment);
	}

	public void testSetSelected() {
		Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
		sel.add(node1);
		sel.add(way);
		ds.setSelected(sel);
		assertTrue(node1.selected);
		assertFalse(node2.selected);
		assertTrue(way.selected);
	}

	public void testSetSelectedOsmPrimitive() {
		ds.setSelected(node3);
		assertTrue(node3.selected);
		assertFalse(node2.selected);

		ds.setSelected();
		assertFalse(node3.selected || node2.selected);
		
		ds.setSelected(node1, way);
		assertTrue(node1.selected && way.selected);
		assertFalse(node3.selected);
		
		ds.setSelected((OsmPrimitive)null);
		assertFalse(node1.selected || node2.selected || node3.selected || way.selected);
	}

	public void testFireSelectionChanged() {
		TestSelectionChangeListener l = new TestSelectionChangeListener();
		DataSet.listeners.add(l);
		ds.setSelected(segment);
		assertNotNull(l.called);
		assertEquals(1, l.called.size());
		assertSame(segment, l.called.iterator().next());
		DataSet.listeners.remove(l);
	}
}
