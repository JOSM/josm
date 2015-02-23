// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;

/**
 * Unit tests of {@link JoinAreasAction} class.
 */
public class JoinAreasActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Non-regression test for bug #10511.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket10511() throws IOException, IllegalDataException {
        try (InputStream is = new FileInputStream(TestUtils.getRegressionDataFile(10511, "10511_mini.osm"))) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            Main.map.mapView.addLayer(new OsmDataLayer(ds, null, null));
            // FIXME enable this test after we fix the bug. Test disabled for now
            // new JoinAreasAction().join(ds.getWays());
        }
    }
}
