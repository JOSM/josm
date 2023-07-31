// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import org.openstreetmap.josm.tools.HttpClient;

/**
 * An interface for oauth tokens
 * @author Taylor Smock
 * @since 18650
 */
public interface IOAuthToken {
    /**
     * Sign a client
     * @param client The client to sign
     * @throws OAuthException if the OAuth token type is unknown (AKA we don't know how to handle it)
     */
    void sign(HttpClient client) throws OAuthException;

    /**
     * Get the preferences string of this auth token.
     * This should match the expected return body from the authentication server.
     * For OAuth, this is typically JSON.
     * @return The preferences string
     */
    String toPreferencesString();

    /**
     * Get the auth type of this token
     * @return The auth type
     */
    OAuthVersion getOAuthType();

    /**
     * Get the OAuth parameters
     * @return The OAuth parameters
     */
    IOAuthParameters getParameters();
}
