// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.awt.Transparency;
import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;

/**
 * Unit tests of {@link ImageProvider} class.
 */
public class ImageProviderTest {

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/9984">#9984</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket9984() throws IOException {
        File file = new File(TestUtils.getRegressionDataFile(9984, "tile.png"));
        assertThat(ImageProvider.read(file, true, true).getTransparency(), is(Transparency.TRANSLUCENT));
        assertThat(ImageProvider.read(file, false, true).getTransparency(), is(Transparency.TRANSLUCENT));
        assertThat(ImageProvider.read(file, false, false).getTransparency(), is(Transparency.OPAQUE));
        assertThat(ImageProvider.read(file, true, false).getTransparency(), is(Transparency.OPAQUE));
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/10030">#10030</a>
     * @throws IOException if an error occurs during reading
     */
    @Test
    public void testTicket10030() throws IOException {
        File file = new File(TestUtils.getRegressionDataFile(10030, "tile.jpg"));
        ImageProvider.read(file, true, true);
    }
}
