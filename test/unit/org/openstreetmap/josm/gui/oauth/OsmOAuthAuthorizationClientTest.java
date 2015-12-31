// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.Assert.assertNotNull;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.io.OsmTransferCanceledException;

/**
 * Unit tests of {@link OsmOAuthAuthorizationClient} class.
 */
public class OsmOAuthAuthorizationClientTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link OsmOAuthAuthorizationClient}.
     * @throws OsmOAuthAuthorizationException if OAuth authorization error occurs
     * @throws OsmTransferCanceledException  if OSM transfer error occurs
     * @throws MalformedURLException in case of invalid URL
     */
    @Test
    public void testOsmOAuthAuthorizationClient() throws OsmTransferCanceledException, OsmOAuthAuthorizationException, MalformedURLException {
        OsmOAuthAuthorizationClient client = new OsmOAuthAuthorizationClient(OAuthParameters.createDefault());
        OAuthToken requestToken = client.getRequestToken(null);
        assertNotNull(requestToken);
        String url = client.getAuthoriseUrl(requestToken);
        assertNotNull(url);
        System.out.println(new URL(url));
        //OAuthToken accessToken = client.getAccessToken(null);
        //assertNotNull(accessToken);
    }
}
