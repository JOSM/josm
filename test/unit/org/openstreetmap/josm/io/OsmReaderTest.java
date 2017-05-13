// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14754">Bug #14754</a>.
     * @throws Exception if any error occurs
     */
    @Test
    public void testTicket14754() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(14754, "malformed_for_14754.osm")) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            fail("should throw exception");
        } catch (IllegalDataException e) {
            assertEquals("Illegal value for attributes 'lat', 'lon' on node with ID 1425146006." +
                    " Got '550.3311950157', '10.49428298298'." +
                    " (at line 5, column 179). 578 bytes have been read", e.getMessage());
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14788">Bug #14788</a>.
     * @throws Exception if any error occurs
     */
    @Test
    public void testTicket14788() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(14788, "remove_sign_test_4.osm")) {
            OsmReader.parseDataSet(in, NullProgressMonitor.INSTANCE);
            fail("should throw exception");
        } catch (IllegalDataException e) {
            assertEquals("Illegal value for attributes 'lat', 'lon' on node with ID 978." +
                    " Got 'nan', 'nan'." +
                    " (at line 4, column 151). 336 bytes have been read", e.getMessage());
        }
    }
}
