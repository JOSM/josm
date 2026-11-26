// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.Logging;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests for class {@link OAuthParameters}.
 */
@BasicPreferences
class OAuthParametersTest {

    private static final String THIRD_PARTY = "https://nonprod-mapops.example.org/api";
    private static final String THIRD_PARTY_HOST = "nonprod-mapops.example.org";
    private static final String OHM = "https://api.openhistoricalmap.org/api";
    private static final String REDIRECT_URI = "http://127.0.0.1:8111/oauth_authorization";

    @AfterEach
    void tearDown() {
        NetworkManager.setOnline(OnlineResource.ALL);
        // the preferences are static state shared with the other tests of this class
        Config.getPref().put("osm-server.url", null);
        for (String key : new String[] {THIRD_PARTY_HOST, THIRD_PARTY, "api.openhistoricalmap.org"}) {
            Config.getPref().put("oauth.access-token.parameters.OAuth20." + key, null);
            Config.getPref().put("oauth.settings.use-default." + key, null);
        }
        Config.getPref().put("oauth.settings.use-default", null);
    }

    private static OAuth20Parameters enteredParameters(String apiUrl, String clientId) {
        return new OAuth20Parameters(clientId, null, apiUrl, apiUrl, REDIRECT_URI);
    }

    private static String clientIdOf(String apiUrl) {
        return OAuthParameters.createFromApiUrl(apiUrl, OAuthVersion.OAuth20).getClientId();
    }

    /**
     * {@link OAuthParameters#createFromApiUrl} answers for the server it is asked about, whichever server is
     * configured in the preferences, and finds the parameters the user entered in the advanced OAuth settings.
     * <p>
     * Non-regression test for #24925: the URL was reduced to the host before the fallback to
     * {@link OAuthParameters#createDefault}, and a host is never a valid URL, so the defaults of the configured
     * API were returned instead of those of the server which was asked about.
     */
    @Test
    void testCreateFromApiUrlUsesTheRequestedServer() {
        // the RFC 8414 lookup must not make a network request
        NetworkManager.setOffline(OnlineResource.ALL);
        final String thirdParty = "https://nonprod-mapops.example.org/api";
        Config.getPref().put("osm-server.url", thirdParty);

        // a server JOSM has a client id for is answered with that client id, not with the configured server's
        assertEquals("Hl5yIhFS-Egj6aY7A35ouLOuZl0EHjj8JJQQ46IO96E",
                OAuthParameters.createFromApiUrl("https://api.openhistoricalmap.org/api", OAuthVersion.OAuth20).getClientId());
        // a server JOSM knows nothing about has no client id until the user enters one
        assertEquals("", OAuthParameters.createFromApiUrl(thirdParty, OAuthVersion.OAuth20).getClientId());

        // once the parameters are remembered - as the authentication preferences do - they are found both by the
        // API URL and by the host, which is what a connection has at hand
        OAuthParameters.setUseDefault(thirdParty, false);
        new OAuth20Parameters("entered-client-id", null, thirdParty, thirdParty, "http://127.0.0.1:8111/oauth_authorization")
                .rememberPreferences();
        assertEquals("entered-client-id",
                OAuthParameters.createFromApiUrl(thirdParty, OAuthVersion.OAuth20).getClientId());
        assertEquals("https://api.openhistoricalmap.org/api",
                OAuthParameters.createFromApiUrl("https://api.openhistoricalmap.org/api", OAuthVersion.OAuth20).getApiUrl());
    }

    /**
     * The parameters the user entered are stored under the host of the API, which is where the access token of the
     * server is stored too, and replace those stored under the API URL by earlier versions of JOSM.
     */
    @Test
    void testParametersAreStoredUnderTheHost() {
        NetworkManager.setOffline(OnlineResource.ALL);
        OAuthParameters.setUseDefault(THIRD_PARTY, false);
        // as stored by earlier versions of JOSM, which are still found
        Config.getPref().put("oauth.access-token.parameters.OAuth20." + THIRD_PARTY,
                enteredParameters(THIRD_PARTY, "old-client-id").toPreferencesString());
        assertEquals("old-client-id", clientIdOf(THIRD_PARTY));

        enteredParameters(THIRD_PARTY, "new-client-id").rememberPreferences();
        assertNotNull(Config.getPref().get("oauth.access-token.parameters.OAuth20." + THIRD_PARTY_HOST, null));
        assertNull(Config.getPref().get("oauth.access-token.parameters.OAuth20." + THIRD_PARTY, null),
                "the parameters stored by earlier versions of JOSM are left behind");
        assertEquals("new-client-id", clientIdOf(THIRD_PARTY));
        assertEquals("new-client-id", clientIdOf(THIRD_PARTY_HOST));
    }

    /**
     * "Use default settings" is a setting of each server: entering the client id of one server does not take the
     * client ids JOSM has for the other servers away, and the entered parameters are not used once the server is set
     * to use the default settings again, but stay for when they are needed again.
     */
    @Test
    void testUseDefaultIsASettingOfEachServer() {
        NetworkManager.setOffline(OnlineResource.ALL);
        assertTrue(OAuthParameters.isUseDefault(THIRD_PARTY));
        OAuthParameters.setUseDefault(THIRD_PARTY, false);
        enteredParameters(THIRD_PARTY, "entered-client-id").rememberPreferences();
        enteredParameters(OHM, "not-used").rememberPreferences();

        assertFalse(OAuthParameters.isUseDefault(THIRD_PARTY));
        assertFalse(OAuthParameters.isUseDefault(THIRD_PARTY_HOST));
        assertTrue(OAuthParameters.isUseDefault(OHM));
        assertEquals("entered-client-id", clientIdOf(THIRD_PARTY));
        assertEquals("Hl5yIhFS-Egj6aY7A35ouLOuZl0EHjj8JJQQ46IO96E", clientIdOf(OHM));

        OAuthParameters.setUseDefault(THIRD_PARTY, true);
        assertEquals("", clientIdOf(THIRD_PARTY));
        OAuthParameters.setUseDefault(THIRD_PARTY, false);
        assertEquals("entered-client-id", clientIdOf(THIRD_PARTY));
    }

