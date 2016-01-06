// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.IllegalDataException;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link ImageEntry} class.
 */
public class ImageEntryTest {

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/12255">#12255</a>.
     * @throws IllegalDataException if an error was found while parsing the data from the source
     * @throws IOException if any I/O error occurs
     * @throws SAXException if any XML error occurs
     */
    @Test
    public void testTicket12255() throws IllegalDataException, IOException, SAXException {
        ImageEntry e = new ImageEntry(new File(TestUtils.getRegressionDataFile(12255, "G0016941.JPG")));
        e.extractExif();
        assertNotNull(e.getExifTime());
    }
}
