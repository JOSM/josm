// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;

/**
 * Unit tests of {@link ImageEntry} class.
 */
public class ImageEntryTest {

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12255">#12255</a>.
     */
    @Test
    public void testTicket12255() {
        ImageEntry e = new ImageEntry(new File(TestUtils.getRegressionDataFile(12255, "G0016941.JPG")));
        e.extractExif();
        assertNotNull(e.getExifTime());
    }
}
