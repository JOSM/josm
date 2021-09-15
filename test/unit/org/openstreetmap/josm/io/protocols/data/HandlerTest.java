// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.protocols.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;

/**
 * Unit tests of {@link Handler} class.
 */
@LayerEnvironment
class HandlerTest {
    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        Handler.install();
    }

    /**
     * Reads a base-64 image.
     * @throws IOException if any I/O error occurs
     */
    @Test
    void testBase64Image() throws IOException {
        // Red dot image, taken from https://en.wikipedia.org/wiki/Data_URI_scheme#HTML
        URLConnection connection = new Handler().openConnection(new URL("data:image/png;base64," +
                "iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4"+
                "//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="));
        connection.connect();
        assertNotNull(connection.getInputStream());
    }
}
