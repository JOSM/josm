// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.FixedDelayDistribution;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.oauth.osm.OsmScopes;
import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.remotecontrol.RemoteControl;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.mockers.OpenBrowserMocker;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;

@BasicPreferences
@HTTP
class OAuth20AuthorizationTest {
    private static final String RESPONSE_TYPE = "response_type";
    private static final String RESPONSE_TYPE_VALUE = "code";
    private static final String CLIENT_ID = "client_id";
    private static final String CLIENT_ID_VALUE = "edPII614Lm0_0zEpc_QzEltA9BUll93-Y-ugRQUoHMI";
    private static final String REDIRECT_URI = "redirect_uri";
    private static final String REDIRECT_URI_VALUE = "http://127.0.0.1:8111/oauth_authorization";
    private static final String SCOPE = "scope";
    private static final String STATE = "state";
    private static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    private static final String CODE_CHALLENGE_METHOD_VALUE = "S256";
    private static final String CODE_CHALLENGE = "code_challenge";

    private enum ConnectionProblems {
        NONE,
        SOCKET_TIMEOUT
    }

    private static class OAuthServerWireMock extends ResponseTransformer {
        String stateToReturn;
        ConnectionProblems connectionProblems = ConnectionProblems.NONE;
        @Override
        public Response transform(Request request, Response response, FileSource files, Parameters parameters) {
            try {
                if (request.getUrl().startsWith("/oauth2/authorize")) {
                    return authorizationRequest(request, response);
                } else if (request.getUrl().startsWith("/oauth2/token")) {
                    return tokenRequest(request, response);
                }
                return response;
            } catch (Exception e) {
                // Make certain we actually see the exception in logs -- WireMock returns the error, but then our code needs to print it
                Logging.error(e);
                throw e;
            }
        }

        private Response tokenRequest(Request request, Response response) {
            Map<String, String> queryParameters = Stream.of(request.getBodyAsString().split("&", -1))
                    .map(string -> string.split("=", -1))
                    .collect(Collectors.toMap(strings -> strings[0], strings -> strings[1]));
            if (!queryParameters.containsKey("grant_type")
                    || !queryParameters.containsKey(REDIRECT_URI) || !queryParameters.containsKey(CLIENT_ID)
                    || !queryParameters.containsKey("code") || !queryParameters.containsKey("code_verifier")) {
                return Response.Builder.like(response).but().status(500).build();
            }
            switch (connectionProblems) {
                case SOCKET_TIMEOUT:
                    return Response.Builder.like(response).but().configureDelay(null, null,
                                    10_000, new FixedDelayDistribution(0)).build();
                case NONE:
                default:
                    return Response.Builder.like(response).but()
                            .body("{\"token_type\": \"bearer\", \"access_token\": \"test_access_token\"}").build();
            }
        }

        private Response authorizationRequest(Request request, Response response) {
            final QueryParameter state = request.queryParameter(STATE);
            final QueryParameter codeChallenge = request.queryParameter(CODE_CHALLENGE);
            final QueryParameter redirectUri = request.queryParameter(REDIRECT_URI);
            final QueryParameter responseType = request.queryParameter(RESPONSE_TYPE);
            final QueryParameter scope = request.queryParameter(SCOPE);
            final QueryParameter clientId = request.queryParameter(CLIENT_ID);
            final QueryParameter codeChallengeMethod = request.queryParameter(CODE_CHALLENGE_METHOD);
            final boolean badRequest = !(state.isPresent() && state.isSingleValued());
            if (badRequest || checkQueryParameter(redirectUri, REDIRECT_URI_VALUE) || checkQueryParameter(responseType, RESPONSE_TYPE_VALUE)
                    || checkQueryParameter(clientId, CLIENT_ID_VALUE) || checkQueryParameter(codeChallengeMethod, CODE_CHALLENGE_METHOD_VALUE)
                    || checkQueryParameter(scope, "read_gpx")
                    || !codeChallenge.isPresent()) {
                return Response.Builder.like(response).but().status(500).build();
            }
            return Response.Builder.like(response).but().status(307)
                    .headers(new HttpHeaders(new HttpHeader("Location",
                            redirectUri.values().get(0)
                                    + "?state=" + (this.stateToReturn != null ? stateToReturn : state.firstValue())
                                    + "&code=test_code"))).build();
        }

        private static boolean checkQueryParameter(QueryParameter parameter, String expected) {
            return !parameter.isPresent() || !parameter.isSingleValued() || !parameter.containsValue(expected);
        }

        @Override
        public String getName() {
            return "OAuthServerWireMock";
        }
    }

