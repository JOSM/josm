// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.data.oauth.OAuth20Exception;
import org.openstreetmap.josm.data.oauth.OAuth20Parameters;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.data.oauth.OAuthToken;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Test interface for {@link CredentialsAgent} implementations.
 */
@BasicPreferences
public interface CredentialsAgentTest<T extends CredentialsAgent> {
    /**
     * Create the agent to test
     * @return The agent to test
     */
    T createAgent();

    static List<String> getHosts() {
        return Arrays.asList("https://somewhere.random", OsmApi.getOsmApi().getHost());
    }

    @ParameterizedTest
    @MethodSource("getHosts")
    @BasicPreferences // We need to reset preferences between runs
    default void testLookUpAndStorePasswordAuthentication(final String host) throws CredentialsAgentException {
        final T agent = createAgent();
        for (Authenticator.RequestorType type : Authenticator.RequestorType.values()) {
            PasswordAuthentication passwordAuthentication = agent.lookup(type, host);
            assertNull(passwordAuthentication, "Password authentication should not be set up yet");
            PasswordAuthentication toStore = new PasswordAuthentication("hunter", "password".toCharArray());
            agent.store(type, host, toStore);
            passwordAuthentication = agent.lookup(type, host);
            assertNotNull(passwordAuthentication);
            // We can't just use equals, since PasswordAuthentication does not override the default equals method
            assertEquals(toStore.getUserName(), passwordAuthentication.getUserName());
            assertArrayEquals(toStore.getPassword(), passwordAuthentication.getPassword());
            // This is what sets the Config values. Note that PasswordAuthentication cannot take a null password.
            agent.store(type, host, new PasswordAuthentication("hunter", new char[0]));
            // Now we need to purge the cache
            agent.purgeCredentialsCache(type);
            passwordAuthentication = agent.lookup(type, host);
            assertEquals(toStore.getUserName(), passwordAuthentication.getUserName());
            assertArrayEquals(new char[0], passwordAuthentication.getPassword());
            // We don't currently have a way to fully remove credentials, but that ought to be tested here.
        }
    }

    @Test
    default void testLookUpAndStorePasswordAuthenticationNull() throws CredentialsAgentException {
        final T agent = createAgent();
        assertDoesNotThrow(() -> agent.store(null, "https://somewhere.random", new PasswordAuthentication("random", new char[0])));
        assertNull(agent.lookup(null, "https://somewhere.random"));
        assertDoesNotThrow(() -> agent.store(Authenticator.RequestorType.SERVER, null, new PasswordAuthentication("random", new char[0])));
        for (Authenticator.RequestorType type : Authenticator.RequestorType.values()) {
            assertNull(agent.lookup(type, null));
        }
        assertNull(agent.lookup(null, null));
    }

    @Test
    default void testLookUpAndStoreOAuth10() throws CredentialsAgentException {
        final T agent = createAgent();
        assertNull(agent.lookupOAuthAccessToken());
        final OAuthToken token = new OAuthToken("foo", "bar");
        agent.storeOAuthAccessToken(token);
        final OAuthToken actual = agent.lookupOAuthAccessToken();
        assertEquals(token, actual);
        agent.storeOAuthAccessToken(null);
        assertNull(agent.lookupOAuthAccessToken());
    }

    @ParameterizedTest
    @MethodSource("getHosts")
    default void testLookupAndStoreOAuthTokens(final String host) throws CredentialsAgentException, OAuth20Exception {
        final T agent = createAgent();
        assertNull(agent.lookupOAuthAccessToken(host));
        agent.storeOAuthAccessToken(host, new OAuth20Token(new OAuth20Parameters("clientId", "clientSecret",
                "tokenUrl", "authorizeUrl", "apiUrl", "redirectUrl"),
                "{\"access_token\": \"test_token\", \"token_type\": \"bearer\"}"));
        OAuth20Token token = (OAuth20Token) agent.lookupOAuthAccessToken(host);
        assertNotNull(token);
        assertEquals("test_token", token.getBearerToken());
        agent.storeOAuthAccessToken(host, null);
        assertNull(agent.lookupOAuthAccessToken(host));
    }
}
