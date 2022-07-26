// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.OsmApiType;
import org.openstreetmap.josm.testutils.annotations.Users;

/**
 * Unit tests of {@link DownloadPrimitivesTask} class.
 */
@OsmApiType(OsmApiType.APIType.DEV)
@Timeout(20)
@Users
class DownloadPrimitivesTaskTest {
    /**
     * Test of {@link DownloadPrimitivesTask} class.
     */
    @Test
    void testDownloadPrimitivesTask() {
        DataSet ds = new DataSet();
        assertTrue(ds.allPrimitives().isEmpty());
        SimplePrimitiveId pid = new SimplePrimitiveId(1, OsmPrimitiveType.NODE);
        new DownloadPrimitivesTask(new OsmDataLayer(ds, "", null), Arrays.asList(pid), true).run();
        assertFalse(ds.allPrimitives().isEmpty());
        assertNotNull(ds.getPrimitiveById(pid));
    }
}
