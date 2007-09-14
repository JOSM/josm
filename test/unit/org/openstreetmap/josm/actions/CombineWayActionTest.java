// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openstreetmap.josm.testframework.MotherObject.createSegment;
import static org.openstreetmap.josm.testframework.MotherObject.createWay;

import javax.swing.Action;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testframework.MainMock;
import org.openstreetmap.josm.testframework.MotherObject;


public class CombineWayActionTest extends MainMock {

	private Action action = Main.main.menu.combineWay;
	private Way way1;
	private Way way2;
	private Segment segment1;
	private Segment segment2;

	@Before public void createTwoWays() {
		Main.ds = new DataSet();
		MotherObject.dataSet = Main.ds;
		segment1 = createSegment();
		segment2 = createSegment();
		way1 = createWay(segment1);
		way2 = createWay(segment2);
		Main.ds.setSelected(way1, way2);
	}
	
	@Test public void noSelectionBreaks() throws Exception {
		Main.ds.setSelected();
		action.actionPerformed(null);
		assertPopup();
	}
	
	@Test public void oneSelectedWayBreaks() throws Exception {
		Main.ds.setSelected(way1);
		action.actionPerformed(null);
		assertPopup();
	}
	
	@Test public void segmentsAreMergedInNewWay() throws Exception {
	    action.actionPerformed(null);
	    Way w = way1.deleted ? way2 : way1;
	    assertFalse(w.deleted);
	    assertEquals(2, w.segments.size());
    }
	
	@Test public void nonConflictingPropertiesAreMerged() throws Exception {
	    way1.put("foo", "bar");
	    way2.put("baz", "imi");
	    
	    action.actionPerformed(null);
	    Way w = way1.deleted ? way2 : way1;

	    assertEquals(2, w.keys.size());
	    assertEquals("bar", w.get("foo"));
	    assertEquals("imi", w.get("baz"));
    }
	
	@Test public void conflictingPropertiesOpenResolveDialog() throws Exception {
	    way1.put("foo", "bar");
	    way2.put("foo", "baz");
	    way2.put("imi", "ada");
	    
	    action.actionPerformed(null);
	    assertPopup();
	    Way w = way1.deleted ? way2 : way1;

	    assertEquals(2, w.keys.size());
	    assertTrue(w.get("foo").equals("bar") || w.get("foo").equals("bar"));
	    assertEquals("ada", w.get("imi"));
	}
}
