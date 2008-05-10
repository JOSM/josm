// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.lang.reflect.Field;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testframework.MotherObject;

public class AddVisitorTest extends MotherObject {

	private AddVisitor v;
	private DataSet ds;

	@Override protected void setUp() throws Exception {
		super.setUp();
		ds = new DataSet();
		v = new AddVisitor(ds);
	}

	public void testAddVisitorDataSet() throws Exception {
		AddVisitor v = new AddVisitor(ds);
		Field vDs = AddVisitor.class.getDeclaredField("ds");
		vDs.setAccessible(true);
		assertSame(ds, vDs.get(v));
	}
	
	public void testVisitNode() {
		createNode(23).visit(v);
		assertEquals(1, ds.nodes.size());
		assertEquals(23, ds.nodes.iterator().next().id);
	}

	public void testVisitSegment() {
		createSegment().visit(v);
		assertEquals(1, ds.segments.size());
	}

	public void testVisitWay() {
		createWay().visit(v);
		assertEquals(1, ds.ways.size());
	}

}
