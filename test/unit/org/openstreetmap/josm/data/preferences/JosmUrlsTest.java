// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.OsmApi;

/**
 * Unit tests of {@link JosmUrls} class.
 */
@OsmApi(OsmApi.APIType.DEV)
class JosmUrlsTest {
    /**
     * Unit test of {@link JosmUrls#getBaseUserUrl}.
     */
    @Test
    void testGetBaseUserUrl() {
        assertEquals("https://api06.dev.openstreetmap.org/user", Config.getUrls().getBaseUserUrl());
    }
}
