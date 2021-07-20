// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;

/**
 * Tests the {@link GettingStarted} class.
 */
@BasicPreferences
@HTTP
class GettingStartedTest {

    /**
     * Tests that image links are replaced.
     *
     * @throws IOException if any I/O error occurs
     */
    @Test
    @Disabled("see #15240, inactive for /browser/trunk/nodist/images/download.png")
    void testImageReplacement() throws IOException {
        final String motd = new GettingStarted.MotdContent().updateIfRequiredString();
        // assuming that the MOTD contains one image included, fixImageLinks changes the HTML string
        assertNotEquals(GettingStarted.fixImageLinks(motd), motd);
    }

    /**
     * Tests that image links are replaced.
     */
    @Test
    void testImageReplacementStatic() {
        final String html = "the download button <img src=\"/browser/trunk/resources/images/download.svg?format=raw\" " +
                "alt=\"source:trunk/resources/images/download.svg\" title=\"source:trunk/resources/images/download.svg\" />.";
        assertNotEquals(GettingStarted.fixImageLinks(html), html);
    }
}
