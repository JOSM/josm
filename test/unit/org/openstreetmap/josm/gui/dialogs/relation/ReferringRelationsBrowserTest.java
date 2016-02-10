// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * Unit tests of {@link ReferringRelationsBrowser} class.
 */
public class ReferringRelationsBrowserTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ReferringRelationsBrowser#ReferringRelationsBrowser}.
     */
    @Test
    public void testReferringRelationsBrowser() {
        DataSet ds = new DataSet();
        Relation r = new Relation();
        ds.addPrimitive(r);
        new ReferringRelationsBrowser(new OsmDataLayer(ds, "", null), new ReferringRelationsBrowserModel(r)).init();
    }
}
