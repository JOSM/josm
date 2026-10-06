// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.oauth.IOAuthParameters;
import org.openstreetmap.josm.data.oauth.OAuth20Exception;
import org.openstreetmap.josm.data.oauth.OAuth20Parameters;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthAccessTokenHolder;
import org.openstreetmap.josm.data.oauth.OAuthVersion;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.HttpClient;

/**
 * Unit tests of {@link OsmConnection}.
 */
@BasicPreferences
class OsmConnectionTest {

    private static final String API_URL = "https://nonprod-mapops.example.org/api";
    private static final String CLIENT_ID = "a-client-id-of-an-application-registered-on-that-server";
    private static final String REDIRECT_URI = "http://127.0.0.1:8111/oauth_authorization";

    /** Gives the test access to the protected members of {@link OsmConnection} */
    private static final class TestConnection extends OsmConnection {
        IOAuthParameters parameters() {
            return this.oAuth20Parameters;
        }
    }

    @BeforeEach
    void setUp() {
        // the RFC 8414 lookup of OAuthParameters must not make a network request
        NetworkManager.setOffline(OnlineResource.ALL);
        Config.getPref().put("osm-server.url", API_URL);
    }

    @AfterEach
    void tearDown() {
        NetworkManager.setOnline(OnlineResource.ALL);
        OAuthAccessTokenHolder.getInstance().clear();
        // preferences and the token holder are static state shared with the other tests of this class
        Config.getPref().put("oauth.access-token.parameters." + OAuthVersion.OAuth20 + "." + API_URL, null);
        Config.getPref().put("osm-server.url", null);
    }

    private static HttpClient apiRequest() throws MalformedURLException {
        return HttpClient.create(new URL(API_URL + "/0.6/changeset/create"), "PUT");
    }

    /**
     * Stores the advanced OAuth parameters the way the authentication preferences do, that is under the API URL.
     */
    private static OAuth20Parameters rememberParameters() {
        OAuth20Parameters parameters = new OAuth20Parameters(CLIENT_ID, null, API_URL, API_URL, REDIRECT_URI);
        parameters.rememberPreferences();
        return parameters;
    }

    /**
     * The OAuth parameters of a connection are the ones the user entered for the configured API.
     * <p>
     * Non-regression test for #24925: they were looked up with the host of the request although they are stored
     * under the API URL, so the lookup missed and the client id was empty - which made the authorization request
     * on upload fail with "Missing required parameter: client_id".
     * @throws Exception if the connection cannot be signed
     */
    @Test
    void testOAuthParametersComeFromTheApiUrl() throws Exception {
        OAuth20Parameters parameters = rememberParameters();
        // a token is needed, otherwise the connection would try to obtain one
        OAuthAccessTokenHolder.getInstance().setAccessToken(API_URL, new OAuth20Token(parameters,
                "{\"token_type\":\"bearer\", \"access_token\":\"a-token\"}"));

        TestConnection connection = new TestConnection();
        assertNull(connection.parameters());
        HttpClient client = apiRequest();
        connection.addOAuth20AuthorizationHeader(client);

        assertEquals(CLIENT_ID, connection.parameters().getClientId());
        assertEquals(API_URL, connection.parameters().getApiUrl());
        assertEquals("Bearer a-token", client.getRequestHeader("Authorization"));
    }

    /**
     * Without a client id there is nothing to authorize with, so the user is told to configure one instead of
     * being sent to a browser which the server then rejects with "Missing required parameter: client_id".
     * <p>
     * The confirmation dialog is pre-answered with "No" so that the test still terminates if the check is ever
     * removed; what tells the two apart is that only the check names the server it gave up on.
     * @throws MalformedURLException never
     */
    @Test
    void testNoClientIdFailsBeforeAskingToOpenABrowser() throws MalformedURLException {
        Config.getPref().putBoolean("message.oauth.oauth20.obtain.automatically", false);
        Config.getPref().putInt("message.oauth.oauth20.obtain.automatically.value", JOptionPane.NO_OPTION);

        // no parameters remembered for this API, and it has no client id built into JOSM
        HttpClient client = apiRequest();
        MissingOAuthAccessTokenException thrown = assertThrows(MissingOAuthAccessTokenException.class,
                () -> new TestConnection().addOAuth20AuthorizationHeader(client));
        assertNotNull(thrown.getMessage(), "gave up only after asking the user, rather than for the missing client id");
        assertTrue(thrown.getMessage().contains(API_URL), thrown.getMessage());
        assertNull(client.getRequestHeader("Authorization"));
    }

    /**
     * An existing token is used without the parameters having to be stored, since the token carries its own.
     * @throws OAuth20Exception if the token cannot be built
     * @throws Exception if the connection cannot be signed
     */
    @Test
    void testExistingTokenSignsTheConnection() throws Exception {
        OAuth20Parameters parameters = new OAuth20Parameters(CLIENT_ID, null, API_URL, API_URL, REDIRECT_URI);
        OAuthAccessTokenHolder.getInstance().setAccessToken(API_URL, new OAuth20Token(parameters,
                "{\"token_type\":\"bearer\", \"access_token\":\"another-token\"}"));

        HttpClient client = apiRequest();
        new TestConnection().addOAuth20AuthorizationHeader(client);
        assertEquals("Bearer another-token", client.getRequestHeader("Authorization"));
    }
}
