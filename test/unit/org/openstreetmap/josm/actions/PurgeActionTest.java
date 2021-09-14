// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for class {@link PurgeAction}.
 */
@Main
@Projection
class PurgeActionTest {
    /**
     * Non-regression test for ticket #12038.
     * @throws IOException if any I/O error occurs
     * @throws IllegalDataException if OSM parsing fails
     */
    @Test
    void testCopyStringWayRelation() throws IOException, IllegalDataException {
        try (InputStream is = TestUtils.getRegressionDataStream(12038, "data.osm")) {
            DataSet ds = OsmReader.parseDataSet(is, null);
            OsmDataLayer layer = new OsmDataLayer(ds, null, null);
            MainApplication.getLayerManager().addLayer(layer);
            for (Way w : ds.getWays()) {
                if (w.getId() == 222191929L) {
                    ds.addSelected(w);
                }
            }
            new PurgeAction().actionPerformed(null);
            for (Way w : ds.getWays()) {
                if (w.getId() == 222191929L) {
                    assertTrue(w.isIncomplete());
                    assertEquals(0, w.getNodesCount());
                }
            }
        }
    }
}

