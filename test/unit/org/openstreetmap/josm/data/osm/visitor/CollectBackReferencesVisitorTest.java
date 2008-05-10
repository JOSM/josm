// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.lang.reflect.Field;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testframework.MotherObject;

public class CollectBackReferencesVisitorTest extends MotherObject {

	private CollectBackReferencesVisitor v;
	private DataSet ds;

	@Override protected void setUp() throws Exception {
		super.setUp();
		ds = createDataSet();
		v = new CollectBackReferencesVisitor(ds);
	}

	public void testCollectBackReferencesVisitor() throws Exception {
		DataSet dataSet = new DataSet();
		CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(dataSet);
		Field f = v.getClass().getDeclaredField("ds");
		f.setAccessible(true);
		assertSame(dataSet, f.get(v));
	}

	public void testVisitNode() {
		ds.nodes.iterator().next().visit(v);
		assertContainsSame(v.data, ds.segments.iterator().next(), ds.ways.iterator().next());
	}

	public void testVisitSegment() {
		ds.segments.iterator().next().visit(v);
		assertContainsSame(v.data, ds.ways.iterator().next());
	}

	public void testVisitWay() {
		ds.ways.iterator().next().visit(v);
		assertEquals(0, v.data.size());
	}

}
