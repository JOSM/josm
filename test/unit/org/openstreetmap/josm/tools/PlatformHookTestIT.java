// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.json.Json;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Integration tests of {@link PlatformHook} class.
 */
public class PlatformHookTestIT {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test that we always support the latest Ubuntu version.
     * @throws Exception in case of error
     */
    @Test
    public void testLatestUbuntuVersion() throws Exception {
        String latestUbuntuVersion = Json.createReader(new StringReader(HttpClient.create(
                new URL("https://api.launchpad.net/devel/ubuntu/series")).connect().fetchContent()))
                .readObject().getJsonArray("entries").getJsonObject(0).getString("name");
        assertEquals(latestUbuntuVersion, HttpURLConnection.HTTP_OK, HttpClient.create(
                new URL("https://josm.openstreetmap.de/apt/dists/" + latestUbuntuVersion + '/')).connect().getResponseCode());
    }
}
