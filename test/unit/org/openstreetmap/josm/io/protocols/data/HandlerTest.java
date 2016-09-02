// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.protocols.data;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Unit tests of {@link Handler} class.
 */
public class HandlerTest {

    /**
     * Use the test rules to remove any layers and reset state.
     */
    @Rule
    public final JOSMTestRules rules = new JOSMTestRules();

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        Handler.install();
    }

    /**
     * Reads a base-64 image.
     * @throws IOException if any I/O error occurs
     */
    @Test
    public void testBase64Image() throws IOException {
        // Red dot image, taken from https://en.wikipedia.org/wiki/Data_URI_scheme#HTML
        URLConnection connection = new Handler().openConnection(new URL("data:image/png;base64," +
                "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4"+
                "//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="));
        connection.connect();
        assertNotNull(connection.getInputStream());
    }
}