    /**
     * Until a server has a "use default settings" of its own, the setting earlier versions of JOSM had for all
     * servers applies, so that the parameters entered with them are still used.
     */
    @Test
    void testUseDefaultFallsBackToTheSettingForAllServers() {
        Config.getPref().putBoolean("oauth.settings.use-default", false);
        assertFalse(OAuthParameters.isUseDefault(THIRD_PARTY));
        OAuthParameters.setUseDefault(THIRD_PARTY, true);
        assertTrue(OAuthParameters.isUseDefault(THIRD_PARTY));
        assertFalse(OAuthParameters.isUseDefault(OHM));
    }

    /**
     * The OAuth settings of a server are stored under its host, whether it is given as the API URL or as the host,
     * and a missing API URL does not throw.
     */
    @Test
    void testGetHost() {
        assertEquals(THIRD_PARTY_HOST, OAuthParameters.getHost(THIRD_PARTY));
        assertEquals(THIRD_PARTY_HOST, OAuthParameters.getHost(THIRD_PARTY_HOST));
        assertEquals("localhost", OAuthParameters.getHost("http://localhost:3000/api"));
        assertNull(OAuthParameters.getHost(null));
        assertTrue(OAuthParameters.isUseDefault(null));
        OAuthParameters.setUseDefault(null, false);
        assertTrue(OAuthParameters.isUseDefault(null));
        // parameters without a server are not stored under a key without a host
        new OAuth20Parameters("a-client-id", null, "", "", "", REDIRECT_URI).rememberPreferences();
        assertNull(Config.getPref().get("oauth.access-token.parameters.OAuth20.", null));
    }

    /**
     * When the server does not publish its OAuth metadata, the endpoints are those of openstreetmap-website, on the
     * website rather than below the API URL: {@code <api url>/authorize} is wrong for every OSM API server.
     */
    @Test
    void testEndpointsWithoutDiscovery() {
        NetworkManager.setOffline(OnlineResource.ALL);
        final IOAuthParameters thirdParty = OAuthParameters.createDefault(THIRD_PARTY, OAuthVersion.OAuth20);
        assertEquals("https://nonprod-mapops.example.org/oauth2/authorize", thirdParty.getAuthorizationUrl());
        assertEquals("https://nonprod-mapops.example.org/oauth2/token", thirdParty.getAccessTokenUrl());
        assertEquals(THIRD_PARTY, thirdParty.getApiUrl());
        // the default API is served from a host of its own
        final IOAuthParameters osm = OAuthParameters.createDefault("https://api.openstreetmap.org/api", OAuthVersion.OAuth20);
        assertEquals("https://www.openstreetmap.org/oauth2/authorize", osm.getAuthorizationUrl());
        assertEquals("https://www.openstreetmap.org/oauth2/token", osm.getAccessTokenUrl());
    }

    /**
     * The OAuth metadata (RFC 8414) of a server is looked up on its port, such as that of a development server on
     * http://localhost:3000. Non-regression test: the port was dropped, so the metadata was not found.
     */
    @Test
    void testDiscoveryUsesThePort() {
        final WireMockServer server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        try {
            final String website = "http://localhost:" + server.port();
            server.stubFor(WireMock.get(WireMock.urlEqualTo("/.well-known/oauth-authorization-server")).willReturn(WireMock.okJson(
                    "{\"authorization_endpoint\": \"" + website + "/discovered/authorize\","
                    + " \"token_endpoint\": \"" + website + "/discovered/token\"}")));
            final IOAuthParameters parameters = OAuthParameters.createDefault(website + "/api", OAuthVersion.OAuth20);
            assertEquals(website + "/discovered/authorize", parameters.getAuthorizationUrl());
            assertEquals(website + "/discovered/token", parameters.getAccessTokenUrl());
        } finally {
            server.stop();
        }
    }

    /**
     * Unit test of method {@link OAuthParameters#createDefault}.
     */
    @Test
    @SuppressFBWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    void testCreateDefault() {
        IOAuthParameters def = OAuthParameters.createDefault();
        assertNotNull(def);
        assertEquals(def, OAuthParameters.createDefault(Config.getUrls().getDefaultOsmApiUrl(), OAuthVersion.OAuth20));
        IOAuthParameters dev = OAuthParameters.createDefault("https://api06.dev.openstreetmap.org/api", OAuthVersion.OAuth20);
        assertNotNull(dev);
        assertNotEquals(def, dev);
        Logging.setLogLevel(Logging.LEVEL_TRACE); // enable trace for line coverage
        assertEquals(def, OAuthParameters.createDefault("wrong_url", OAuthVersion.OAuth20));
    }

    /**
     * Unit test of methods {@link OAuthParameters#equals} and {@link OAuthParameters#hashCode}.
     */
    @Test
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(OAuth20Parameters.class).usingGetClass().verify();
    }
}
