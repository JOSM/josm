// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.Assert.assertNotNull;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link OsmOAuthAuthorizationClient} class.
 */
public class OsmOAuthAuthorizationClientTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().timeout(20000);

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

    /**
     * Unit test for correct cookie handling when logging in to the OSM website.
     *
     * https://josm.openstreetmap.de/ticket/12584
     * @throws Exception if any error occurs
     */
    @Test
    public void testCookieHandling() throws Exception {
        final OAuthParameters parameters = OAuthParameters.createDefault();
        final OsmOAuthAuthorizationClient client = new OsmOAuthAuthorizationClient(parameters);
        assertNotNull(client.fetchOsmWebsiteSessionId());

        // emulate Java Web Start behaviour
        // see https://docs.oracle.com/javase/tutorial/deployment/doingMoreWithRIA/accessingCookies.html
        final CookieManager cm = new CookieManager();
        cm.put(new URI(parameters.getOsmLoginUrl()),
                Collections.singletonMap("Cookie", Collections.singletonList("_osm_session=" + String.valueOf(Math.PI).substring(2))));
        CookieHandler.setDefault(cm);
        assertNotNull(client.fetchOsmWebsiteSessionId());
    }
}
