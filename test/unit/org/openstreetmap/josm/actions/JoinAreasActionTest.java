// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.tools.Utils;

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
        try (InputStream is = TestUtils.getRegressionDataStream(10511, "10511_mini.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            Layer layer = new OsmDataLayer(ds, null, null);
            Main.getLayerManager().addLayer(layer);
            // FIXME enable this test after we fix the bug. Test disabled for now
            // try {
            //     new JoinAreasAction().join(ds.getWays());
            // } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.getLayerManager().removeLayer(layer);
            // }
        }
    }

    /**
     * Non-regression test for bug #11992.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    public void testTicket11992() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(11992, "shapes.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            assertEquals(10, ds.getWays().size());
            Layer layer = new OsmDataLayer(ds, null, null);
            Main.getLayerManager().addLayer(layer);
            for (String ref : new String[]{"A", "B", "C", "D", "E"}) {
                System.out.print("Joining ways " + ref);
                Collection<OsmPrimitive> found = SearchAction.searchAndReturn("type:way ref="+ref, SearchAction.SearchMode.replace);
                assertEquals(2, found.size());

                Main.main.menu.joinAreas.join(Utils.filteredCollection(found, Way.class));

                Collection<OsmPrimitive> found2 = SearchAction.searchAndReturn("type:way ref="+ref, SearchAction.SearchMode.replace);
                assertEquals(1, found2.size());
                System.out.println(" ==> OK");
            }
        }
    }
}
