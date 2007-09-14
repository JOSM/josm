// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.download;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;

public class BoundingBoxSelectionTest {

	private Bounds bounds;

	@Test public void osmurl2boundsDoesWorkWithAnyDomain() throws Exception {
		bounds = BoundingBoxSelection.osmurl2bounds("http://foobar?mlat=123&mlon=234&zoom=1");
		assertNotNull(bounds);
		
		bounds = BoundingBoxSelection.osmurl2bounds("http://www.openstreetmap.org?mlat=123&mlon=234&zoom=1");
		assertNotNull(bounds);
	}
}