    private static final OAuthServerWireMock oauthServer = new OAuthServerWireMock();
    @RegisterExtension
    static WireMockExtension wml = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort().dynamicHttpsPort().extensions(oauthServer))
            .build();
    @BeforeEach
    @AfterEach
    void setup() {
        // Reset the mocker
        OpenBrowserMocker.getCalledURIs().clear();
        RemoteControl.stop(); // Ensure remote control is stopped
        oauthServer.stateToReturn = null;
        oauthServer.connectionProblems = ConnectionProblems.NONE;
    }

    /**
     * Set up the default wiremock information
     * @param wireMockRuntimeInfo The info to set up
     */
    @BeforeEach
    void setupWireMock(WireMockRuntimeInfo wireMockRuntimeInfo) {
        Config.getPref().put("osm-server.url", wireMockRuntimeInfo.getHttpBaseUrl() + "/api/");
        new MockUp<JosmUrls>() {
            @Mock
            public String getDefaultOsmApiUrl() {
                return wireMockRuntimeInfo.getHttpBaseUrl() + "/api/";
            }
        };
        new OpenBrowserMocker();
        final Map<String, StringValuePattern> queryParams = new HashMap<>();
        queryParams.put(RESPONSE_TYPE, new EqualToPattern(RESPONSE_TYPE_VALUE));
        queryParams.put(CLIENT_ID, new EqualToPattern(CLIENT_ID_VALUE));
        queryParams.put(REDIRECT_URI, new EqualToPattern(REDIRECT_URI_VALUE));
        queryParams.put(SCOPE, new EqualToPattern("read_gpx"));
        queryParams.put(STATE, new AnythingPattern()); // This is generated via a random UUID, and we have to return this in the redirect
        queryParams.put(CODE_CHALLENGE_METHOD, new EqualToPattern(CODE_CHALLENGE_METHOD_VALUE));
        queryParams.put(CODE_CHALLENGE, new AnythingPattern()); // This is generated via a random UUID
        wireMockRuntimeInfo.getWireMock().register(WireMock.get(WireMock.urlPathEqualTo("/oauth2/authorize")).withQueryParams(queryParams));
        wireMockRuntimeInfo.getWireMock().register(WireMock.post(WireMock.urlPathEqualTo("/oauth2/token")));
    }

    private HttpClient generateClient(WireMockRuntimeInfo wireMockRuntimeInfo, AtomicReference<Optional<IOAuthToken>> consumer) {
        final OAuth20Authorization authorization = new OAuth20Authorization();
        OAuth20Parameters parameters = (OAuth20Parameters) OAuthParameters.createDefault(OsmApi.getOsmApi().getBaseUrl(), OAuthVersion.OAuth20);
        RemoteControl.start();
        authorization.authorize(new OAuth20Parameters(parameters.getClientId(), parameters.getClientSecret(),
                wireMockRuntimeInfo.getHttpBaseUrl() + "/oauth2", wireMockRuntimeInfo.getHttpBaseUrl() + "/api",
                parameters.getRedirectUri()), consumer::set, OsmScopes.read_gpx);
        assertEquals(1, OpenBrowserMocker.getCalledURIs().size());
        final URL url = assertDoesNotThrow(() -> OpenBrowserMocker.getCalledURIs().get(0).toURL());
        return HttpClient.create(url);
    }

    @Test
    void testAuthorize(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        final AtomicReference<Optional<IOAuthToken>> consumer = new AtomicReference<>();
        final HttpClient client = generateClient(wireMockRuntimeInfo, consumer);
        try {
            HttpClient.Response response = client.connect();
            assertEquals(200, response.getResponseCode());
        } finally {
            client.disconnect();
        }
        assertNotNull(consumer.get());
        assertTrue(consumer.get().isPresent());
        assertEquals(OAuthVersion.OAuth20, consumer.get().get().getOAuthType());
        OAuth20Token token = (OAuth20Token) consumer.get().get();
        assertEquals("test_access_token", token.getBearerToken());
    }

    @Test
    void testAuthorizeBadState(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        oauthServer.stateToReturn = "Bad_State";
        final AtomicReference<Optional<IOAuthToken>> consumer = new AtomicReference<>();
        final HttpClient client = generateClient(wireMockRuntimeInfo, consumer);
        try {
            HttpClient.Response response = client.connect();
            assertEquals(400, response.getResponseCode());
            String content = response.fetchContent();
            assertTrue(content.contains("Unknown state for authorization"));
        } finally {
            client.disconnect();
        }
        assertNull(consumer.get(), "The OAuth consumer should not be called since the state does not match");
    }

    @Test
    void testSocketTimeout(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        // 1s before timeout
        Config.getPref().putInt("socket.timeout.connect", 1);
        Config.getPref().putInt("socket.timeout.read", 1);
        oauthServer.connectionProblems = ConnectionProblems.SOCKET_TIMEOUT;

        final AtomicReference<Optional<IOAuthToken>> consumer = new AtomicReference<>();
        final HttpClient client = generateClient(wireMockRuntimeInfo, consumer)
                .setConnectTimeout(15_000).setReadTimeout(30_000);
        try {
            HttpClient.Response response = client.connect();
            assertEquals(500, response.getResponseCode());
            String content = response.fetchContent();
            assertTrue(content.contains("java.net.SocketTimeoutException: Read timed out"));
        } finally {
            client.disconnect();
        }
        assertEquals(Optional.empty(), consumer.get());
    }
}
