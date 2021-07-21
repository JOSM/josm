// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.data.oauth.OAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.io.OsmTransferCanceledException;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.HTTP;

import com.github.tomakehurst.wiremock.WireMockServer;

/**
 * Unit tests of {@link OsmOAuthAuthorizationClient} class.
 */
@Timeout(20)
@BasicWiremock
// Needed for OAuthParameters
@BasicPreferences
@HTTP
class OsmOAuthAuthorizationClientTest {
    /**
     * HTTP mock.
     */
    @BasicWiremock
    WireMockServer wireMockServer;

    /**
     * Unit test of {@link OsmOAuthAuthorizationClient}.
     * @throws OsmOAuthAuthorizationException if OAuth authorization error occurs
     * @throws OsmTransferCanceledException  if OSM transfer error occurs
     */
    @Test
    void testOsmOAuthAuthorizationClient() throws OsmTransferCanceledException, OsmOAuthAuthorizationException {
        // request token
        wireMockServer.stubFor(get(urlEqualTo("/oauth/request_token"))
                .willReturn(aResponse().withStatus(200).withBody(String.join("&",
                        "oauth_token=entxUGuwRKV6KyVDF0OWScdGhbqXGMGmosXuiChR",
                        "oauth_token_secret=nsBD2Hr5lLGDUeNoh3SnLaGsUV1TiPYM4qUr7tPB"))));
        OsmOAuthAuthorizationClient client = new OsmOAuthAuthorizationClient(OAuthParameters.createDefault(
                wireMockServer.url("/api")));

        OAuthToken requestToken = client.getRequestToken(null);
        assertEquals("entxUGuwRKV6KyVDF0OWScdGhbqXGMGmosXuiChR", requestToken.getKey(), "requestToken.key");
        assertEquals("nsBD2Hr5lLGDUeNoh3SnLaGsUV1TiPYM4qUr7tPB", requestToken.getSecret(), "requestToken.secret");
        String url = client.getAuthoriseUrl(requestToken);
        assertEquals(wireMockServer.url("/oauth/authorize?oauth_token=entxUGuwRKV6KyVDF0OWScdGhbqXGMGmosXuiChR"), url, "url");

        // access token
        wireMockServer.stubFor(get(urlEqualTo("/oauth/access_token"))
                .willReturn(aResponse().withStatus(200).withBody(String.join("&",
                        "oauth_token=eGMGmosXuiChRntxUGuwRKV6KyVDF0OWScdGhbqX",
                        "oauth_token_secret=nsBUeNor7tPh3SHr5lLaGsGDUD2PYMV1TinL4qUB"))));

        OAuthToken accessToken = client.getAccessToken(null);
        assertEquals("eGMGmosXuiChRntxUGuwRKV6KyVDF0OWScdGhbqX", accessToken.getKey(), "accessToken.key");
        assertEquals("nsBUeNor7tPh3SHr5lLaGsGDUD2PYMV1TinL4qUB", accessToken.getSecret(), "accessToken.secret");
    }

    /**
     * Unit test for correct cookie handling when logging in to the OSM website.
     *
     * https://josm.openstreetmap.de/ticket/12584
     * @throws Exception if any error occurs
     */
    @Test
    void testCookieHandlingMock() throws Exception {
        wireMockServer.stubFor(get(urlEqualTo("/login?cookie_test=true"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Set-Cookie", "_osm_session=7fe8e2ea36c6b803cb902301b28e0a; path=/; HttpOnly; SameSite=Lax")
                .withBody("<input type=\"hidden\" " +
                        "name=\"authenticity_token\" " +
                        "value=\"fzp6CWJhp6Vns09re3s2Tw==\" />")));
        final OAuthParameters parameters = OAuthParameters.createDefault(wireMockServer.url("/api"));
        final OsmOAuthAuthorizationClient client = new OsmOAuthAuthorizationClient(parameters);
        final OsmOAuthAuthorizationClient.SessionId sessionId = client.fetchOsmWebsiteSessionId();
        assertNotNull(sessionId);
        assertEquals("7fe8e2ea36c6b803cb902301b28e0a", sessionId.id, "sessionId.id");
        assertEquals("fzp6CWJhp6Vns09re3s2Tw==", sessionId.token, "sessionId.token");
        assertNull(sessionId.userName, "sessionId.userName");
    }

    /**
     * Unit test for correct cookie handling when logging in to the OSM website.
     *
     * https://josm.openstreetmap.de/ticket/12584
     * @throws Exception if any error occurs
     */
    @Test
    void testCookieHandlingCookieManager() throws Exception {
        // emulate Java Web Start behaviour
        // see https://docs.oracle.com/javase/tutorial/deployment/doingMoreWithRIA/accessingCookies.html
        final OAuthParameters parameters = OAuthParameters.createDefault();
        final OsmOAuthAuthorizationClient client = new OsmOAuthAuthorizationClient(parameters);
        final CookieManager cm = new CookieManager();
        cm.put(new URI(parameters.getOsmLoginUrl()),
                Collections.singletonMap("Cookie", Collections.singletonList("_osm_session=" + String.valueOf(Math.PI).substring(2))));
        CookieHandler.setDefault(cm);
        assertNotNull(client.fetchOsmWebsiteSessionId());
    }
}
