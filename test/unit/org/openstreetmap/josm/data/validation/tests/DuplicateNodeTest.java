// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;

/**
 * JUnit Test of "Duplicate node" validation test. 
 */
public class DuplicateNodeTest {

    /**
     * Setup test by initializing JOSM preferences and projection. 
     */
    @BeforeClass
    public static void setUp() {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
        Main.initApplicationPreferences();
    }

    /**
     * Test of "Duplicate node" validation test.
     */
    @Test
    public void test() {
        DataSet ds = new DataSet();

        Node a = new Node(new LatLon(10.0, 5.0));
        Node b = new Node(new LatLon(10.0, 5.0));
        ds.addPrimitive(a);
        ds.addPrimitive(b);

        DuplicateNode test = new DuplicateNode();
        test.startTest(NullProgressMonitor.INSTANCE);
        test.visit(ds.allPrimitives());
        test.endTest();
        
        assertEquals(1, test.getErrors().size());
    }
}
