// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.oauth.OAuth20Exception;
import org.openstreetmap.josm.data.oauth.OAuth20Parameters;
import org.openstreetmap.josm.data.oauth.OAuth20Token;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;

/**
 * Test {@link JosmPreferencesCredentialAgent}
 */
@HTTP
class JosmPreferencesCredentialAgentTest implements CredentialsAgentTest<JosmPreferencesCredentialAgent> {

    @Override
    public JosmPreferencesCredentialAgent createAgent() {
        return new JosmPreferencesCredentialAgent();
    }

    /**
     * Removing an OAuth token removes it from the preferences, and keeps the parameters stored with it: they are the
     * OAuth settings of the server too, which are needed to obtain the next token.
     * <p>
     * Non-regression test for #24932: both keys were built with the {@code parameters} prefix, so the token itself
     * stayed in the preferences in clear text. {@link CredentialsAgentTest#testLookupAndStoreOAuthTokens} did not
     * notice, because a lookup needs both keys and so already failed once the parameters were gone.
     * @throws CredentialsAgentException if the token cannot be stored or removed
     * @throws OAuth20Exception if the token cannot be built
     */
    @Test
    @BasicPreferences
    void testRemoveOAuthTokenRemovesItFromThePreferences() throws CredentialsAgentException, OAuth20Exception {
        final String host = "example.org";
        final String tokenKey = "oauth.access-token.object.OAuth20." + host;
        final String parametersKey = "oauth.access-token.parameters.OAuth20." + host;
        final JosmPreferencesCredentialAgent agent = createAgent();
        agent.storeOAuthAccessToken(host, new OAuth20Token(new OAuth20Parameters("clientId", null,
                "https://example.org/api", "https://example.org/api", "http://127.0.0.1:8111/oauth_authorization"),
                "{\"access_token\": \"a-secret-token\", \"token_type\": \"bearer\"}"));
        assertNotNull(Config.getPref().get(tokenKey, null));
        final String parameters = Config.getPref().get(parametersKey, null);
        assertNotNull(parameters);

        agent.storeOAuthAccessToken(host, null);

        assertNull(Config.getPref().get(tokenKey, null), "the token is still stored in the preferences");
        assertFalse(Config.getPref().getSensitive().contains(tokenKey));
        assertNull(agent.lookupOAuthAccessToken(host));
        assertEquals(parameters, Config.getPref().get(parametersKey, null), "the OAuth settings of the server are gone");
        Config.getPref().removeSensitive(parametersKey);
    }
}
