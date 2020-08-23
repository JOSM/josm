// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertNotEquals;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Tests the {@link GettingStarted} class.
 */
public class GettingStartedTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void init() {
        JOSMFixture.createFunctionalTestFixture().init();
    }

    /**
     * Tests that image links are replaced.
     *
     * @throws IOException if any I/O error occurs
     */
    @Test
    @Ignore("see #15240, inactive for /browser/trunk/nodist/images/download.png")
    public void testImageReplacement() throws IOException {
        final String motd = new GettingStarted.MotdContent().updateIfRequiredString();
        // assuming that the MOTD contains one image included, fixImageLinks changes the HTML string
        assertNotEquals(GettingStarted.fixImageLinks(motd), motd);
    }

    /**
     * Tests that image links are replaced.
     */
    @Test
    public void testImageReplacementStatic() {
        final String html = "the download button <img src=\"/browser/trunk/resources/images/download.svg?format=raw\" " +
                "alt=\"source:trunk/resources/images/download.svg\" title=\"source:trunk/resources/images/download.svg\" />.";
        assertNotEquals(GettingStarted.fixImageLinks(html), html);
    }
}
