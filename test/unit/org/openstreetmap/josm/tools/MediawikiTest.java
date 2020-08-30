// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests of {@link Mediawiki}.
 */
public class MediawikiTest {

    /**
     * Test of {@link Mediawiki#getImageUrl}
     */
    @Test
    public void testImageUrl() {
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/1/18/OpenJDK_logo.svg",
                Mediawiki.getImageUrl("https://upload.wikimedia.org/wikipedia/commons", "OpenJDK_logo.svg"));
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/1/18/OpenJDK_logo.svg",
                Mediawiki.getImageUrl("https://upload.wikimedia.org/wikipedia/commons/", "OpenJDK_logo.svg"));
    }
}
