// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxImageEntry;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

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

    /**
     * Unit test of methods {@link ImageEntry#equals} and {@link ImageEntry#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(ImageEntry.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withPrefabValues(GpxImageEntry.class, new GpxImageEntry(new File("foo")), new GpxImageEntry(new File("bar")))
            .verify();
    }
}
