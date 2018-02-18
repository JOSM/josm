// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link DownloadPrimitivesTask} class.
 */
public class DownloadPrimitivesTaskTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().devAPI().timeout(20000);

    /**
     * Test of {@link DownloadPrimitivesTask} class.
     */
    @Test
    public void testDownloadPrimitivesTask() {
        DataSet ds = new DataSet();
        assertTrue(ds.allPrimitives().isEmpty());
        SimplePrimitiveId pid = new SimplePrimitiveId(1, OsmPrimitiveType.NODE);
        new DownloadPrimitivesTask(new OsmDataLayer(ds, "", null), Arrays.asList(pid), true).run();
        assertFalse(ds.allPrimitives().isEmpty());
        assertNotNull(ds.getPrimitiveById(pid));
    }
}
