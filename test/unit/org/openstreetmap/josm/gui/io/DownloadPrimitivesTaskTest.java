// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link DownloadPrimitivesTask} class.
 */
class DownloadPrimitivesTaskTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().devAPI().timeout(20000);

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
