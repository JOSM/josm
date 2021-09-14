// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.IntegrationTest;

/**
 * Integration tests of {@link ImageProvider} class.
 */
@IntegrationTest
@BasicPreferences
@HTTP
class ImageProviderTestIT {
    /**
     * Test fetching an image using {@code wiki://} protocol.
     */
    @Test
    void testWikiProtocol() {
        // https://commons.wikimedia.org/wiki/File:OpenJDK_logo.svg
        assertNotNull(ImageProvider.get("wiki://OpenJDK_logo.svg"));
    }
}
