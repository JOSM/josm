// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;

/**
 * Unit tests of {@link Mediawiki}.
 */
class MediawikiTest {

    /**
     * Test of {@link Mediawiki#getImageUrl}
     */
    @Test
    void testImageUrl() {
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/1/18/OpenJDK_logo.svg",
                Mediawiki.getImageUrl("https://upload.wikimedia.org/wikipedia/commons", "OpenJDK_logo.svg"));
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/1/18/OpenJDK_logo.svg",
                Mediawiki.getImageUrl("https://upload.wikimedia.org/wikipedia/commons/", "OpenJDK_logo.svg"));
    }

    /**
     * Test of {@link Mediawiki#getGeoImagesUrl}
     * @throws Exception never
     */
    @Test
    void testGeoImagesUrl() throws Exception {
        // See https://josm.openstreetmap.de/ticket/21126
        // Checks that URL can be converted to URI, needed for HTTP/2
        // CHECKSTYLE.OFF: LineLength
        assertEquals(new URI("https://commons.wikimedia.org/w/api.php?format=xml&action=query&list=geosearch&gsnamespace=6&gslimit=500&gsprop=type%7Cname&gsbbox=48.8623665%7C2.3913497%7C48.8600879%7C2.3967605"),
                new URL(Mediawiki.getGeoImagesUrl("https://commons.wikimedia.org/w/api.php",
                        new Bounds(48.8600879, 2.3913497, 48.8623665, 2.3967605))).toURI());
        // CHECKSTYLE.ON: LineLength
    }
}
