// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.HTTP;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;

/**
 * Credentials are not written to the log by {@link HttpClient}: debug and trace logs are often posted in public
 * tickets. The stripping itself is tested in {@link TextUtilsTest}.
 */
@BasicPreferences
@BasicWiremock
@HTTP
class HttpClientCredentialsLoggingTest {
    private static final String TOKEN = "a-secret-bearer-token";
    private static final String COOKIE = "a-secret-session-cookie";
    private static final String CLIENT_SECRET = "a-secret-client-secret";

    private final List<String> logged = Collections.synchronizedList(new ArrayList<>());
    private final SimpleFormatter formatter = new SimpleFormatter();
    private final Handler handler = new Handler() {
        @Override
        public void publish(LogRecord logRecord) {
            logged.add(formatter.formatMessage(logRecord));
        }

        @Override
        public void flush() {
            // nothing to flush
        }

        @Override
        public void close() {
            // nothing to close
        }
    };
    private Level level;

    @BeforeEach
    void setUp() {
        level = Logging.getLogger().getLevel();
        Logging.getLogger().addHandler(handler);
        Logging.setLogLevel(Logging.LEVEL_TRACE);
    }

    @AfterEach
    void tearDown() {
        Logging.getLogger().removeHandler(handler);
        Logging.setLogLevel(level);
    }

    /**
     * A request with a bearer token, answered with a session cookie, and a token request with a client secret, are
     * logged at trace level without any of them.
     * @param wireMock the server answering the requests
     * @throws Exception if a request fails
     */
    @Test
    void testRequestsAreLoggedWithoutCredentials(WireMockRuntimeInfo wireMock) throws Exception {
        wireMock.getWireMock().register(get(urlEqualTo("/api/0.6/user/details"))
                .willReturn(aResponse().withStatus(200).withHeader("Set-Cookie", "_osm_session=" + COOKIE).withBody("<osm/>")));
        wireMock.getWireMock().register(post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse().withStatus(200).withBody("{}")));

        final HttpClient details = HttpClient.create(URI.create(wireMock.getHttpBaseUrl() + "/api/0.6/user/details").toURL());
        details.setHeader("Authorization", "Bearer " + TOKEN);
        try {
            details.connect();
        } finally {
            details.disconnect();
        }
        final HttpClient token = HttpClient.create(URI.create(wireMock.getHttpBaseUrl() + "/oauth2/token").toURL(), "POST");
        token.setHeader("Content-Type", "application/x-www-form-urlencoded");
        token.setRequestBody(("grant_type=authorization_code&client_id=an-id&client_secret=" + CLIENT_SECRET + "&code=a-code")
                .getBytes(StandardCharsets.UTF_8));
        try {
            token.connect();
        } finally {
            token.disconnect();
        }

        final String log = String.join("\n", logged);
        assertTrue(log.contains("Authorization=Bearer ...stripped..."), log);
        assertTrue(log.contains("client_secret=...stripped..."), log);
        for (String secret : new String[] {TOKEN, COOKIE, CLIENT_SECRET}) {
            assertFalse(log.contains(secret), () -> "logged: " + secret + "\n" + log);
        }
    }
}
