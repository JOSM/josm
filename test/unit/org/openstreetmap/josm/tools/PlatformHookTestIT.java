// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.testutils.annotations.IntegrationTest;

/**
 * Integration tests of {@link PlatformHook} class.
 */
@HTTP
@HTTPS
@IntegrationTest
class PlatformHookTestIT {
    /**
     * Test that we always support the latest Ubuntu version.
     * @throws Exception in case of error
     */
    @Test
    void testLatestUbuntuVersion() throws Exception {
        String latestUbuntuVersion = Json.createReader(new StringReader(HttpClient.create(
                new URL("https://api.launchpad.net/devel/ubuntu/series")).connect().fetchContent()))
                .readObject().getJsonArray("entries").getJsonObject(0).getString("name");
        assertEquals(HttpURLConnection.HTTP_OK, HttpClient.create(
                new URL("https://josm.openstreetmap.de/apt/dists/" + latestUbuntuVersion + '/')).connect().getResponseCode(),
                latestUbuntuVersion);
    }
}
