// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
}
