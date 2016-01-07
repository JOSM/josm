// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.GpxReader;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link GpxDrawHelper} class.
 */
public class GpxDrawHelperTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(false);
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12312">#12312</a>.
     * @throws IOException if any I/O error occurs
     * @throws FileNotFoundException if the data file is not found
     * @throws SAXException if any SAX error occurs
     */
    @Test
    public void testTicket12312() throws FileNotFoundException, IOException, SAXException {
        try (InputStream in = new FileInputStream((TestUtils.getRegressionDataFile(12312, "single_trackpoint.gpx")))) {
            GpxReader reader = new GpxReader(in);
            reader.parse(false);
            GpxDrawHelper gdh = new GpxDrawHelper(reader.getGpxData());
            Main.pref.put("draw.rawgps.colors.dynamic.layer 12312", true);
            Main.pref.putInteger("draw.rawgps.colors.layer 12312", 1); // ColorMode.VELOCITY
            gdh.readPreferences("12312");
            gdh.calculateColors();
        }
    }
}
