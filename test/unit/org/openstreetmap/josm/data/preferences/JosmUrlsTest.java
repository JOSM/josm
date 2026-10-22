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

    /**
     * An API URL entered with a trailing slash must give the same web site URLs as one without, so that
     * "History (web)" and the other links to the server are built from the web site and not from the API path.
     */
    @Test
    void testApiUrlWithTrailingSlash() {
        Config.getPref().put("osm-server.url", "https://api06.dev.openstreetmap.org/api/");
        assertEquals("https://api06.dev.openstreetmap.org", Config.getUrls().getOSMWebsiteDependingOnSelectedApi());
        assertEquals("https://api06.dev.openstreetmap.org", Config.getUrls().getBaseBrowseUrl());
        assertEquals("https://api06.dev.openstreetmap.org/user", Config.getUrls().getBaseUserUrl());
    }

    /**
     * The default API URL is recognized even when it is entered with a trailing slash, so the links point to
     * the OSM web site rather than to the API host.
     */
    @Test
    void testDefaultApiUrlWithTrailingSlash() {
        Config.getPref().put("osm-server.url", Config.getUrls().getDefaultOsmApiUrl() + '/');
        assertEquals(Config.getUrls().getOSMWebsite(), Config.getUrls().getOSMWebsiteDependingOnSelectedApi());
    }
}
