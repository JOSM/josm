// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;

/**
 * Unit tests of {@link OsmReader} class.
 */
public class OsmReaderTest {

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14199">Bug #14199</a>.
     * @throws Exception if any error occurs
     */
    @Test
    public void testTicket14199() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(14199, "emptytag.osm")) {
            Way w = OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE).getWays().iterator().next();
            assertEquals(1, w.getKeys().size());
            assertNull(w.get("  "));
            assertTrue(w.isModified());
        }
    }
}
