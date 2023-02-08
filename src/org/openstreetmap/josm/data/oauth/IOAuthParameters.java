// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import java.util.stream.Stream;

/**
 * A generic interface for OAuth Parameters
 * @author Taylor Smock
 * @since 18650
 */
public interface IOAuthParameters {
    /**
     * Get the access token URL
     * @return The URL to use to switch the code to a token
     */
    String getAccessTokenUrl();

    /**
     * Get the base authorization URL to open in a browser
     * @return The base URL to send to the browser
     */
    String getAuthorizationUrl();

    /**
     * Get the authorization URL to open in a browser
     * @param state The state to prevent attackers from providing their own token
     * @param scopes The scopes to request
     * @return The URL to send to the browser
     */
    default String getAuthorizationUrl(String state, Enum<?>... scopes) {
        return this.getAuthorizationUrl(state, Stream.of(scopes).map(Enum::toString).toArray(String[]::new));
    }

    /**
     * Get the authorization URL to open in a browser
     * @param state The state to prevent attackers from providing their own token
     * @param scopes The scopes to request
     * @return The URL to send to the browser
     */
    default String getAuthorizationUrl(String state, String... scopes) {
        // response_type = code | token, but token is deprecated in the draft oauth 2.1 spec
        // 2.1 is adding code_challenge, code_challenge_method
        // code_challenge requires a code_verifier
        return this.getAuthorizationUrl() + "?response_type=code&client_id=" + this.getClientId()
                + "&redirect_uri=" + this.getRedirectUri()
                + "&scope=" + String.join(" ", scopes)
                // State is used to detect/prevent cross-site request forgery
                + "&state=" + state;
    }

    /**
     * Get the OAuth version that the API expects
     * @return The oauth version
     */
    OAuthVersion getOAuthVersion();

    /**
     * Get the client id
     * @return The client id
     */
    String getClientId();

    /**
     * Get the client secret
     * @return The client secret
     */
    String getClientSecret();

    /**
     * Get the redirect URI
     * @return The redirect URI
     */
    default String getRedirectUri() {
        return null;
    }

    /**
     * Convert to a preference string
     * @return the preference string
     */
    default String toPreferencesString() {
        return null;
    }

    /**
     * Get the actual API URL
     * @return The API URl
     */
    default String getApiUrl() {
        return null;
    }

    void rememberPreferences();
}
