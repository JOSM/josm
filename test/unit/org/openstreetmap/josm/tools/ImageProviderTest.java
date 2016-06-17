// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;

/**
 * Unit tests of {@link ImageProvider} class.
 */
public class ImageProviderTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/9984">#9984</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket9984() throws IOException {
        File file = new File(TestUtils.getRegressionDataFile(9984, "tile.png"));
        assertEquals(Transparency.TRANSLUCENT, ImageProvider.read(file, true, true).getTransparency());
        assertEquals(Transparency.TRANSLUCENT, ImageProvider.read(file, false, true).getTransparency());
        assertEquals(Transparency.OPAQUE, ImageProvider.read(file, false, false).getTransparency());
        assertEquals(Transparency.OPAQUE, ImageProvider.read(file, true, false).getTransparency());
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/10030">#10030</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket10030() throws IOException {
        File file = new File(TestUtils.getRegressionDataFile(10030, "tile.jpg"));
        BufferedImage img = ImageProvider.read(file, true, true);
        assertNotNull(img);
    }

    /**
     * Test fetching an image using {@code wiki://} protocol.
     */
    @Test
    public void testWikiProtocol() {
        // https://commons.wikimedia.org/wiki/File:OpenJDK_logo.svg
        assertNotNull(ImageProvider.get("wiki://OpenJDK_logo.svg"));
    }

    /**
     * Test fetching an image using {@code data:} URL.
     */
    @Test
    public void testDataUrl() {
        // Red dot image, taken from https://en.wikipedia.org/wiki/Data_URI_scheme#HTML
        assertNotNull(ImageProvider.get("data:image/png;base64," +
                "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4"+
                "//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="));
    }
}
